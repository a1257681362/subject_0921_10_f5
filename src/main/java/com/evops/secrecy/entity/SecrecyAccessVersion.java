package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 出入登记版本：登记 / 复核确认 / 复核拒绝各追加一版，永不修改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_access_version")
public class SecrecyAccessVersion extends BaseEntity {
    private Long accessId;
    private Integer versionNo;
    /** 本版状态：PENDING_REVIEW / CONFIRMED / REJECTED */
    private String status;
    /** REGISTER 登记 / CONFIRM 复核确认 / REJECT 复核拒绝 */
    private String changeType;
    private String reviewerCode;
    private String reviewerName;
    private String changeReason;
    private String operator;
    private LocalDateTime changedAt;
}
