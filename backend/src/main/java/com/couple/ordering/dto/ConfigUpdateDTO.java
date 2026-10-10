package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 批量更新系统配置入参：key → value。
 */
@Data
@Schema(description = "配置更新入参")
public class ConfigUpdateDTO {

    @Schema(description = "配置键值集合")
    @NotEmpty(message = "配置内容不能为空")
    private Map<String, String> configs;
}
