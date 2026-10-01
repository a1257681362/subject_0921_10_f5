package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 纸面存量导入条目：纸面记录为既成事实，导入即为已生效历史。
 */
@Data
public class AccessImportItem {
    @NotBlank(message = "人员编码不能为空")
    private String personCode;
    @NotBlank(message = "进出方向不能为空（IN 进 / OUT 出）")
    private String direction;
    @NotNull(message = "原始登记时间不能为空")
    private LocalDateTime happenedAt;
    @NotBlank(message = "第二人编码不能为空")
    private String secondPersonCode;
    private String remark;
}
