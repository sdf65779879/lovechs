package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 收藏菜品出参。
 */
@Data
@Schema(description = "收藏菜品")
public class FavoriteVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String image;
    private String tags;
    private List<String> tagList;
    private Integer loveValue;
    private Integer sort;
    private Integer status;
    private Integer isRecommend;

    @Schema(description = "是否已收藏（恒为 true）")
    private Boolean favorite;
}
