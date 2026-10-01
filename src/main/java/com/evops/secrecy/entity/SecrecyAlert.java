package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 异常标记：异常时段进出自动标记给保卫；保卫签收闭环。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_alert")
public class SecrecyAlert extends BaseEntity {
    private Long accessId;
    private String accessNo;
    /** OFF_HOURS 异常时段进出 */
    private String alertType;
    private String personCode;
    private String personName;
    private LocalDateTime happenedAt;
    private String content;
    /** NEW 待签收 / ACKED 已签收 */
    private String status;
    private String ackedBy;
    private LocalDateTime ackedAt;
}
