package com.couple.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.couple.ordering.entity.Orders;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper，含看板趋势统计。
 */
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 统计起始时间之后每日有效订单数量（排除取消/拒绝）。
     *
     * @param startTime 统计起点
     * @return 每行包含 day（yyyy-MM-dd）、cnt（订单数）
     */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, COUNT(*) AS cnt
            FROM orders
            WHERE deleted = 0
              AND status NOT IN ('CANCELLED', 'REJECTED')
              AND create_time >= #{startTime}
            GROUP BY day
            ORDER BY day
            """)
    List<Map<String, Object>> countDaily(@Param("startTime") LocalDateTime startTime);
}
