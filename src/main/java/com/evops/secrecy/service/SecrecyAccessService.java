package com.evops.secrecy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.common.BusinessException;
import com.evops.common.OperatorHolder;
import com.evops.exam.vo.ActionPreview;
import com.evops.exam.vo.AffectedItem;
import com.evops.secrecy.common.SecrecyConstants;
import com.evops.secrecy.dto.AccessImportItem;
import com.evops.secrecy.dto.AccessImportRequest;
import com.evops.secrecy.dto.ReviewRequest;
import com.evops.secrecy.dto.SupplementRequest;
import com.evops.secrecy.dto.SwipeRequest;
import com.evops.secrecy.entity.SecrecyAccess;
import com.evops.secrecy.entity.SecrecyAccessSupplement;
import com.evops.secrecy.entity.SecrecyAccessVersion;
import com.evops.secrecy.entity.SecrecyAlert;
import com.evops.secrecy.entity.SecrecyBox;
import com.evops.secrecy.entity.SecrecyPerson;
import com.evops.secrecy.mapper.SecrecyAccessMapper;
import com.evops.secrecy.mapper.SecrecyAccessSupplementMapper;
import com.evops.secrecy.mapper.SecrecyAccessVersionMapper;
import com.evops.secrecy.mapper.SecrecyAlertMapper;
import com.evops.secrecy.vo.AccessDetailView;
import com.evops.secrecy.vo.InRoomView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 出入登记（模块 + 版本 + 状态）：
 * - 进出刷证：登记初始 PENDING_REVIEW 不生效；第二人复核确认才 CONFIRMED 生效，拒绝 REJECTED 终态；
 * - 复核人必须是登记时指定的第二人本人（双人双锁），且不能是登记人本人；
 * - 登记落库不可改：状态推进只追加版本，纠错只走补充说明；
 * - 在室清单按已生效进出实时推导；卷箱出入随登记挂钩联动；
 * - 异常时段自动打标并生成异常标记给保卫；
 * - 纸面存量两步导入：导入即为已生效历史，保留原始登记时间。
 */
@Service
public class SecrecyAccessService {

    private static final DateTimeFormatter ACCESS_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SecrecyAccessMapper accessMapper;
    private final SecrecyAccessVersionMapper versionMapper;
    private final SecrecyAccessSupplementMapper supplementMapper;
    private final SecrecyAlertMapper alertMapper;
    private final SecrecyPersonService personService;
    private final SecrecyBoxService boxService;

    public SecrecyAccessService(SecrecyAccessMapper accessMapper,
                                SecrecyAccessVersionMapper versionMapper,
                                SecrecyAccessSupplementMapper supplementMapper,
                                SecrecyAlertMapper alertMapper,
                                SecrecyPersonService personService,
                                SecrecyBoxService boxService) {
        this.accessMapper = accessMapper;
        this.versionMapper = versionMapper;
        this.supplementMapper = supplementMapper;
        this.alertMapper = alertMapper;
        this.personService = personService;
        this.boxService = boxService;
    }

    // ==================== 刷证登记（进出刷证，登记待复核） ====================

    @Transactional
    public AccessDetailView swipe(SwipeRequest request) {
        String operator = OperatorHolder.current();
        String direction = normalizeDirection(request.getDirection());
        SecrecyPerson person = personService.requireEnabledPerson(request.getCardNo(), request.getPersonCode());
        SecrecyPerson second = personService.requireEnabledByCode(
                request.getSecondPersonCode(), "第二人编码不能为空（双人双锁）");
        if (second.getId().equals(person.getId())) {
            throw new BusinessException("SECOND_PERSON_SAME",
                    "第二人不能是登记人本人（双人双锁须两人同行）：" + person.getCode());
        }
        LocalDateTime happenedAt = request.getHappenedAt() == null ? LocalDateTime.now() : request.getHappenedAt();

        Long pending = accessMapper.selectCount(new QueryWrapper<SecrecyAccess>()
                .eq("person_id", person.getId())
                .eq("direction", direction)
                .eq("status", SecrecyConstants.ACCESS_PENDING));
        if (pending != null && pending > 0) {
            throw new BusinessException("DUPLICATE_PENDING",
                    "存在未复核的同方向登记，请先复核处理：" + person.getCode() + " " + person.getName());
        }

        List<SecrecyBox> boxes = boxService.validateForSwipe(request.getBoxNos(), direction);
        boolean offHours = personService.isOffHours(happenedAt);

        SecrecyAccess access = new SecrecyAccess();
        access.setAccessNo(nextAccessNo(happenedAt.toLocalDate(), new HashMap<LocalDate, Integer>()));
        access.setPersonId(person.getId());
        access.setPersonCode(person.getCode());
        access.setPersonName(person.getName());
        access.setCardNo(person.getCardNo());
        access.setDirection(direction);
        access.setHappenedAt(happenedAt);
        access.setSecondPersonId(second.getId());
        access.setSecondPersonCode(second.getCode());
        access.setSecondPersonName(second.getName());
        access.setCurrentVersion(1);
        access.setStatus(SecrecyConstants.ACCESS_PENDING);
        access.setSource(SecrecyConstants.SOURCE_MANUAL);
        access.setOffHours(offHours);
        access.setBoxCount(boxes.size());
        access.setRemark(request.getRemark());
        access.setOperator(operator);
        accessMapper.insert(access);

        appendVersion(access, SecrecyConstants.ACCESS_PENDING, SecrecyConstants.CHANGE_REGISTER,
                null, null, "刷证登记，待第二人复核", operator);

        boxService.createMoves(access, boxes, operator);

        if (offHours) {
            createOffHoursAlert(access);
        }
        return detail(access.getId());
    }

    // ==================== 第二人复核（确认生效 / 拒绝作废） ====================

    /** 复核确认：PENDING_REVIEW → CONFIRMED，卷箱出入联动生效。 */
    @Transactional
    public AccessDetailView confirm(Long accessId, ReviewRequest request) {
        SecrecyAccess access = requirePending(accessId);
        SecrecyPerson reviewer = requireSecondPerson(access, request.getReviewerCode());
        String operator = OperatorHolder.current();

        access.setCurrentVersion(access.getCurrentVersion() + 1);
        access.setStatus(SecrecyConstants.ACCESS_CONFIRMED);
        accessMapper.updateById(access);
        appendVersion(access, SecrecyConstants.ACCESS_CONFIRMED, SecrecyConstants.CHANGE_CONFIRM,
                reviewer.getCode(), reviewer.getName(), "第二人复核确认，登记生效", operator);

        boxService.syncMovesOnReview(access, true);
        return detail(access.getId());
    }

    /** 复核拒绝：PENDING_REVIEW → REJECTED（终态），卷箱出入联动作废。 */
    @Transactional
    public AccessDetailView reject(Long accessId, ReviewRequest request) {
        SecrecyAccess access = requirePending(accessId);
        SecrecyPerson reviewer = requireSecondPerson(access, request.getReviewerCode());
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("REJECT_REASON_REQUIRED", "复核拒绝必须填写原因");
        }
        String operator = OperatorHolder.current();

        access.setCurrentVersion(access.getCurrentVersion() + 1);
        access.setStatus(SecrecyConstants.ACCESS_REJECTED);
        accessMapper.updateById(access);
        appendVersion(access, SecrecyConstants.ACCESS_REJECTED, SecrecyConstants.CHANGE_REJECT,
                reviewer.getCode(), reviewer.getName(), request.getReason().trim(), operator);

        boxService.syncMovesOnReview(access, false);
        return detail(access.getId());
    }

    // ==================== 补充说明（登记不可改，纠错唯一入口） ====================

    @Transactional
    public SecrecyAccessSupplement addSupplement(Long accessId, SupplementRequest request) {
        requireAccess(accessId);
        SecrecyAccessSupplement supplement = new SecrecyAccessSupplement();
        supplement.setAccessId(accessId);
        supplement.setContent(request.getContent().trim());
        supplement.setReason(request.getReason());
        supplement.setOperator(OperatorHolder.current());
        supplement.setSupplementedAt(LocalDateTime.now());
        supplementMapper.insert(supplement);
        return supplement;
    }

    public List<SecrecyAccessSupplement> listSupplements(Long accessId) {
        requireAccess(accessId);
        return supplementMapper.selectList(new QueryWrapper<SecrecyAccessSupplement>()
                .eq("access_id", accessId)
                .orderByAsc("id"));
    }

    // ==================== 查询（在室清单实时可查） ====================

    public List<SecrecyAccess> list(String personCode, String status, String direction,
                                    Boolean offHours, LocalDate dateFrom, LocalDate dateTo) {
        QueryWrapper<SecrecyAccess> wrapper = new QueryWrapper<SecrecyAccess>()
                .orderByDesc("happened_at", "id");
        if (personCode != null && !personCode.trim().isEmpty()) {
            wrapper.eq("person_code", personCode.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }
        if (direction != null && !direction.trim().isEmpty()) {
            wrapper.eq("direction", direction.trim().toUpperCase());
        }
        if (offHours != null) {
            wrapper.eq("off_hours", offHours);
        }
        if (dateFrom != null) {
            wrapper.ge("happened_at", dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            wrapper.lt("happened_at", dateTo.plusDays(1).atStartOfDay());
        }
        return accessMapper.selectList(wrapper);
    }

    public AccessDetailView detail(Long accessId) {
        SecrecyAccess access = requireAccess(accessId);
        AccessDetailView view = new AccessDetailView();
        view.setAccess(access);
        view.setVersions(versions(accessId));
        view.setSupplements(supplementMapper.selectList(new QueryWrapper<SecrecyAccessSupplement>()
                .eq("access_id", accessId).orderByAsc("id")));
        view.setBoxMoves(boxService.movesOfAccess(accessId));
        return view;
    }

    public List<SecrecyAccessVersion> versions(Long accessId) {
        requireAccess(accessId);
        return versionMapper.selectList(new QueryWrapper<SecrecyAccessVersion>()
                .eq("access_id", accessId)
                .orderByAsc("version_no"));
    }

    /** 在室清单：已生效 IN 且无更晚已生效 OUT 的人员；待复核不计入，单列计数。 */
    @Transactional(readOnly = true)
    public InRoomView inRoom() {
        List<SecrecyAccess> confirmed = accessMapper.selectList(new QueryWrapper<SecrecyAccess>()
                .eq("status", SecrecyConstants.ACCESS_CONFIRMED)
                .orderByAsc("happened_at", "id"));
        Map<Long, SecrecyAccess> lastByPerson = new LinkedHashMap<>();
        for (SecrecyAccess access : confirmed) {
            lastByPerson.put(access.getPersonId(), access);
        }
        InRoomView view = new InRoomView();
        List<InRoomView.InRoomPerson> persons = new ArrayList<>();
        for (SecrecyAccess last : lastByPerson.values()) {
            if (SecrecyConstants.DIRECTION_IN.equals(last.getDirection())) {
                InRoomView.InRoomPerson p = new InRoomView.InRoomPerson();
                p.setPersonCode(last.getPersonCode());
                p.setPersonName(last.getPersonName());
                p.setEnteredAt(last.getHappenedAt());
                p.setAccessNo(last.getAccessNo());
                persons.add(p);
            }
        }
        view.setPersons(persons);
        view.setInRoomCount(persons.size());
        Long pending = accessMapper.selectCount(new QueryWrapper<SecrecyAccess>()
                .eq("status", SecrecyConstants.ACCESS_PENDING));
        view.setPendingCount(pending == null ? 0 : pending.intValue());
        return view;
    }

    // ==================== 纸面存量导入（三个月；两步：preview → execute） ====================

    @Transactional(readOnly = true)
    public ActionPreview previewImport(AccessImportRequest request) {
        List<ImportPlan> plan = buildImportPlan(request);
        List<AffectedItem> items = new ArrayList<>();
        int actionable = 0;
        int skip = 0;
        for (ImportPlan p : plan) {
            items.add(new AffectedItem(p.key, p.label, p.duplicate ? "已存在" : "可导入",
                    p.actionable, p.note));
            if (p.actionable) {
                actionable++;
            } else {
                skip++;
            }
        }
        ActionPreview preview = new ActionPreview();
        preview.setAction("IMPORT");
        preview.setTotalScanned(plan.size());
        preview.setActionableCount(actionable);
        preview.setSkipCount(skip);
        preview.setSummary(String.format("纸面存量 %d 条：可导入 %d 条，跳过 %d 条（重复）",
                plan.size(), actionable, skip));
        preview.setItems(items);
        return preview;
    }

    /**
     * 执行导入：结构性问题（人不存在 / 方向非法 / 时间缺失 / 第二人=本人）整批拒收、不落半条；
     * 同人同时间同方向重复逐条跳过。导入记录直接 CONFIRMED（纸面既成事实），保留原始时间，
     * 打异常时段标记但不生成异常标记单（历史不追保卫）。
     */
    @Transactional
    public Map<String, Object> importAccess(AccessImportRequest request) {
        List<ImportPlan> plan = buildImportPlan(request);
        List<String> structuralErrors = new ArrayList<>();
        for (ImportPlan p : plan) {
            if (p.structuralError != null) {
                structuralErrors.add(p.structuralError);
            }
        }
        if (!structuralErrors.isEmpty()) {
            throw new BusinessException("IMPORT_REJECTED",
                    "纸面存量导入整批拒收，共 " + structuralErrors.size() + " 条结构性问题："
                            + String.join("；", structuralErrors));
        }

        String operator = OperatorHolder.current();
        Map<LocalDate, Integer> sequences = new HashMap<>();
        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (ImportPlan p : plan) {
            if (!p.actionable) {
                skipped.add(p.label + "（" + p.note + "）");
                continue;
            }
            SecrecyAccess access = new SecrecyAccess();
            access.setAccessNo(nextAccessNo(p.item.getHappenedAt().toLocalDate(), sequences));
            access.setPersonId(p.person.getId());
            access.setPersonCode(p.person.getCode());
            access.setPersonName(p.person.getName());
            access.setCardNo(p.person.getCardNo());
            access.setDirection(p.direction);
            access.setHappenedAt(p.item.getHappenedAt());
            access.setSecondPersonId(p.second.getId());
            access.setSecondPersonCode(p.second.getCode());
            access.setSecondPersonName(p.second.getName());
            access.setCurrentVersion(2);
            access.setStatus(SecrecyConstants.ACCESS_CONFIRMED);
            access.setSource(SecrecyConstants.SOURCE_PAPER_IMPORT);
            access.setOffHours(personService.isOffHours(p.item.getHappenedAt()));
            access.setBoxCount(0);
            access.setRemark(p.item.getRemark());
            access.setOperator(operator);
            accessMapper.insert(access);

            appendVersion(access, 1, SecrecyConstants.ACCESS_PENDING, SecrecyConstants.CHANGE_REGISTER,
                    null, null, "纸面存量导入（原始登记）", operator);
            appendVersion(access, 2, SecrecyConstants.ACCESS_CONFIRMED, SecrecyConstants.CHANGE_CONFIRM,
                    p.second.getCode(), p.second.getName(), "纸面记录既成事实，导入即生效", operator);
            imported.add(access.getAccessNo());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("importedCount", imported.size());
        result.put("skipCount", skipped.size());
        result.put("importedAccessNos", imported);
        result.put("skipped", skipped);
        return result;
    }

    // ==================== 内部 ====================

    private SecrecyAccess requireAccess(Long accessId) {
        SecrecyAccess access = accessMapper.selectById(accessId);
        if (access == null) {
            throw new BusinessException("ACCESS_NOT_FOUND", "出入登记不存在：id=" + accessId);
        }
        return access;
    }

    private SecrecyAccess requirePending(Long accessId) {
        SecrecyAccess access = requireAccess(accessId);
        if (!SecrecyConstants.ACCESS_PENDING.equals(access.getStatus())) {
            throw new BusinessException("ACCESS_NOT_PENDING",
                    "登记已复核（" + access.getStatus() + "），不能重复处理：" + access.getAccessNo());
        }
        return access;
    }

    /** 复核人必须是登记时指定的第二人本人（双人双锁）。 */
    private SecrecyPerson requireSecondPerson(SecrecyAccess access, String reviewerCode) {
        if (reviewerCode == null || reviewerCode.trim().isEmpty()) {
            throw new BusinessException("REVIEWER_REQUIRED", "复核人编码不能为空");
        }
        if (!access.getSecondPersonCode().equals(reviewerCode.trim())) {
            throw new BusinessException("REVIEWER_NOT_SECOND_PERSON",
                    "复核人必须是登记指定的第二人本人：" + access.getSecondPersonCode()
                            + " " + access.getSecondPersonName());
        }
        return personService.requireEnabledByCode(reviewerCode.trim(), "复核人编码不能为空");
    }

    private void appendVersion(SecrecyAccess access, String status, String changeType,
                               String reviewerCode, String reviewerName, String reason, String operator) {
        appendVersion(access, access.getCurrentVersion(), status, changeType, reviewerCode, reviewerName,
                reason, operator);
    }

    private void appendVersion(SecrecyAccess access, int versionNo, String status, String changeType,
                               String reviewerCode, String reviewerName, String reason, String operator) {
        SecrecyAccessVersion version = new SecrecyAccessVersion();
        version.setAccessId(access.getId());
        version.setVersionNo(versionNo);
        version.setStatus(status);
        version.setChangeType(changeType);
        version.setReviewerCode(reviewerCode);
        version.setReviewerName(reviewerName);
        version.setChangeReason(reason);
        version.setOperator(operator);
        version.setChangedAt(LocalDateTime.now());
        versionMapper.insert(version);
    }

    private void createOffHoursAlert(SecrecyAccess access) {
        SecrecyAlert alert = new SecrecyAlert();
        alert.setAccessId(access.getId());
        alert.setAccessNo(access.getAccessNo());
        alert.setAlertType(SecrecyConstants.ALERT_OFF_HOURS);
        alert.setPersonCode(access.getPersonCode());
        alert.setPersonName(access.getPersonName());
        alert.setHappenedAt(access.getHappenedAt());
        alert.setContent(String.format("异常时段%s：%s %s 于 %s 刷证%s（登记号 %s，第二人 %s %s），请保卫核实",
                SecrecyConstants.DIRECTION_IN.equals(access.getDirection()) ? "进室" : "出室",
                access.getPersonCode(), access.getPersonName(), access.getHappenedAt(),
                SecrecyConstants.DIRECTION_IN.equals(access.getDirection()) ? "进" : "出",
                access.getAccessNo(), access.getSecondPersonCode(), access.getSecondPersonName()));
        alert.setStatus(SecrecyConstants.ALERT_NEW);
        alertMapper.insert(alert);
    }

    /** 登记号：SE + 日期 + 4 位流水；sequences 为批量导入时的本批流水缓存。 */
    private String nextAccessNo(LocalDate date, Map<LocalDate, Integer> sequences) {
        String prefix = "SE" + date.format(ACCESS_NO_DATE);
        Integer seq = sequences.get(date);
        if (seq == null) {
            List<SecrecyAccess> existing = accessMapper.selectList(new QueryWrapper<SecrecyAccess>()
                    .likeRight("access_no", prefix)
                    .orderByDesc("access_no")
                    .last("LIMIT 1"));
            seq = 0;
            if (!existing.isEmpty()) {
                String no = existing.get(0).getAccessNo();
                try {
                    seq = Integer.parseInt(no.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    // 非本规则登记号忽略，从 1 起
                }
            }
        }
        seq = seq + 1;
        sequences.put(date, seq);
        return prefix + String.format("%04d", seq);
    }

    private String normalizeDirection(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            throw new BusinessException("DIRECTION_REQUIRED", "进出方向不能为空（IN 进 / OUT 出）");
        }
        String d = direction.trim().toUpperCase();
        if (!SecrecyConstants.DIRECTION_IN.equals(d) && !SecrecyConstants.DIRECTION_OUT.equals(d)) {
            throw new BusinessException("DIRECTION_INVALID", "进出方向非法：" + direction + "（IN 进 / OUT 出）");
        }
        return d;
    }

    // ---------- 导入计划 ----------

    private List<ImportPlan> buildImportPlan(AccessImportRequest request) {
        List<ImportPlan> plan = new ArrayList<>();
        int index = 0;
        for (AccessImportItem item : request.getItems()) {
            index++;
            ImportPlan p = new ImportPlan();
            p.item = item;
            p.key = "#" + index;
            p.label = "#" + index + " " + item.getPersonCode() + " " + item.getDirection()
                    + " " + item.getHappenedAt();
            try {
                p.direction = normalizeDirection(item.getDirection());
                if (item.getHappenedAt() == null) {
                    throw new BusinessException("HAPPENED_AT_REQUIRED", "原始登记时间不能为空");
                }
                p.person = personService.requireEnabledByCode(item.getPersonCode(), "人员编码不能为空");
                p.second = personService.requireEnabledByCode(item.getSecondPersonCode(), "第二人编码不能为空");
                if (p.second.getId().equals(p.person.getId())) {
                    throw new BusinessException("SECOND_PERSON_SAME",
                            "第二人不能是登记人本人：" + p.person.getCode());
                }
            } catch (BusinessException e) {
                p.structuralError = "第 " + index + " 条（" + item.getPersonCode() + "）：" + e.getMessage();
                p.actionable = false;
                p.note = e.getMessage();
                plan.add(p);
                continue;
            }
            // 同人同时间同方向已存在 → 逐条跳过（幂等）
            Long dup = accessMapper.selectCount(new QueryWrapper<SecrecyAccess>()
                    .eq("person_id", p.person.getId())
                    .eq("direction", p.direction)
                    .eq("happened_at", item.getHappenedAt()));
            if (dup != null && dup > 0) {
                p.duplicate = true;
                p.actionable = false;
                p.note = "同人同时间同方向登记已存在，跳过";
            } else {
                p.actionable = true;
                p.note = "可导入（导入即生效，来源 PAPER_IMPORT）";
            }
            plan.add(p);
        }
        return plan;
    }

    private static class ImportPlan {
        private AccessImportItem item;
        private String key;
        private String label;
        private String direction;
        private SecrecyPerson person;
        private SecrecyPerson second;
        private boolean actionable;
        private boolean duplicate;
        private String note;
        private String structuralError;
    }
}
