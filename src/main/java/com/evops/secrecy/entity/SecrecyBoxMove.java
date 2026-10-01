package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 卷箱出入记录：跟人员进出挂钩——随登记创建、随复核联动，不单独开口子。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_box_move")
public class SecrecyBoxMove extends BaseEntity {
    private Long boxId;
    private String boxNo;
    /** 挂钩的人员出入登记 */
    private Long accessId;
    private String accessNo;
    /** TAKE_OUT 带出（人出） / BRING_IN 带回（人进） */
    private String direction;
    /** 随人员登记联动：PENDING_REVIEW / CONFIRMED / REJECTED */
    private String status;
    private LocalDateTime happenedAt;
    private String operator;
}
