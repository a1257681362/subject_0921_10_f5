package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登记补充说明：纠错唯一入口，只追加，登记本体永不修改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_access_supplement")
public class SecrecyAccessSupplement extends BaseEntity {
    private Long accessId;
    private String content;
    private String reason;
    private String operator;
    private LocalDateTime supplementedAt;
}
