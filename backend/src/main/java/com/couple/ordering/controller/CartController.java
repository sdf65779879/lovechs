package com.couple.ordering.controller;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.CartDTO;
import com.couple.ordering.dto.CartUpdateDTO;
import com.couple.ordering.service.CartService;
import com.couple.ordering.vo.CartVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口。
 */
@Tag(name = "4. 购物车")
@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "我的购物车")
    @GetMapping
    public Result<List<CartVO>> list() {
        return Result.success(cartService.listMyCart());
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    @Operation(summary = "修改购物车条目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CartUpdateDTO dto) {
        cartService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除购物车条目")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }
}
