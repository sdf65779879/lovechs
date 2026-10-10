package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/修改入参。
 */
@Data
@Schema(description = "分类入参")
public class CategoryDTO {

    @Schema(description = "分类名称", example = "主食")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称不能超过 32 个字符")
    private String name;

    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

    @Schema(description = "状态：1 启用 / 0 禁用", example = "1")
    private Integer status;
}
