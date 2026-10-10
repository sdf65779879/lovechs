package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增用户入参。
 */
@Data
@Schema(description = "新增用户入参")
public class UserCreateDTO {

    @Schema(description = "登录账号（3~32 位）")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 32, message = "账号长度需在 3~32 位之间")
    private String username;

    @Schema(description = "初始密码（6~32 位）")
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 位之间")
    private String password;

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称不能超过 32 个字符")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "角色：USER / ADMIN，默认 USER")
    private String role;
}
