package com.couple.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.couple.ordering.entity.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单明细 Mapper，含热门菜品聚合。
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 按累计点单数量聚合最受欢迎的菜品。
     *
     * @param limit 返回条数
     * @return 每行包含 dishId、dishName、cnt（累计份数）
     */
    @Select("""
            SELECT oi.dish_id AS dishId, oi.dish_name AS dishName,
                   SUM(oi.quantity) AS cnt
            FROM order_item oi
            INNER JOIN orders o ON oi.order_id = o.id
            WHERE o.deleted = 0
              AND o.status NOT IN ('CANCELLED', 'REJECTED')
            GROUP BY oi.dish_id, oi.dish_name
            ORDER BY cnt DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topDishes(@Param("limit") int limit);
}
