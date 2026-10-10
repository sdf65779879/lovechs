package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜品出参。
 */
@Data
@Schema(description = "菜品信息")
public class DishVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String image;

    @Schema(description = "标签原始字符串（逗号分隔）")
    private String tags;

    @Schema(description = "标签数组，便于前端渲染")
    private List<String> tagList;

    private Integer loveValue;
    private Integer sort;
    private Integer status;
    private Integer isRecommend;

    @Schema(description = "当前用户是否已收藏")
    private Boolean favorite;
}
