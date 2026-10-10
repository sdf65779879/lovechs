package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参。
 */
@Data
@Schema(description = "登录入参")
public class LoginDTO {

    @Schema(description = "账号", example = "love")
    @NotBlank(message = "请输入账号")
    private String username;

    @Schema(description = "密码", example = "love123")
    @NotBlank(message = "请输入密码")
    private String password;

    @Schema(description = "记住我（前端控制 Token 持久化策略）")
    private Boolean remember;
}
