package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保密室准入人员：刷证进出按证卡号定位。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_person")
public class SecrecyPerson extends BaseEntity {
    private String code;
    private String name;
    /** 证卡号（刷证用，唯一） */
    private String cardNo;
    /** KEEPER 保管员 / REVIEWER 复核员 / STAFF 普通涉密人员 */
    private String roleCode;
    private String phone;
    /** ENABLED / DISABLED */
    private String status;
    private String remark;
}
