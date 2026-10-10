package com.couple.ordering.controller;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.service.SysConfigService;
import com.couple.ordering.vo.ConfigPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端配置接口。
 */
@Tag(name = "8. 配置（用户端）")
@RestController
@RequestMapping("/v1/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "公开配置（营业状态 + 公告）")
    @GetMapping("/public")
    public Result<ConfigPublicVO> publicConfig() {
        return Result.success(sysConfigService.publicConfig());
    }
}
