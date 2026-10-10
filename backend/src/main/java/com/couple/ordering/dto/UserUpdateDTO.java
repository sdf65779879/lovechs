package com.couple.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员编辑用户入参。
 */
@Data
@Schema(description = "编辑用户入参")
public class UserUpdateDTO {

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称不能超过 32 个字符")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "角色：USER / ADMIN")
    private String role;
}
