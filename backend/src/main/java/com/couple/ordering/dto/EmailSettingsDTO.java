package com.couple.ordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 密码仅作为更新输入，不使用自动生成的 toString，避免意外记录密钥。 */
@Getter
@Setter
public class EmailSettingsDTO {
    @Valid
    @NotNull(message = "请填写系统邮箱设置")
    private Smtp smtp;
    @Valid
    @NotNull(message = "请填写新订单通知设置")
    private Channel channel;

    @Getter
    @Setter
    public static class Smtp {
        @NotNull
        @Size(max = 253, message = "SMTP 主机过长")
        private String host = "";
        @NotNull
        @Min(value = 1, message = "端口应为 1–65535")
        @Max(value = 65535, message = "端口应为 1–65535")
        private Integer port = 465;
        @NotNull
        @Pattern(regexp = "TLS|STARTTLS|NONE", message = "请选择 TLS、STARTTLS 或 NONE")
        private String encryption = "TLS";
        @NotNull
        @Size(max = 254, message = "发件地址过长")
        private String fromAddress = "";
        @NotNull
        @Size(max = 254, message = "用户名过长")
        private String username = "";
        @Size(max = 512, message = "授权码过长")
        private String password;
        private boolean clearPassword;
    }

    @Getter
    @Setter
    public static class Channel {
        private boolean enabled;
        @NotNull
        @Size(max = 1500, message = "收件地址过长，最多填写 5 个邮箱")
        private String recipients = "";
        @NotNull
        @Pattern(regexp = "NEW_ORDER", message = "仅支持新订单通知")
        private String event = "NEW_ORDER";
    }
}
