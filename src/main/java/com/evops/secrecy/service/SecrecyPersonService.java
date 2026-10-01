package com.evops.secrecy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.common.BusinessException;
import com.evops.secrecy.common.SecrecyConstants;
import com.evops.secrecy.dto.PersonRequest;
import com.evops.secrecy.dto.TimeRuleRequest;
import com.evops.secrecy.entity.SecrecyPerson;
import com.evops.secrecy.entity.SecrecyTimeRule;
import com.evops.secrecy.mapper.SecrecyPersonMapper;
import com.evops.secrecy.mapper.SecrecyTimeRuleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 保密室准入人员与进出时段规则。
 */
@Service
public class SecrecyPersonService {

    private final SecrecyPersonMapper personMapper;
    private final SecrecyTimeRuleMapper timeRuleMapper;

    public SecrecyPersonService(SecrecyPersonMapper personMapper,
                                SecrecyTimeRuleMapper timeRuleMapper) {
        this.personMapper = personMapper;
        this.timeRuleMapper = timeRuleMapper;
    }

    // ---------- 准入人员 ----------

    @Transactional
    public SecrecyPerson createPerson(PersonRequest request) {
        SecrecyPerson person = new SecrecyPerson();
        person.setCode(request.getCode().trim());
        person.setName(request.getName().trim());
        person.setCardNo(request.getCardNo().trim());
        person.setRoleCode(normalizeRole(request.getRoleCode()));
        person.setPhone(trimToNull(request.getPhone()));
        person.setStatus(SecrecyConstants.STATUS_ENABLED);
        person.setRemark(request.getRemark());
        try {
            personMapper.insert(person);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("DUPLICATE_PERSON",
                    "人员编码或证卡号已存在：" + person.getCode() + " / " + person.getCardNo());
        }
        return person;
    }

    public List<SecrecyPerson> listPersons(String status) {
        QueryWrapper<SecrecyPerson> wrapper = new QueryWrapper<SecrecyPerson>().orderByAsc("code");
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }
        return personMapper.selectList(wrapper);
    }

    /** 按证卡号或人员编码定位启用人员（刷证入口）。 */
    public SecrecyPerson requireEnabledPerson(String cardNo, String personCode) {
        SecrecyPerson person = null;
        if (cardNo != null && !cardNo.trim().isEmpty()) {
            person = personMapper.selectOne(
                    new QueryWrapper<SecrecyPerson>().eq("card_no", cardNo.trim()));
            if (person == null) {
                throw new BusinessException("CARD_NOT_FOUND", "证卡号未登记：" + cardNo.trim());
            }
        } else if (personCode != null && !personCode.trim().isEmpty()) {
            person = personMapper.selectOne(
                    new QueryWrapper<SecrecyPerson>().eq("code", personCode.trim()));
            if (person == null) {
                throw new BusinessException("PERSON_NOT_FOUND", "人员不存在：" + personCode.trim());
            }
        } else {
            throw new BusinessException("PERSON_REQUIRED", "证卡号或人员编码至少填一项");
        }
        requireEnabled(person);
        return person;
    }

    public SecrecyPerson requireEnabledByCode(String code, String notFoundMessage) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("PERSON_CODE_REQUIRED", notFoundMessage);
        }
        SecrecyPerson person = personMapper.selectOne(
                new QueryWrapper<SecrecyPerson>().eq("code", code.trim()));
        if (person == null) {
            throw new BusinessException("PERSON_NOT_FOUND", "人员不存在：" + code.trim());
        }
        requireEnabled(person);
        return person;
    }

    private void requireEnabled(SecrecyPerson person) {
        if (!SecrecyConstants.STATUS_ENABLED.equals(person.getStatus())) {
            throw new BusinessException("PERSON_DISABLED",
                    "人员已停用，不能进出保密室：" + person.getCode() + " " + person.getName());
        }
    }

    private String normalizeRole(String roleCode) {
        String role = trimToNull(roleCode);
        if (role == null) {
            return SecrecyConstants.ROLE_STAFF;
        }
        role = role.toUpperCase();
        if (!SecrecyConstants.ROLE_KEEPER.equals(role)
                && !SecrecyConstants.ROLE_REVIEWER.equals(role)
                && !SecrecyConstants.ROLE_STAFF.equals(role)) {
            throw new BusinessException("ROLE_INVALID",
                    "人员角色非法：" + role + "（KEEPER 保管员 / REVIEWER 复核员 / STAFF 普通涉密人员）");
        }
        return role;
    }

    // ---------- 进出时段规则 ----------

    @Transactional
    public SecrecyTimeRule createTimeRule(TimeRuleRequest request) {
        if (request.getEndTime().compareTo(request.getStartTime()) <= 0) {
            throw new BusinessException("TIME_RANGE_INVALID",
                    "允许时段起点必须早于止点：" + request.getStartTime() + " ~ " + request.getEndTime());
        }
        SecrecyTimeRule rule = new SecrecyTimeRule();
        rule.setName(request.getName().trim());
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setWeekdays(normalizeWeekdays(request.getWeekdays()));
        rule.setStatus(SecrecyConstants.STATUS_ENABLED);
        rule.setRemark(request.getRemark());
        timeRuleMapper.insert(rule);
        return rule;
    }

    public List<SecrecyTimeRule> listTimeRules() {
        return timeRuleMapper.selectList(new QueryWrapper<SecrecyTimeRule>().orderByAsc("id"));
    }

    /**
     * 异常时段判定：刷证时间不在任何启用规则覆盖内即异常。
     * 无启用规则时不判异常（未配置口径不误报）。
     */
    public boolean isOffHours(LocalDateTime happenedAt) {
        List<SecrecyTimeRule> rules = timeRuleMapper.selectList(new QueryWrapper<SecrecyTimeRule>()
                .eq("status", SecrecyConstants.STATUS_ENABLED));
        if (rules.isEmpty()) {
            return false;
        }
        LocalTime time = happenedAt.toLocalTime();
        int weekday = happenedAt.getDayOfWeek().getValue();
        for (SecrecyTimeRule rule : rules) {
            if (!weekdayMatches(rule.getWeekdays(), weekday)) {
                continue;
            }
            if (!time.isBefore(rule.getStartTime()) && !time.isAfter(rule.getEndTime())) {
                return false;
            }
        }
        return true;
    }

    private boolean weekdayMatches(String weekdays, int weekday) {
        Set<Integer> days = parseWeekdays(weekdays);
        return days.isEmpty() || days.contains(weekday);
    }

    private String normalizeWeekdays(String weekdays) {
        Set<Integer> days = parseWeekdays(weekdays);
        if (days.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer day : new java.util.TreeSet<>(days)) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(day);
        }
        return sb.toString();
    }

    private Set<Integer> parseWeekdays(String weekdays) {
        Set<Integer> days = new HashSet<>();
        if (weekdays == null || weekdays.trim().isEmpty()) {
            return days;
        }
        for (String part : Arrays.asList(weekdays.split(","))) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int day;
            try {
                day = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                throw new BusinessException("WEEKDAYS_INVALID", "适用星期非法：" + weekdays);
            }
            if (day < 1 || day > 7) {
                throw new BusinessException("WEEKDAYS_INVALID", "适用星期须在 1-7 之间：" + weekdays);
            }
            days.add(day);
        }
        return days;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
