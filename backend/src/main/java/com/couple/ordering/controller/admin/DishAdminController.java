package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.DishBatchStatusDTO;
import com.couple.ordering.dto.DishDTO;
import com.couple.ordering.dto.RecommendDTO;
import com.couple.ordering.dto.StatusDTO;
import com.couple.ordering.service.DishService;
import com.couple.ordering.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端菜品管理接口。
 */
@Tag(name = "10. 菜品管理")
@RestController
@RequestMapping("/v1/admin/dishes")
@RequiredArgsConstructor
public class DishAdminController {

    private final DishService dishService;

    @Operation(summary = "菜品分页（支持分类/状态/关键词）")
    @GetMapping
    public Result<PageResult<DishVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(
                dishService.adminPage(categoryId, status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增菜品")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DishDTO dto) {
        return Result.success(dishService.create(dto));
    }

    @Operation(summary = "修改菜品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody DishDTO dto) {
        dishService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除菜品（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dishService.remove(id);
        return Result.success();
    }

    @Operation(summary = "上架/下架")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusDTO dto) {
        dishService.toggleStatus(id, dto.getStatus());
        return Result.success();
    }

    @Operation(summary = "设置/取消推荐")
    @PutMapping("/{id}/recommend")
    public Result<Void> toggleRecommend(@PathVariable Long id,
                                        @Valid @RequestBody RecommendDTO dto) {
        dishService.toggleRecommend(id, dto.getIsRecommend());
        return Result.success();
    }

    @Operation(summary = "批量上架/下架")
    @PutMapping("/batch/status")
    public Result<Void> batchStatus(@Valid @RequestBody DishBatchStatusDTO dto) {
        dishService.batchStatus(dto.getIds(), dto.getStatus());
        return Result.success();
    }
}
