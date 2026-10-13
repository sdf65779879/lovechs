package com.couple.ordering.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.common.utils.IdempotencyStore;
import com.couple.ordering.dto.OrderStatusDTO;
import com.couple.ordering.dto.OrderSubmitDTO;
import com.couple.ordering.entity.CartItem;
import com.couple.ordering.entity.Dish;
import com.couple.ordering.entity.OrderItem;
import com.couple.ordering.entity.Orders;
import com.couple.ordering.entity.SysUser;
import com.couple.ordering.mapper.CartItemMapper;
import com.couple.ordering.mapper.DishMapper;
import com.couple.ordering.mapper.OrderItemMapper;
import com.couple.ordering.mapper.OrdersMapper;
import com.couple.ordering.mapper.SysUserMapper;
import com.couple.ordering.service.OrderService;
import com.couple.ordering.service.SysConfigService;
import com.couple.ordering.notification.OrderEmailNotificationService;
import com.couple.ordering.vo.OrderCalendarVO;
import com.couple.ordering.vo.OrderItemVO;
import com.couple.ordering.vo.OrderVO;
import com.couple.ordering.vo.RebuyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现：下单快照、幂等、状态机、日历、管理端流转。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;
    private final SysUserMapper sysUserMapper;
    private final SysConfigService sysConfigService;
    private final IdempotencyStore idempotencyStore;
    private final OrderEmailNotificationService emailNotifications;

    private static final List<String> ACTIVE_STATUSES =
            List.of("PENDING", "ACCEPTED", "COOKING");
    private static final List<String> FINISHED_STATUSES =
            List.of("COMPLETED", "CANCELLED", "REJECTED");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submit(OrderSubmitDTO dto, String idempotencyKey) {
        Long userId = SecurityUtils.getUserId();
        // 幂等：短时间内重复提交直接返回已创建订单，避免重复下单
        Long existingId = idempotencyStore.getOrderId(idempotencyKey);
        if (existingId != null) {
            return buildOrderVO(ordersMapper.selectById(existingId), true, false);
        }
        if (!sysConfigService.isBusinessOpen()) {
            throw new BusinessException(ResultCode.CLOSED);
        }
        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByAsc(CartItem::getId));
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车是空的，先挑点好吃的吧");
        }
        // 校验菜品并构建订单明细快照
        List<OrderItem> snapshots = new ArrayList<>();
        int totalLove = 0;
        for (CartItem cartItem : cartItems) {
            Dish dish = dishMapper.selectById(cartItem.getDishId());
            if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
                throw new BusinessException(
                        "购物车中有菜品已下架，请返回购物车调整后再下单");
            }
            OrderItem snapshot = new OrderItem();
            snapshot.setDishId(dish.getId());
            snapshot.setDishName(dish.getName());
            snapshot.setDishImage(dish.getImage());
            snapshot.setLoveValue(dish.getLoveValue());
            snapshot.setQuantity(cartItem.getQuantity());
            snapshot.setTaste(StrUtil.nullToEmpty(cartItem.getTaste()));
            snapshot.setRemark(StrUtil.nullToEmpty(cartItem.getRemark()));
            snapshots.add(snapshot);
            totalLove += dish.getLoveValue() * cartItem.getQuantity();
        }
        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setTotalLove(totalLove);
        order.setRemark(StrUtil.nullToEmpty(dto.getRemark()));
        order.setExpectTime(parseExpectTime(dto.getExpectTime()));
        order.setRejectReason("");
        ordersMapper.insert(order);
        for (OrderItem snapshot : snapshots) {
            snapshot.setOrderId(order.getId());
            orderItemMapper.insert(snapshot);
        }
        // 下单成功后清空购物车
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        emailNotifications.orderSubmitted(order.getId());
        idempotencyStore.put(idempotencyKey, order.getId());
        return buildOrderVO(order, true, false);
    }

    @Override
    public PageResult<OrderVO> myPage(String tab, Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtils.getUserId();
        List<String> statuses = "finished".equals(tab)
                ? FINISHED_STATUSES : ACTIVE_STATUSES;
        Page<Orders> page = new Page<>(pn(pageNum), ps(pageSize));
        Page<Orders> result = ordersMapper.selectPage(page,
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .in(Orders::getStatus, statuses)
                        .orderByDesc(Orders::getCreateTime));
        // 列表携带明细：订单卡片需展示菜品缩略图与名称（双人小站数据量极小）
        List<OrderVO> list = result.getRecords().stream()
                .map(order -> buildOrderVO(order, true, false)).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public OrderVO detail(Long id) {
        return buildOrderVO(mustOwn(id), true, false);
    }

    @Override
    public void cancel(Long id) {
        Orders order = mustOwn(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "只有待接单的订单才能取消");
        }
        order.setStatus("CANCELLED");
        ordersMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RebuyVO rebuy(Long id) {
        Orders order = mustOwn(id);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : items) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
                skipped.add(item.getDishName());
                continue;
            }
            String taste = StrUtil.nullToEmpty(item.getTaste());
            String remark = StrUtil.nullToEmpty(item.getRemark());
            CartItem same = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, order.getUserId())
                    .eq(CartItem::getDishId, dish.getId())
                    .eq(CartItem::getTaste, taste)
                    .eq(CartItem::getRemark, remark));
            if (same != null) {
                same.setQuantity(same.getQuantity() + item.getQuantity());
                cartItemMapper.updateById(same);
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setUserId(order.getUserId());
                cartItem.setDishId(dish.getId());
                cartItem.setQuantity(item.getQuantity());
                cartItem.setTaste(taste);
                cartItem.setRemark(remark);
                cartItemMapper.insert(cartItem);
            }
            added++;
        }
        RebuyVO vo = new RebuyVO();
        vo.setAddedCount(added);
        vo.setSkippedNames(skipped);
        return vo;
    }

    @Override
    public OrderCalendarVO calendar(String month) {
        Long userId = SecurityUtils.getUserId();
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "月份格式应为 yyyy-MM");
        }
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .ge(Orders::getCreateTime, start)
                .lt(Orders::getCreateTime, end)
                .notIn(Orders::getStatus, "CANCELLED", "REJECTED")
                .orderByAsc(Orders::getCreateTime));
        Map<String, List<Orders>> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().toLocalDate().toString()));
        List<OrderCalendarVO.CalendarDay> days = new ArrayList<>();
        for (Map.Entry<String, List<Orders>> entry : grouped.entrySet()) {
            OrderCalendarVO.CalendarDay day = new OrderCalendarVO.CalendarDay();
            day.setDate(entry.getKey());
            day.setOrderCount(entry.getValue().size());
            day.setTotalLove(entry.getValue().stream()
                    .mapToInt(Orders::getTotalLove).sum());
            day.setOrders(entry.getValue().stream()
                    .map(this::toCalendarOrder).toList());
            days.add(day);
        }
        OrderCalendarVO vo = new OrderCalendarVO();
        vo.setMonth(month);
        vo.setDays(days);
        return vo;
    }

    @Override
    public PageResult<OrderVO> adminPage(String status, String date,
                                         Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            // 先校验状态合法性
            wrapper.eq(Orders::getStatus, OrderStatusSafe.from(status));
        }
        if (StrUtil.isNotBlank(date)) {
            LocalDateTime dayStart;
            try {
                dayStart = java.time.LocalDate.parse(date,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "日期格式应为 yyyy-MM-dd");
            }
            wrapper.ge(Orders::getCreateTime, dayStart)
                    .lt(Orders::getCreateTime, dayStart.plusDays(1));
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        Page<Orders> page = new Page<>(pn(pageNum), ps(pageSize));
        Page<Orders> result = ordersMapper.selectPage(page, wrapper);
        // 列表携带明细，便于管理端快速核对菜品
        List<OrderVO> list = result.getRecords().stream()
                .map(order -> buildOrderVO(order, true, true)).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public OrderVO adminDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return buildOrderVO(order, true, true);
    }

    @Override
    public OrderVO changeStatus(Long id, OrderStatusDTO dto) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        com.couple.ordering.enums.OrderStatus current =
                com.couple.ordering.enums.OrderStatus.from(order.getStatus());
        com.couple.ordering.enums.OrderStatus target =
                com.couple.ordering.enums.OrderStatus.from(dto.getStatus());
        if (target == com.couple.ordering.enums.OrderStatus.CANCELLED) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "管理员不可取消订单");
        }
        if (!current.canTransitTo(target)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        if (target == com.couple.ordering.enums.OrderStatus.REJECTED
                && StrUtil.isBlank(dto.getRejectReason())) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "拒绝订单请填写理由");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(target.name());
        if (target == com.couple.ordering.enums.OrderStatus.ACCEPTED) {
            order.setAcceptTime(now);
        }
        if (target == com.couple.ordering.enums.OrderStatus.COMPLETED) {
            order.setFinishTime(now);
        }
        if (target == com.couple.ordering.enums.OrderStatus.REJECTED) {
            order.setRejectReason(dto.getRejectReason().trim());
        }
        ordersMapper.updateById(order);
        return buildOrderVO(order, true, true);
    }

    // ========================= 私有辅助 =========================

    private Orders mustOwn(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return order;
    }

    private OrderVO buildOrderVO(Orders order, boolean withItems,
                                 boolean withNickname) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo);
        vo.setStatusLabel(com.couple.ordering.enums.OrderStatus
                .from(order.getStatus()).getLabel());
        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));
            vo.setItems(items.stream().map(this::toItemVO).toList());
        }
        if (withNickname) {
            SysUser user = sysUserMapper.selectById(order.getUserId());
            vo.setNickname(user == null ? "未知用户" : user.getNickname());
        }
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtil.copyProperties(item, vo);
        vo.setLineLove(item.getLoveValue() * item.getQuantity());
        return vo;
    }

    private OrderCalendarVO.CalendarOrder toCalendarOrder(Orders order) {
        OrderCalendarVO.CalendarOrder co =
                new OrderCalendarVO.CalendarOrder();
        co.setId(order.getId());
        co.setOrderNo(order.getOrderNo());
        co.setStatus(order.getStatus());
        co.setStatusLabel(com.couple.ordering.enums.OrderStatus
                .from(order.getStatus()).getLabel());
        co.setTotalLove(order.getTotalLove());
        return co;
    }

    /**
     * 生成订单号：yyyyMMddHHmmss + 4 位随机数，冲突时重试。
     */
    private String generateOrderNo() {
        String prefix = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        for (int i = 0; i < 5; i++) {
            String candidate = prefix + RandomUtil.randomNumbers(4);
            Long count = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getOrderNo, candidate));
            if (count == 0) {
                return candidate;
            }
        }
        return prefix + RandomUtil.randomNumbers(6);
    }

    private LocalDateTime parseExpectTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "期望用餐时间格式不正确");
        }
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

    /**
     * 内部别名，避免与本类中频繁使用的全限定名冲突，同时复用枚举校验。
     */
    private static final class OrderStatusSafe {
        static String from(String code) {
            return com.couple.ordering.enums.OrderStatus.from(code).name();
        }
    }
}
