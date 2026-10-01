package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 卷箱建档。
 */
@Data
public class BoxRequest {
    @NotBlank(message = "箱号不能为空")
    private String boxNo;
    @NotBlank(message = "题名不能为空")
    private String title;
    /** SECRET 秘密 / CONFIDENTIAL 机密 / INTERNAL 内部；缺省 INTERNAL。 */
    private String secretLevel;
    private String keeperCode;
    private String remark;
}
