package com.couple.ordering.enums;

import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import lombok.Getter;

/**
 * 用户角色枚举。
 */
@Getter
public enum UserRole {

    ADMIN("管理员"),
    USER("普通用户");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    /**
     * 安全地从字符串解析角色，非法值抛出业务异常。
     */
    public static UserRole from(String code) {
        try {
            return UserRole.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未知角色：" + code);
        }
    }
}
