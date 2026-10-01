package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 补充说明：登记落库不可改，纠错唯一入口（只追加）。
 */
@Data
public class SupplementRequest {
    @NotBlank(message = "补充说明内容不能为空")
    private String content;
    private String reason;
}
