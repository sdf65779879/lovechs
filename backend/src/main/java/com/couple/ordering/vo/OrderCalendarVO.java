package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 吃饭日历出参：按月返回有订单的日期。
 */
@Data
@Schema(description = "吃饭日历")
public class OrderCalendarVO {

    @Schema(description = "月份 yyyy-MM")
    private String month;

    @Schema(description = "有订单的日期列表")
    private List<CalendarDay> days;

    /**
     * 日历中的一天。
     */
    @Data
    @Schema(description = "日历日期")
    public static class CalendarDay {

        /** 日期 yyyy-MM-dd */
        private String date;
        private Integer orderCount;
        private Integer totalLove;
        private List<CalendarOrder> orders;
    }

    /**
     * 当日订单摘要。
     */
    @Data
    @Schema(description = "日历订单摘要")
    public static class CalendarOrder {

        private Long id;
        private String orderNo;
        private String status;
        private String statusLabel;
        private Integer totalLove;
    }
}
