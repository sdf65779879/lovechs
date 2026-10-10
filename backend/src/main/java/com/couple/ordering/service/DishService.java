package com.couple.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.dto.DishDTO;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.vo.DishVO;

import java.util.List;

/**
 * 菜品服务。
 */
public interface DishService extends IService<Dish> {

    /**
     * 用户端分页查询上架菜品。
     */
    PageResult<DishVO> userPage(Long categoryId, String keyword,
                                Integer pageNum, Integer pageSize);

    /**
     * 推荐菜品。
     */
    List<DishVO> recommend(Integer limit);

    /**
     * 菜品详情。
     */
    DishVO detail(Long id);

    /**
     * 管理端分页查询。
     */
    PageResult<DishVO> adminPage(Long categoryId, Integer status, String keyword,
                                 Integer pageNum, Integer pageSize);

    /**
     * 新增菜品，返回 id。
     */
    Long create(DishDTO dto);

    /**
     * 修改菜品。
     */
    void update(Long id, DishDTO dto);

    /**
     * 删除菜品（逻辑删除）。
     */
    void remove(Long id);

    /**
     * 上架/下架。
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 设置/取消推荐。
     */
    void toggleRecommend(Long id, Integer isRecommend);

    /**
     * 批量上架/下架。
     */
    void batchStatus(List<Long> ids, Integer status);
}
