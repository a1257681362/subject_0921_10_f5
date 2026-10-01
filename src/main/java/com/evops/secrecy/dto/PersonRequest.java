package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 保密室准入人员建档。
 */
@Data
public class PersonRequest {
    @NotBlank(message = "人员编码不能为空")
    private String code;
    @NotBlank(message = "人员姓名不能为空")
    private String name;
    @NotBlank(message = "证卡号不能为空")
    private String cardNo;
    /** KEEPER 保管员 / REVIEWER 复核员 / STAFF 普通涉密人员；缺省 STAFF。 */
    private String roleCode;
    private String phone;
    private String remark;
}
