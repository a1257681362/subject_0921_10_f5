package com.evops.secrecy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.common.BusinessException;
import com.evops.common.OperatorHolder;
import com.evops.secrecy.common.SecrecyConstants;
import com.evops.secrecy.entity.SecrecyAlert;
import com.evops.secrecy.mapper.SecrecyAlertMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常标记：异常时段进出自动标记给保卫（由 SecrecyAccessService 生成），保卫签收闭环。
 */
@Service
public class SecrecyAlertService {

    private final SecrecyAlertMapper alertMapper;

    public SecrecyAlertService(SecrecyAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    public List<SecrecyAlert> list(String status) {
        QueryWrapper<SecrecyAlert> wrapper = new QueryWrapper<SecrecyAlert>()
                .orderByDesc("happened_at", "id");
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim().toUpperCase());
        }
        return alertMapper.selectList(wrapper);
    }

    /** 保卫签收：NEW → ACKED，记签收人与钟点。 */
    @Transactional
    public SecrecyAlert ack(Long alertId) {
        SecrecyAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("ALERT_NOT_FOUND", "异常标记不存在：id=" + alertId);
        }
        if (SecrecyConstants.ALERT_ACKED.equals(alert.getStatus())) {
            throw new BusinessException("ALERT_ALREADY_ACKED", "异常标记已签收，无需重复处理：" + alert.getAccessNo());
        }
        alert.setStatus(SecrecyConstants.ALERT_ACKED);
        alert.setAckedBy(OperatorHolder.current());
        alert.setAckedAt(LocalDateTime.now());
        alertMapper.updateById(alert);
        return alert;
    }
}
