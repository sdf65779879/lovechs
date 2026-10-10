package com.couple.ordering.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.dto.CartDTO;
import com.couple.ordering.dto.CartUpdateDTO;
import com.couple.ordering.entity.CartItem;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.mapper.CartItemMapper;
import com.couple.ordering.mapper.DishMapper;
import com.couple.ordering.service.CartService;
import com.couple.ordering.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务实现。
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;

    @Override
    public List<CartVO> listMyCart() {
        Long userId = SecurityUtils.getUserId();
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreateTime));
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> dishIds = items.stream().map(CartItem::getDishId).distinct().toList();
        Map<Long, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, dish -> dish));
        return items.stream()
                .map(item -> toVO(item, dishMap.get(item.getDishId())))
                .toList();
    }

    @Override
    public void add(CartDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BusinessException("菜品已下架，不能加入购物车");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        String taste = StrUtil.nullToEmpty(dto.getTaste());
        String remark = StrUtil.nullToEmpty(dto.getRemark());
        CartItem existing = findSame(userId, dish.getId(), taste, remark);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setDishId(dish.getId());
            item.setQuantity(quantity);
            item.setTaste(taste);
            item.setRemark(remark);
            cartItemMapper.insert(item);
        }
    }

    @Override
    public void update(Long cartId, CartUpdateDTO dto) {
        CartItem item = mustOwn(cartId);
        String taste = dto.getTaste() == null
                ? item.getTaste() : StrUtil.nullToEmpty(dto.getTaste());
        String remark = dto.getRemark() == null
                ? item.getRemark() : StrUtil.nullToEmpty(dto.getRemark());
        // 口味/备注变化时需与已有条目合并，避免违反唯一约束
        if (!taste.equals(item.getTaste()) || !remark.equals(item.getRemark())) {
            CartItem existing = findSame(item.getUserId(), item.getDishId(), taste, remark);
            if (existing != null && !existing.getId().equals(item.getId())) {
                existing.setQuantity(existing.getQuantity() + dto.getQuantity());
                cartItemMapper.updateById(existing);
                cartItemMapper.deleteById(item.getId());
                return;
            }
            item.setTaste(taste);
            item.setRemark(remark);
        }
        item.setQuantity(dto.getQuantity());
        cartItemMapper.updateById(item);
    }

    @Override
    public void remove(Long cartId) {
        CartItem item = mustOwn(cartId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    public void clear() {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, SecurityUtils.getUserId()));
    }

    private CartItem findSame(Long userId, Long dishId, String taste, String remark) {
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dishId)
                .eq(CartItem::getTaste, taste)
                .eq(CartItem::getRemark, remark));
    }

    private CartItem mustOwn(Long cartId) {
        CartItem item = cartItemMapper.selectById(cartId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车条目不存在");
        }
        if (!item.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return item;
    }

    private CartVO toVO(CartItem item, Dish dish) {
        CartVO vo = new CartVO();
        vo.setId(item.getId());
        vo.setDishId(item.getDishId());
        vo.setQuantity(item.getQuantity());
        vo.setTaste(item.getTaste());
        vo.setRemark(item.getRemark());
        if (dish != null) {
            vo.setName(dish.getName());
            vo.setImage(dish.getImage());
            vo.setLoveValue(dish.getLoveValue());
            boolean available = dish.getStatus() != null && dish.getStatus() == 1;
            vo.setAvailable(available);
            vo.setLineLove(dish.getLoveValue() * item.getQuantity());
        } else {
            vo.setName("菜品已被删除");
            vo.setLoveValue(0);
            vo.setAvailable(false);
            vo.setLineLove(0);
        }
        return vo;
    }
}
