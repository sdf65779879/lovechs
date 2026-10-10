package com.couple.ordering.service;

import com.couple.ordering.dto.CartDTO;
import com.couple.ordering.dto.CartUpdateDTO;
import com.couple.ordering.vo.CartVO;

import java.util.List;

/**
 * 购物车服务。
 */
public interface CartService {

    /**
     * 查询我的购物车。
     */
    List<CartVO> listMyCart();

    /**
     * 加入购物车（同菜品同口味自动合并）。
     */
    void add(CartDTO dto);

    /**
     * 修改购物车条目数量。
     */
    void update(Long cartId, CartUpdateDTO dto);

    /**
     * 删除购物车条目。
     */
    void remove(Long cartId);

    /**
     * 清空我的购物车。
     */
    void clear();
}
