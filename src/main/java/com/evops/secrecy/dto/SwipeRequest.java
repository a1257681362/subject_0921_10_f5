package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷证登记：进出刷证 + 指定第二人（双人双锁）；可随带卷箱。
 */
@Data
public class SwipeRequest {
    /** 证卡号（与 personCode 二选一，卡号优先）。 */
    private String cardNo;
    /** 人员编码（无卡环境兜底）。 */
    private String personCode;
    @NotBlank(message = "进出方向不能为空（IN 进 / OUT 出）")
    private String direction;
    @NotBlank(message = "第二人编码不能为空（双人双锁，复核确认须其本人）")
    private String secondPersonCode;
    /** 刷证时间；缺省当前时间。 */
    private LocalDateTime happenedAt;
    /** 随带卷箱箱号：人出=带出、人进=带回，随登记复核联动生效。 */
    private List<String> boxNos;
    private String remark;
}
