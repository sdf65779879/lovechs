package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车条目入参。
 */
@Data
@Schema(description = "购物车修改入参")
public class CartUpdateDTO {

    @Schema(description = "目标数量")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    @Schema(description = "口味选择（可选修改）")
    private String taste;

    @Schema(description = "单品备注（可选修改）")
    private String remark;
}
