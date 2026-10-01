package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 复核请求：确认 / 拒绝共用。复核人必须是登记时指定的第二人本人。
 */
@Data
public class ReviewRequest {
    @NotBlank(message = "复核人编码不能为空")
    private String reviewerCode;
    /** 拒绝时必填原因。 */
    private String reason;
}
