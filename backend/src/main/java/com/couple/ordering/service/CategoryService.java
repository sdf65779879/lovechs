package com.couple.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.dto.CategoryDTO;
import com.couple.ordering.entity.Category;
import com.couple.ordering.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务。
 */
public interface CategoryService extends IService<Category> {

    /**
     * 用户端：查询所有启用分类。
     */
    List<CategoryVO> listEnabled();

    /**
     * 管理端分页列表（含菜品数量）。
     */
    PageResult<CategoryVO> adminPage(Integer pageNum, Integer pageSize);

    /**
     * 管理端全部分类（下拉选择用）。
     */
    List<CategoryVO> adminListAll();

    /**
     * 新增分类，返回 id。
     */
    Long create(CategoryDTO dto);

    /**
     * 修改分类。
     */
    void update(Long id, CategoryDTO dto);

    /**
     * 删除分类（存在菜品时拒绝）。
     */
    void remove(Long id);

    /**
     * 启用/禁用分类。
     */
    void toggleStatus(Long id, Integer status);
}
