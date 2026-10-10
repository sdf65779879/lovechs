package com.couple.ordering.common.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下单幂等存储。
 *
 * <p>前端提交订单时携带 {@code Idempotency-Key}（UUID），
 * 后端在 TTL（10 分钟）内记录 key → orderId；重复提交直接返回已创建的订单，
 * 避免网络重试/连点产生重复订单。内存实现，低并发场景足够。</p>
 */
@Component
public class IdempotencyStore {

    /** 幂等记录存活时长：10 分钟 */
    private static final long TTL_MS = 10 * 60_000L;

    /**
     * 幂等条目。
     *
     * @param orderId  已创建的订单 id
     * @param expireAt 过期时间戳
     */
    private record Entry(Long orderId, long expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * 查询幂等键对应的已创建订单 id；不存在或已过期返回 null。
     */
    public Long getOrderId(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            store.remove(key);
            return null;
        }
        return entry.orderId();
    }

    /**
     * 记录幂等键与订单 id。
     */
    public void put(String key, Long orderId) {
        if (key == null || key.isBlank()) {
            return;
        }
        store.put(key, new Entry(orderId, System.currentTimeMillis() + TTL_MS));
        cleanupExpired();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now > entry.getValue().expireAt());
    }
}
