package com.couple.ordering.controller;

import com.couple.ordering.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口（容器健康检查使用）。
 */
@Tag(name = "0. 健康检查")
@RestController
@RequestMapping("/v1/health")
public class HealthController {

    @Operation(summary = "服务健康检查")
    @GetMapping
    public Result<String> health() {
        return Result.success("ok");
    }
}
