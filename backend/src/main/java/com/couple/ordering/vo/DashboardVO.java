package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理端数据看板出参。
 */
@Data
@Schema(description = "数据看板")
public class DashboardVO {

    @Schema(description = "今日订单数")
    private Integer todayOrderCount;

    @Schema(description = "待处理（待接单）订单数")
    private Integer pendingCount;

    @Schema(description = "累计订单数")
    private Integer totalCount;

    @Schema(description = "今日完成订单数")
    private Integer todayCompletedCount;

    @Schema(description = "最常点菜品 Top 5")
    private List<TopDishVO> topDishes;

    @Schema(description = "最近 7 天订单趋势")
    private List<TrendVO> trend;
}
