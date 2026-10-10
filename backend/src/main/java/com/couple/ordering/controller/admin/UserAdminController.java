package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.ResetPasswordDTO;
import com.couple.ordering.dto.StatusDTO;
import com.couple.ordering.dto.UserCreateDTO;
import com.couple.ordering.dto.UserUpdateDTO;
import com.couple.ordering.service.SysUserService;
import com.couple.ordering.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理接口。
 */
@Tag(name = "12. 用户管理")
@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户分页")
    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(
                sysUserService.adminPage(pageNum, pageSize, keyword));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(sysUserService.createUser(dto));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateDTO dto) {
        sysUserService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusDTO dto) {
        sysUserService.toggleStatus(id, dto.getStatus());
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(id, dto.getPassword());
        return Result.success();
    }
}
