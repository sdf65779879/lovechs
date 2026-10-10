package com.couple.ordering.interceptor;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录接口限流拦截器。
 *
 * <p>固定窗口：同一 IP 每分钟最多 10 次登录请求，超限抛出 429。
 * 规模极小（≤5 人），使用内存计数即可，无需 Redis；容器重启计数清空可接受。</p>
 */
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    /** 每个窗口最大请求数 */
    private static final int MAX_PER_WINDOW = 10;

    /** 窗口时长（毫秒） */
    private static final long WINDOW_MS = 60_000L;

    /** 触发清理的表大小阈值 */
    private static final int CLEANUP_THRESHOLD = 200;

    /**
     * 计数窗口。
     */
    private static final class Window {
        private long startMs;
        private int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String clientIp = JakartaServletUtil.getClientIP(request);
        Window window = windows.computeIfAbsent(clientIp, key -> newWindow());
        synchronized (window) {
            long now = System.currentTimeMillis();
            // 窗口已过期则重置
            if (now - window.startMs >= WINDOW_MS) {
                window.startMs = now;
                window.count = 0;
            }
            if (window.count >= MAX_PER_WINDOW) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
            }
            window.count++;
        }
        cleanupIfNeeded();
        return true;
    }

    private Window newWindow() {
        Window window = new Window();
        window.startMs = System.currentTimeMillis();
        return window;
    }

    /**
     * 表过大时惰性清理过期窗口，避免长期运行后内存增长。
     */
    private void cleanupIfNeeded() {
        if (windows.size() <= CLEANUP_THRESHOLD) {
            return;
        }
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startMs >= WINDOW_MS);
    }
}
