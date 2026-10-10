package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.OrderStatusDTO;
import com.couple.ordering.service.OrderService;
import com.couple.ordering.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单管理接口。
 */
@Tag(name = "11. 订单管理")
@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @Operation(summary = "订单分页（状态/日期筛选）")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(
                orderService.adminPage(status, date, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.adminDetail(id));
    }

    @Operation(summary = "更新订单状态（接单/制作/完成/拒绝）")
    @PutMapping("/{id}/status")
    public Result<OrderVO> changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody OrderStatusDTO dto) {
        return Result.success(orderService.changeStatus(id, dto));
    }
}
