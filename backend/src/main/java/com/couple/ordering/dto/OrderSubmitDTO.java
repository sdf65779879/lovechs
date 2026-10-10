package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交订单入参。订单菜品来源于当前用户购物车。
 */
@Data
@Schema(description = "提交订单入参")
public class OrderSubmitDTO {

    @Schema(description = "整单备注（口味/忌口/撒娇留言）")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    @Schema(description = "期望用餐时间，格式 yyyy-MM-dd HH:mm；留空表示尽快",
            example = "2026-10-10 18:30")
    private String expectTime;
}
