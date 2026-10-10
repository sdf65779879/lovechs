package com.couple.ordering.common.constant;

/**
 * Spring Security 相关常量：白名单路径等。
 *
 * <p>注意：路径不包含 server.servlet.context-path（/api），
 * 因为 Spring Security 的匹配器基于 context 之内的路径。</p>
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** 无需认证即可访问的路径 */
    public static final String[] WHITE_PATHS = {
            // 登录、健康检查
            "/v1/auth/login",
            "/v1/health",
            // Knife4j / SpringDoc 接口文档
            "/doc.html",
            "/doc.html/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/favicon.ico",
            // Spring Boot 默认错误分发
            "/error"
    };

    /** 管理端路径前缀，要求 ADMIN 角色 */
    public static final String ADMIN_PATH_PATTERN = "/v1/admin/**";
}
