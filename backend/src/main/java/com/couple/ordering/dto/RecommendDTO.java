package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推荐标记切换入参。
 */
@Data
@Schema(description = "推荐切换入参")
public class RecommendDTO {

    @Schema(description = "是否推荐：1 / 0")
    @NotNull(message = "推荐标记不能为空")
    private Integer isRecommend;
}
