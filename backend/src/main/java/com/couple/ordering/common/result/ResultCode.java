package com.couple.ordering.common.result;

import lombok.Getter;

/**
 * 统一业务返回码枚举。
 *
 * <p>约定：成功为 0；401/403/404/429 与 HTTP 语义对齐；
 * 1000 段为业务错误；500 为未知系统错误。</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    BUSINESS_ERROR(1000, "操作失败"),
    LOGIN_FAILED(1001, "账号或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被禁用"),
    ORDER_STATUS_ERROR(1003, "当前订单状态不允许该操作"),
    REPEAT_SUBMIT(1004, "请勿重复提交"),
    UPLOAD_ERROR(1005, "文件上传失败"),
    CLOSED(1006, "今日暂不营业"),

    ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
