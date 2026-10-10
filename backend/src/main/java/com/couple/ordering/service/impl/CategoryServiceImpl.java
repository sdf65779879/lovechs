package com.couple.ordering.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.dto.CategoryDTO;
import com.couple.ordering.entity.Category;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.mapper.CategoryMapper;
import com.couple.ordering.mapper.DishMapper;
import com.couple.ordering.service.CategoryService;
import com.couple.ordering.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    private final DishMapper dishMapper;

    @Override
    public List<CategoryVO> listEnabled() {
        List<Category> categories = getBaseMapper().selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        return categories.stream().map(category -> toVO(category, false)).toList();
    }

    @Override
    public PageResult<CategoryVO> adminPage(Integer pageNum, Integer pageSize) {
        Page<Category> page = new Page<>(pn(pageNum), ps(pageSize));
        Page<Category> result = getBaseMapper().selectPage(page,
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        List<CategoryVO> list = result.getRecords().stream()
                .map(category -> toVO(category, true)).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public List<CategoryVO> adminListAll() {
        return getBaseMapper().selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort).orderByAsc(Category::getId)).stream()
                .map(category -> toVO(category, false)).toList();
    }

    @Override
    public Long create(CategoryDTO dto) {
        Long duplicate = getBaseMapper().selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, dto.getName()));
        if (duplicate > 0) {
            throw new BusinessException("分类名称已存在");
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : normalizeStatus(dto.getStatus()));
        getBaseMapper().insert(category);
        return category.getId();
    }

    @Override
    public void update(Long id, CategoryDTO dto) {
        Category category = mustGet(id);
        if (!category.getName().equals(dto.getName())) {
            Long duplicate = getBaseMapper().selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getName, dto.getName()));
            if (duplicate > 0) {
                throw new BusinessException("分类名称已存在");
            }
        }
        category.setName(dto.getName());
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            category.setStatus(normalizeStatus(dto.getStatus()));
        }
        getBaseMapper().updateById(category);
    }

    @Override
    public void remove(Long id) {
        mustGet(id);
        Long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException(
                    "该分类下还有 " + count + " 个菜品，请先迁移或删除菜品");
        }
        getBaseMapper().deleteById(id);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        Category category = mustGet(id);
        category.setStatus(normalizeStatus(status));
        getBaseMapper().updateById(category);
    }

    private CategoryVO toVO(Category category, boolean withDishCount) {
        CategoryVO vo = new CategoryVO();
        BeanUtil.copyProperties(category, vo);
        if (withDishCount) {
            Long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                    .eq(Dish::getCategoryId, category.getId()));
            vo.setDishCount(count.intValue());
        }
        return vo;
    }

    private Category mustGet(Long id) {
        Category category = getBaseMapper().selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private int normalizeStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态只能为 0 或 1");
        }
        return status;
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
