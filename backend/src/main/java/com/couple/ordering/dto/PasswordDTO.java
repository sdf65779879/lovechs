package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码入参。
 */
@Data
@Schema(description = "修改密码入参")
public class PasswordDTO {

    @Schema(description = "旧密码")
    @NotBlank(message = "请输入旧密码")
    private String oldPassword;

    @Schema(description = "新密码（6~32 位）")
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在 6~32 位之间")
    private String newPassword;
}
