package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日订单趋势出参。
 */
@Data
@Schema(description = "订单趋势")
public class TrendVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 当日订单数 */
    private Integer count;
}
