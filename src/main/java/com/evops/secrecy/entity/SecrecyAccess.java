package com.evops.secrecy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 出入登记模块（主表）：登记落库不可改，纠错走补充说明；
 * 金额/状态取当前版本，版本链可追溯（t_secrecy_access_version 只追加）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_secrecy_access")
public class SecrecyAccess extends BaseEntity {
    /** 登记号：SE + 日期 + 4 位流水 */
    private String accessNo;
    private Long personId;
    private String personCode;
    private String personName;
    private String cardNo;
    /** IN 进室 / OUT 出室 */
    private String direction;
    /** 刷证时间（纸面导入时为原始登记时间） */
    private LocalDateTime happenedAt;
    /** 双人双锁：登记时指定的第二人，复核确认须其本人 */
    private Long secondPersonId;
    private String secondPersonCode;
    private String secondPersonName;
    /** 当前版本号（从 1 起） */
    private Integer currentVersion;
    /** PENDING_REVIEW 待复核 / CONFIRMED 已生效 / REJECTED 复核不通过 */
    private String status;
    /** MANUAL 现场刷证 / PAPER_IMPORT 纸面存量导入 */
    private String source;
    /** 异常时段标记 */
    private Boolean offHours;
    private Integer boxCount;
    private String remark;
    private String operator;
}
