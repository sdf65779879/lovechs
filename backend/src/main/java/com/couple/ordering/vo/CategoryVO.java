package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分类出参。
 */
@Data
@Schema(description = "分类信息")
public class CategoryVO {

    private Long id;
    private String name;
    private Integer sort;
    private Integer status;

    @Schema(description = "该分类下菜品数量（管理端列表使用）")
    private Integer dishCount;
}
