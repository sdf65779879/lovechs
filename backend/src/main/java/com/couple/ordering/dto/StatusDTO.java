package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态切换入参（启用/禁用、上架/下架）。
 */
@Data
@Schema(description = "状态切换入参")
public class StatusDTO {

    @Schema(description = "目标状态：1 / 0")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
