package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单出参。
 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;

    @Schema(description = "下单用户昵称（管理端使用）")
    private String nickname;

    private String status;

    @Schema(description = "状态中文文案")
    private String statusLabel;

    private Integer totalLove;
    private String remark;
    private LocalDateTime expectTime;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime acceptTime;
    private LocalDateTime finishTime;

    @Schema(description = "订单明细列表")
    private List<OrderItemVO> items;
}
