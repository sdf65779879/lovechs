package com.couple.ordering.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.ordering.entity.Orders;
import com.couple.ordering.mapper.OrderItemMapper;
import com.couple.ordering.mapper.OrdersMapper;
import com.couple.ordering.service.DashboardService;
import com.couple.ordering.vo.DashboardVO;
import com.couple.ordering.vo.TopDishVO;
import com.couple.ordering.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据看板服务实现：指标卡、热门菜 Top5、7 天趋势。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public DashboardVO build() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();

        DashboardVO vo = new DashboardVO();
        // 今日有效订单（排除取消/拒绝）
        vo.setTodayOrderCount(intCount(new LambdaQueryWrapper<Orders>()
                .ge(Orders::getCreateTime, dayStart)
                .lt(Orders::getCreateTime, tomorrow)
                .notIn(Orders::getStatus, "CANCELLED", "REJECTED")));
        // 待接单
        vo.setPendingCount(intCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, "PENDING")));
        // 累计订单（含全部状态）
        vo.setTotalCount(intCount(new LambdaQueryWrapper<>()));
        // 今日完成
        vo.setTodayCompletedCount(intCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, "COMPLETED")
                .ge(Orders::getFinishTime, dayStart)
                .lt(Orders::getFinishTime, tomorrow)));

        // 热门菜 Top5
        List<TopDishVO> topDishes = orderItemMapper.topDishes(5).stream()
                .map(row -> {
                    TopDishVO top = new TopDishVO();
                    top.setDishId(((Number) row.get("dishId")).longValue());
                    top.setDishName((String) row.get("dishName"));
                    top.setCount(((Number) row.get("cnt")).intValue());
                    return top;
                }).toList();
        vo.setTopDishes(topDishes);

        // 7 天趋势，缺失日期补 0
        LocalDateTime trendStart = today.minusDays(6).atStartOfDay();
        Map<String, Integer> dailyCounts = ordersMapper.countDaily(trendStart).stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("day"),
                        row -> ((Number) row.get("cnt")).intValue()));
        List<TrendVO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            TrendVO item = new TrendVO();
            item.setDate(date);
            item.setCount(dailyCounts.getOrDefault(date, 0));
            trend.add(item);
        }
        vo.setTrend(trend);
        return vo;
    }

    private int intCount(LambdaQueryWrapper<Orders> wrapper) {
        return Math.toIntExact(ordersMapper.selectCount(wrapper));
    }
}
