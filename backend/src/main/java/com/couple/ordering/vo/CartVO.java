package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 购物车条目出参（含菜品实时信息）。
 */
@Data
@Schema(description = "购物车条目")
public class CartVO {

    @Schema(description = "购物车条目 id")
    private Long id;

    private Long dishId;
    private String name;
    private String image;

    @Schema(description = "当前爱心值（随菜品实时变化）")
    private Integer loveValue;

    private Integer quantity;
    private String taste;
    private String remark;

    @Schema(description = "菜品是否仍上架可购买")
    private Boolean available;

    @Schema(description = "该条目小计爱心值")
    private Integer lineLove;
}
