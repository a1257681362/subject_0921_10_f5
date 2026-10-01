package com.evops.secrecy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.common.BusinessException;
import com.evops.secrecy.common.SecrecyConstants;
import com.evops.secrecy.dto.BoxRequest;
import com.evops.secrecy.entity.SecrecyAccess;
import com.evops.secrecy.entity.SecrecyBox;
import com.evops.secrecy.entity.SecrecyBoxMove;
import com.evops.secrecy.mapper.SecrecyBoxMapper;
import com.evops.secrecy.mapper.SecrecyBoxMoveMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 卷箱：建档与轨迹查询；出入记录跟人员进出挂钩——
 * 随登记创建、随复核联动（由 SecrecyAccessService 调起），不单独开放出入接口。
 */
@Service
public class SecrecyBoxService {

    private final SecrecyBoxMapper boxMapper;
    private final SecrecyBoxMoveMapper moveMapper;

    public SecrecyBoxService(SecrecyBoxMapper boxMapper,
                             SecrecyBoxMoveMapper moveMapper) {
        this.boxMapper = boxMapper;
        this.moveMapper = moveMapper;
    }

    // ---------- 建档与查询 ----------

    @Transactional
    public SecrecyBox createBox(BoxRequest request) {
        SecrecyBox box = new SecrecyBox();
        box.setBoxNo(request.getBoxNo().trim());
        box.setTitle(request.getTitle().trim());
        box.setSecretLevel(normalizeSecretLevel(request.getSecretLevel()));
        box.setStatus(SecrecyConstants.BOX_IN_ROOM);
        box.setKeeperCode(trimToNull(request.getKeeperCode()));
        box.setRemark(request.getRemark());
        try {
            boxMapper.insert(box);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("DUPLICATE_BOX_NO", "箱号已存在：" + box.getBoxNo());
        }
        return box;
    }

    public List<SecrecyBox> listBoxes(String status) {
        QueryWrapper<SecrecyBox> wrapper = new QueryWrapper<SecrecyBox>().orderByAsc("box_no");
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }
        return boxMapper.selectList(wrapper);
    }

    public SecrecyBox getBox(Long id) {
        SecrecyBox box = boxMapper.selectById(id);
        if (box == null) {
            throw new BusinessException("BOX_NOT_FOUND", "卷箱不存在：id=" + id);
        }
        return box;
    }

    public List<SecrecyBoxMove> listMoves(Long boxId) {
        getBox(boxId);
        return moveMapper.selectList(new QueryWrapper<SecrecyBoxMove>()
                .eq("box_id", boxId)
                .orderByAsc("happened_at", "id"));
    }

    public List<SecrecyBoxMove> movesOfAccess(Long accessId) {
        return moveMapper.selectList(new QueryWrapper<SecrecyBoxMove>()
                .eq("access_id", accessId)
                .orderByAsc("id"));
    }

    // ---------- 跟人员进出挂钩（由出入登记服务调起） ----------

    /**
     * 刷证随带卷箱校验：人出=带出（箱须在室）、人进=带回（箱须出室）；
     * 箱号不重复、箱上没有待复核的出入记录（防止一箱两挂）。
     */
    public List<SecrecyBox> validateForSwipe(List<String> boxNos, String direction) {
        List<SecrecyBox> boxes = new ArrayList<>();
        if (boxNos == null || boxNos.isEmpty()) {
            return boxes;
        }
        Set<String> seen = new HashSet<>();
        for (String raw : boxNos) {
            String boxNo = raw == null ? "" : raw.trim();
            if (boxNo.isEmpty()) {
                throw new BusinessException("BOX_NO_REQUIRED", "随带卷箱箱号不能为空");
            }
            if (!seen.add(boxNo)) {
                throw new BusinessException("BOX_DUPLICATED_IN_SWIPE", "同一次刷证卷箱重复：" + boxNo);
            }
            SecrecyBox box = boxMapper.selectOne(
                    new QueryWrapper<SecrecyBox>().eq("box_no", boxNo));
            if (box == null) {
                throw new BusinessException("BOX_NOT_FOUND", "卷箱不存在：" + boxNo);
            }
            boolean takeOut = SecrecyConstants.DIRECTION_OUT.equals(direction);
            if (takeOut && !SecrecyConstants.BOX_IN_ROOM.equals(box.getStatus())) {
                throw new BusinessException("BOX_NOT_IN_ROOM",
                        "卷箱不在室，不能带出：" + boxNo + "（当前 " + box.getStatus() + "）");
            }
            if (!takeOut && !SecrecyConstants.BOX_OUT_OF_ROOM.equals(box.getStatus())) {
                throw new BusinessException("BOX_NOT_OUT_OF_ROOM",
                        "卷箱未出室，不能登记带回：" + boxNo + "（当前 " + box.getStatus() + "）");
            }
            Long pending = moveMapper.selectCount(new QueryWrapper<SecrecyBoxMove>()
                    .eq("box_id", box.getId())
                    .eq("status", SecrecyConstants.ACCESS_PENDING));
            if (pending != null && pending > 0) {
                throw new BusinessException("BOX_MOVE_PENDING",
                        "卷箱有待复核的出入记录，不能重复挂接：" + boxNo);
            }
            boxes.add(box);
        }
        return boxes;
    }

    /** 随登记创建卷箱出入记录（待复核，随复核联动生效）。 */
    @Transactional
    public List<SecrecyBoxMove> createMoves(SecrecyAccess access, List<SecrecyBox> boxes, String operator) {
        List<SecrecyBoxMove> moves = new ArrayList<>();
        for (SecrecyBox box : boxes) {
            SecrecyBoxMove move = new SecrecyBoxMove();
            move.setBoxId(box.getId());
            move.setBoxNo(box.getBoxNo());
            move.setAccessId(access.getId());
            move.setAccessNo(access.getAccessNo());
            move.setDirection(SecrecyConstants.DIRECTION_OUT.equals(access.getDirection())
                    ? SecrecyConstants.MOVE_TAKE_OUT : SecrecyConstants.MOVE_BRING_IN);
            move.setStatus(SecrecyConstants.ACCESS_PENDING);
            move.setHappenedAt(access.getHappenedAt());
            move.setOperator(operator);
            moveMapper.insert(move);
            moves.add(move);
        }
        return moves;
    }

    /** 复核联动：确认 → 出入记录生效并翻卷箱在室状态；拒绝 → 记录作废、箱状态不动。 */
    @Transactional
    public void syncMovesOnReview(SecrecyAccess access, boolean confirmed) {
        List<SecrecyBoxMove> moves = moveMapper.selectList(new QueryWrapper<SecrecyBoxMove>()
                .eq("access_id", access.getId())
                .eq("status", SecrecyConstants.ACCESS_PENDING));
        for (SecrecyBoxMove move : moves) {
            move.setStatus(confirmed ? SecrecyConstants.ACCESS_CONFIRMED : SecrecyConstants.ACCESS_REJECTED);
            moveMapper.updateById(move);
            if (confirmed) {
                SecrecyBox box = boxMapper.selectById(move.getBoxId());
                if (box != null) {
                    box.setStatus(SecrecyConstants.MOVE_TAKE_OUT.equals(move.getDirection())
                            ? SecrecyConstants.BOX_OUT_OF_ROOM : SecrecyConstants.BOX_IN_ROOM);
                    boxMapper.updateById(box);
                }
            }
        }
    }

    private String normalizeSecretLevel(String secretLevel) {
        String level = trimToNull(secretLevel);
        if (level == null) {
            return "INTERNAL";
        }
        level = level.toUpperCase();
        if (!"SECRET".equals(level) && !"CONFIDENTIAL".equals(level) && !"INTERNAL".equals(level)) {
            throw new BusinessException("SECRET_LEVEL_INVALID",
                    "密级非法：" + level + "（SECRET 秘密 / CONFIDENTIAL 机密 / INTERNAL 内部）");
        }
        return level;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
