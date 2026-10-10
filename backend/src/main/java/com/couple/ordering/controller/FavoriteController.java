package com.couple.ordering.controller;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.service.FavoriteService;
import com.couple.ordering.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜品收藏接口。
 */
@Tag(name = "5. 收藏")
@RestController
@RequestMapping("/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "我的收藏列表")
    @GetMapping
    public Result<List<FavoriteVO>> list() {
        return Result.success(favoriteService.list());
    }

    @Operation(summary = "收藏菜品")
    @PostMapping("/{dishId}")
    public Result<Void> add(@PathVariable Long dishId) {
        favoriteService.add(dishId);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{dishId}")
    public Result<Void> remove(@PathVariable Long dishId) {
        favoriteService.remove(dishId);
        return Result.success();
    }
}
