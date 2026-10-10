package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜品新增/修改入参。
 */
@Data
@Schema(description = "菜品入参")
public class DishDTO {

    @Schema(description = "分类 id")
    @NotNull(message = "请选择分类")
    private Long categoryId;

    @Schema(description = "菜品名称")
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称不能超过 64 个字符")
    private String name;

    @Schema(description = "菜品描述")
    private String description;

    @Schema(description = "图片地址")
    private String image;

    @Schema(description = "标签，英文逗号分隔")
    private String tags;

    @Schema(description = "爱心值")
    @NotNull(message = "请填写爱心值")
    @Min(value = 0, message = "爱心值不能为负数")
    private Integer loveValue;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "是否上架：1 是 / 0 否")
    private Integer status;

    @Schema(description = "是否推荐：1 是 / 0 否")
    private Integer isRecommend;
}
