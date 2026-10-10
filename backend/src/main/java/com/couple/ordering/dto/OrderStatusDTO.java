package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员更新订单状态入参。
 */
@Data
@Schema(description = "订单状态更新入参")
public class OrderStatusDTO {

    @Schema(description = "目标状态：ACCEPTED / COOKING / COMPLETED / REJECTED")
    @NotBlank(message = "目标状态不能为空")
    private String status;

    @Schema(description = "拒绝理由（状态为 REJECTED 时必填）")
    private String rejectReason;
}
