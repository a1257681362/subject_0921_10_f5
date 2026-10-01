package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卷箱：在室/出室状态由已生效的出入登记联动维护。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_box")
public class SecrecyBox extends BaseEntity {
    private String boxNo;
    private String title;
    /** SECRET 秘密 / CONFIDENTIAL 机密 / INTERNAL 内部 */
    private String secretLevel;
    /** IN_ROOM 在室 / OUT_OF_ROOM 出室 */
    private String status;
    private String keeperCode;
    private String remark;
}
