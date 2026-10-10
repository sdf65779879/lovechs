package com.couple.ordering.common.constant;

/**
 * 系统通用常量。
 */
public final class Constants {

    private Constants() {
    }

    /** 请求头中 Token 的前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 认证请求头名称 */
    public static final String TOKEN_HEADER = "Authorization";

    /** 管理员角色标识 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 普通用户角色标识 */
    public static final String ROLE_USER = "USER";

    /** 幂等键请求头名称 */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    /** 系统配置键：营业开关 */
    public static final String CONFIG_BUSINESS_OPEN = "business_open";

    /** 系统配置键：首页公告 */
    public static final String CONFIG_NOTICE = "notice";
}
