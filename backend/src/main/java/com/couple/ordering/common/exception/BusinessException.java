package com.couple.ordering.common.exception;

import com.couple.ordering.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。所有可预期的业务错误均抛出本异常，
 * 由 {@link GlobalExceptionHandler} 统一转换为友好响应，不向前端暴露堆栈。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
