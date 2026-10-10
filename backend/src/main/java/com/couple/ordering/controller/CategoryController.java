package com.couple.ordering.controller;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.service.CategoryService;
import com.couple.ordering.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端分类接口。
 */
@Tag(name = "2. 分类（用户端）")
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "启用中的分类列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
