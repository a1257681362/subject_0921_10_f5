package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 进出时段规则：异常时段判定口径；刷证时间不在任何启用规则内即异常。
 * 无启用规则时不判异常。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_time_rule")
public class SecrecyTimeRule extends BaseEntity {
    private String name;
    /** 每日允许进出时段（起早于止） */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    /** 适用星期（1-7 逗号分隔；空表示每天） */
    private String weekdays;
    /** ENABLED / DISABLED */
    private String status;
    private String remark;
}
