package com.couple.ordering.notification;

import lombok.Getter;

/** 仅保留安全、可操作的提示，不附带厂商响应或密钥。 */
@Getter
public class EmailDeliveryException extends RuntimeException {
    private final boolean retryable;
    public EmailDeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }
}
