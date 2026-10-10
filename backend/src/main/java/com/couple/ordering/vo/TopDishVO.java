package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门菜品统计出参。
 */
@Data
@Schema(description = "热门菜品")
public class TopDishVO {

    private Long dishId;
    private String dishName;

    @Schema(description = "累计点单份数")
    private Integer count;
}
