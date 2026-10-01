package com.evops.secrecy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 进出时段规则建档：刷证时间不在任何启用规则内即判异常时段。
 */
@Data
public class TimeRuleRequest {
    @NotBlank(message = "规则名称不能为空")
    private String name;
    @NotNull(message = "允许时段起点不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    @NotNull(message = "允许时段止点不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    /** 适用星期（1-7 逗号分隔，如 "1,2,3,4,5"；空表示每天）。 */
    private String weekdays;
    private String remark;
}
