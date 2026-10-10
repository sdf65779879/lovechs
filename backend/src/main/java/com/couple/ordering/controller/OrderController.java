package com.couple.ordering.controller;

import com.couple.ordering.common.constant.Constants;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.OrderSubmitDTO;
import com.couple.ordering.service.OrderService;
import com.couple.ordering.vo.OrderCalendarVO;
import com.couple.ordering.vo.OrderVO;
import com.couple.ordering.vo.RebuyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户端订单接口。
 */
@Tag(name = "6. 订单（用户端）")
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "提交订单（请求头 Idempotency-Key 幂等）")
    @PostMapping
    public Result<OrderVO> submit(
            @Valid @RequestBody OrderSubmitDTO dto,
            @RequestHeader(value = Constants.IDEMPOTENCY_HEADER, required = false)
            String idempotencyKey) {
        return Result.success(orderService.submit(dto, idempotencyKey));
    }

    @Operation(summary = "我的订单分页（tab: active 进行中 / finished 已完成）")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "active") String tab,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.myPage(tab, pageNum, pageSize));
    }

    @Operation(summary = "吃饭日历（按月）")
    @GetMapping("/calendar")
    public Result<OrderCalendarVO> calendar(
            @RequestParam(required = false) String month) {
        String targetMonth = month == null || month.isBlank()
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : month;
        return Result.success(orderService.calendar(targetMonth));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "取消订单（仅待接单）")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "再次下单（菜品加回购物车）")
    @PostMapping("/{id}/rebuy")
    public Result<RebuyVO> rebuy(@PathVariable Long id) {
        return Result.success(orderService.rebuy(id));
    }
}
