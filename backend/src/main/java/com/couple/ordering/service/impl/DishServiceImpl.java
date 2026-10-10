package com.couple.ordering.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.dto.DishDTO;
import com.couple.ordering.entity.Category;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.entity.Favorite;
import com.couple.ordering.mapper.CategoryMapper;
import com.couple.ordering.mapper.DishMapper;
import com.couple.ordering.mapper.FavoriteMapper;
import com.couple.ordering.service.DishService;
import com.couple.ordering.vo.DishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜品服务实现。
 */
@Service
@RequiredArgsConstructor
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish>
        implements DishService {

    private final FavoriteMapper favoriteMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResult<DishVO> userPage(Long categoryId, String keyword,
                                       Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtils.getUserId();
        Page<Dish> page = new Page<>(pn(pageNum), ps(pageSize));
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Dish::getName, keyword)
                .orderByAsc(Dish::getSort).orderByAsc(Dish::getId);
        Page<Dish> result = getBaseMapper().selectPage(page, wrapper);
        List<DishVO> list = result.getRecords().stream()
                .map(dish -> toVO(dish, userId)).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public List<DishVO> recommend(Integer limit) {
        Long userId = SecurityUtils.getUserId();
        int size = limit == null || limit < 1 ? 8 : Math.min(limit, 20);
        List<Dish> dishes = getBaseMapper().selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .eq(Dish::getIsRecommend, 1)
                .orderByAsc(Dish::getSort).orderByAsc(Dish::getId)
                .last("LIMIT " + size));
        return dishes.stream().map(dish -> toVO(dish, userId)).toList();
    }

    @Override
    public DishVO detail(Long id) {
        Dish dish = getBaseMapper().selectById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        return toVO(dish, SecurityUtils.getUserId());
    }

    @Override
    public PageResult<DishVO> adminPage(Long categoryId, Integer status, String keyword,
                                        Integer pageNum, Integer pageSize) {
        Page<Dish> page = new Page<>(pn(pageNum), ps(pageSize));
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(status != null, Dish::getStatus, status)
                .like(StringUtils.hasText(keyword), Dish::getName, keyword)
                .orderByAsc(Dish::getSort).orderByAsc(Dish::getId);
        Page<Dish> result = getBaseMapper().selectPage(page, wrapper);
        List<DishVO> list = result.getRecords().stream()
                .map(dish -> toVO(dish, null)).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public Long create(DishDTO dto) {
        checkCategory(dto.getCategoryId());
        Dish dish = new Dish();
        BeanUtil.copyProperties(dto, dish);
        dish.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dish.setStatus(dto.getStatus() == null ? 1
                : normalize01(dto.getStatus(), "上架状态"));
        dish.setIsRecommend(dto.getIsRecommend() == null ? 0
                : normalize01(dto.getIsRecommend(), "推荐标记"));
        getBaseMapper().insert(dish);
        return dish.getId();
    }

    @Override
    public void update(Long id, DishDTO dto) {
        Dish exist = mustGet(id);
        checkCategory(dto.getCategoryId());
        BeanUtil.copyProperties(dto, exist);
        exist.setSort(dto.getSort() == null ? 0 : dto.getSort());
        exist.setStatus(normalize01(dto.getStatus() == null ? 1 : dto.getStatus(),
                "上架状态"));
        exist.setIsRecommend(normalize01(dto.getIsRecommend() == null ? 0
                : dto.getIsRecommend(), "推荐标记"));
        getBaseMapper().updateById(exist);
    }

    @Override
    public void remove(Long id) {
        mustGet(id);
        getBaseMapper().deleteById(id);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        Dish dish = mustGet(id);
        dish.setStatus(normalize01(status, "状态"));
        getBaseMapper().updateById(dish);
    }

    @Override
    public void toggleRecommend(Long id, Integer isRecommend) {
        Dish dish = mustGet(id);
        dish.setIsRecommend(normalize01(isRecommend, "推荐标记"));
        getBaseMapper().updateById(dish);
    }

    @Override
    public void batchStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少选择一个菜品");
        }
        int normalized = normalize01(status, "状态");
        getBaseMapper().update(null, new LambdaUpdateWrapper<Dish>()
                .in(Dish::getId, ids)
                .set(Dish::getStatus, normalized));
    }

    private DishVO toVO(Dish dish, Long userId) {
        DishVO vo = new DishVO();
        BeanUtil.copyProperties(dish, vo);
        List<String> tagList = StrUtil.split(dish.getTags(), ',').stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
        vo.setTagList(tagList);
        if (userId != null) {
            Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getDishId, dish.getId()));
            vo.setFavorite(count > 0);
        } else {
            vo.setFavorite(false);
        }
        return vo;
    }

    private void checkCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所选分类不存在");
        }
    }

    private Dish mustGet(Long id) {
        Dish dish = getBaseMapper().selectById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        return dish;
    }

    private int normalize01(Integer value, String fieldName) {
        if (value == null || (value != 0 && value != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    fieldName + "只能为 0 或 1");
        }
        return value;
    }

    private long pn(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long ps(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
