package com.couple.ordering.service;

import com.couple.ordering.vo.FavoriteVO;

import java.util.List;

/**
 * 菜品收藏服务。
 */
public interface FavoriteService {

    /**
     * 我的收藏列表。
     */
    List<FavoriteVO> list();

    /**
     * 收藏菜品。
     */
    void add(Long dishId);

    /**
     * 取消收藏。
     */
    void remove(Long dishId);
}
