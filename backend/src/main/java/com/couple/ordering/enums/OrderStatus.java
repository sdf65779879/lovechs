package com.couple.ordering.enums;

import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举及状态机流转规则。
 */
@Getter
public enum OrderStatus {

    PENDING("待接单"),
    ACCEPTED("已接单"),
    COOKING("制作中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已拒绝");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /**
     * 合法流转关系：
     *
     * <pre>
     * PENDING  → ACCEPTED / CANCELLED / REJECTED
     * ACCEPTED → COOKING
     * COOKING  → COMPLETED
     * 其余均为非法流转
     * </pre>
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ACCEPTED, CANCELLED, REJECTED),
            ACCEPTED, EnumSet.of(COOKING),
            COOKING, EnumSet.of(COMPLETED)
    );

    /**
     * 判断当前状态是否可以流转到目标状态。
     */
    public boolean canTransitTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * 安全地从字符串解析状态，非法值抛出业务异常。
     */
    public static OrderStatus from(String code) {
        try {
            return OrderStatus.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "未知订单状态：" + code);
        }
    }

    /**
     * 是否为进行中状态。
     */
    public boolean isActive() {
        return this == PENDING || this == ACCEPTED || this == COOKING;
    }

    /**
     * 是否为终态。
     */
    public boolean isTerminal() {
        return !isActive();
    }
}
