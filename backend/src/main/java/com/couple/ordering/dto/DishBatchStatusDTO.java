package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 菜品批量上下架入参。
 */
@Data
@Schema(description = "菜品批量状态入参")
public class DishBatchStatusDTO {

    @Schema(description = "菜品 id 集合")
    @NotEmpty(message = "请至少选择一个菜品")
    private List<Long> ids;

    @Schema(description = "目标状态：1 上架 / 0 下架")
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
