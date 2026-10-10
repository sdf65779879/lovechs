package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入购物车入参。
 */
@Data
@Schema(description = "加入购物车入参")
public class CartDTO {

    @Schema(description = "菜品 id")
    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @Schema(description = "数量，默认 1")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    @Schema(description = "口味选择，如「微辣/正常糖」")
    private String taste;

    @Schema(description = "单品忌口/留言")
    private String remark;
}
