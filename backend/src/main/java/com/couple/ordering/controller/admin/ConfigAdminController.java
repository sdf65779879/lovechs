package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.ConfigUpdateDTO;
import com.couple.ordering.entity.SysConfig;
import com.couple.ordering.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端系统配置接口。
 */
@Tag(name = "13. 系统设置")
@RestController
@RequestMapping("/v1/admin/configs")
@RequiredArgsConstructor
public class ConfigAdminController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "全部配置列表")
    @GetMapping
    public Result<List<SysConfig>> list() {
        return Result.success(sysConfigService.listAll());
    }

    @Operation(summary = "批量更新配置")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ConfigUpdateDTO dto) {
        sysConfigService.updateConfigs(dto.getConfigs());
        return Result.success();
    }
}
