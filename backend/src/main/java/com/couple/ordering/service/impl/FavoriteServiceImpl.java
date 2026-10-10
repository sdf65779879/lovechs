package com.couple.ordering.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.entity.Favorite;
import com.couple.ordering.mapper.DishMapper;
import com.couple.ordering.mapper.FavoriteMapper;
import com.couple.ordering.service.FavoriteService;
import com.couple.ordering.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final DishMapper dishMapper;

    @Override
    public List<FavoriteVO> list() {
        Long userId = SecurityUtils.getUserId();
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> dishIds = favorites.stream().map(Favorite::getDishId).distinct().toList();
        Map<Long, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, dish -> dish));
        List<FavoriteVO> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Dish dish = dishMap.get(favorite.getDishId());
            // 菜品被（逻辑）删除则不再展示
            if (dish == null) {
                continue;
            }
            FavoriteVO vo = new FavoriteVO();
            BeanUtil.copyProperties(dish, vo);
            vo.setTagList(StrUtil.split(dish.getTags(), ',').stream()
                    .map(StrUtil::trim).filter(StrUtil::isNotBlank).toList());
            vo.setFavorite(true);
            result.add(vo);
        }
        return result;
    }

    @Override
    public void add(Long dishId) {
        Long userId = SecurityUtils.getUserId();
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDishId, dishId));
        if (exists == 0) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setDishId(dishId);
            favoriteMapper.insert(favorite);
        }
    }

    @Override
    public void remove(Long dishId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, SecurityUtils.getUserId())
                .eq(Favorite::getDishId, dishId));
    }
}
