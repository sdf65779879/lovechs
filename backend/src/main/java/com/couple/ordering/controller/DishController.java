package com.couple.ordering.controller;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.service.DishService;
import com.couple.ordering.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端菜品接口。
 */
@Tag(name = "3. 菜品（用户端）")
@RestController
@RequestMapping("/v1/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @Operation(summary = "菜品分页（仅上架，支持分类与关键词）")
    @GetMapping
    public Result<PageResult<DishVO>> page(
            @Parameter(description = "分类 id") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "菜品名关键词") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(dishService.userPage(categoryId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "推荐菜品")
    @GetMapping("/recommend")
    public Result<List<DishVO>> recommend(
            @RequestParam(required = false, defaultValue = "8") Integer limit) {
        return Result.success(dishService.recommend(limit));
    }

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.success(dishService.detail(id));
    }
}
