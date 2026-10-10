package com.couple.ordering.service;

import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.dto.OrderStatusDTO;
import com.couple.ordering.dto.OrderSubmitDTO;
import com.couple.ordering.vo.OrderCalendarVO;
import com.couple.ordering.vo.OrderVO;
import com.couple.ordering.vo.RebuyVO;

/**
 * 订单服务：用户端下单/查询与管理端流转。
 */
public interface OrderService {

    /**
     * 提交订单（幂等）。
     *
     * @param dto            订单入参
     * @param idempotencyKey 幂等键（可为空）
     */
    OrderVO submit(OrderSubmitDTO dto, String idempotencyKey);

    /**
     * 我的订单分页。
     *
     * @param tab active 进行中 / finished 已完成
     */
    PageResult<OrderVO> myPage(String tab, Integer pageNum, Integer pageSize);

    /**
     * 查询我的订单详情。
     */
    OrderVO detail(Long id);

    /**
     * 取消订单（仅待接单）。
     */
    void cancel(Long id);

    /**
     * 再次下单：历史菜品加回购物车。
     */
    RebuyVO rebuy(Long id);

    /**
     * 吃饭日历（按月）。
     */
    OrderCalendarVO calendar(String month);

    /**
     * 管理端订单分页。
     */
    PageResult<OrderVO> adminPage(String status, String date,
                                  Integer pageNum, Integer pageSize);

    /**
     * 管理端订单详情。
     */
    OrderVO adminDetail(Long id);

    /**
     * 管理端更新订单状态。
     */
    OrderVO changeStatus(Long id, OrderStatusDTO dto);
}
