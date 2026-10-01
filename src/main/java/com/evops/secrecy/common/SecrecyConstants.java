package com.evops.secrecy.common;

/**
 * 保密室双人复核链业务状态常量。
 */
public final class SecrecyConstants {

    private SecrecyConstants() {
    }

    // 人员角色
    public static final String ROLE_KEEPER = "KEEPER";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_STAFF = "STAFF";

    // 通用启用状态
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    // 出入方向
    public static final String DIRECTION_IN = "IN";
    public static final String DIRECTION_OUT = "OUT";

    // 出入登记状态（当前版本状态）
    public static final String ACCESS_PENDING = "PENDING_REVIEW";
    public static final String ACCESS_CONFIRMED = "CONFIRMED";
    public static final String ACCESS_REJECTED = "REJECTED";

    // 出入登记版本变更类型
    public static final String CHANGE_REGISTER = "REGISTER";
    public static final String CHANGE_CONFIRM = "CONFIRM";
    public static final String CHANGE_REJECT = "REJECT";

    // 登记来源
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_PAPER_IMPORT = "PAPER_IMPORT";

    // 卷箱状态
    public static final String BOX_IN_ROOM = "IN_ROOM";
    public static final String BOX_OUT_OF_ROOM = "OUT_OF_ROOM";

    // 卷箱出入方向（随人员方向：人出=带出，人进=带回）
    public static final String MOVE_TAKE_OUT = "TAKE_OUT";
    public static final String MOVE_BRING_IN = "BRING_IN";

    // 异常标记类型与状态
    public static final String ALERT_OFF_HOURS = "OFF_HOURS";
    public static final String ALERT_NEW = "NEW";
    public static final String ALERT_ACKED = "ACKED";
}
