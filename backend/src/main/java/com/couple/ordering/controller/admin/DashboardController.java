package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.service.DashboardService;
import com.couple.ordering.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据看板接口。
 */
@Tag(name = "14. 数据看板")
@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "看板聚合数据")
    @GetMapping
    public Result<DashboardVO> build() {
        return Result.success(dashboardService.build());
    }
}
