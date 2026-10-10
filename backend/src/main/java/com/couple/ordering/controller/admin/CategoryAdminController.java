package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.CategoryDTO;
import com.couple.ordering.dto.StatusDTO;
import com.couple.ordering.service.CategoryService;
import com.couple.ordering.vo.CategoryVO;
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

import java.util.List;

/**
 * 管理端分类管理接口。
 */
@Tag(name = "9. 分类管理")
@RestController
@RequestMapping("/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    @Operation(summary = "分类分页")
    @GetMapping
    public Result<PageResult<CategoryVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(categoryService.adminPage(pageNum, pageSize));
    }

    @Operation(summary = "全部分类（下拉选择）")
    @GetMapping("/all")
    public Result<List<CategoryVO>> all() {
        return Result.success(categoryService.adminListAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除分类（有菜品时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        categoryService.remove(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用分类")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusDTO dto) {
        categoryService.toggleStatus(id, dto.getStatus());
        return Result.success();
    }
}
