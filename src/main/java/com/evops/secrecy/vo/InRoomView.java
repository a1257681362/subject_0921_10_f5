package com.evops.secrecy.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在室清单：按已生效（CONFIRMED）进出实时推导；待复核登记不计入，单列计数。
 */
@Data
public class InRoomView {
    /** 当前在室人数（已生效口径）。 */
    private int inRoomCount;
    /** 待复核登记数（未生效，不计入在室）。 */
    private int pendingCount;
    private List<InRoomPerson> persons;

    @Data
    public static class InRoomPerson {
        private String personCode;
        private String personName;
        /** 最近一次已生效进室时间。 */
        private LocalDateTime enteredAt;
        private String accessNo;
    }
}
