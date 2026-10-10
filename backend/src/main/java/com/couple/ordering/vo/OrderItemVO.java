package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单明细出参（下单时快照）。
 */
@Data
@Schema(description = "订单明细")
public class OrderItemVO {

    private Long dishId;
    private String dishName;
    private String dishImage;
    private Integer loveValue;
    private Integer quantity;
    private String taste;
    private String remark;

    @Schema(description = "该明细小计爱心值")
    private Integer lineLove;
}
