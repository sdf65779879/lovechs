package com.couple.ordering.notification;

import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.dto.EmailSettingsDTO;
import com.couple.ordering.service.SysConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 唯一发件配置来源仍是 sys_config；通知渠道只存收件人、事件与开关。 */
@Service
@RequiredArgsConstructor
public class EmailSettingsService {
    public static final String SMTP_KEY = "email_system";
    public static final String CHANNEL_KEY = "email_order_channel";
    private final SysConfigService configs;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final EmailSecrets secrets;
    @Value("${couple.email.provider:SMTP}")
    private String provider;

    public record StoredSmtp(String host, int port, String encryption, String fromAddress,
                             String username, String encryptedPassword) {}
    public record Channel(boolean enabled, String recipients, String event) {}
    public record SmtpView(String host, int port, String encryption, String fromAddress,
                           String username, boolean passwordConfigured) {}
    public record SettingsView(SmtpView smtp, Channel channel, String provider) {}

    public boolean isMock() {
        if (!Set.of("SMTP", "MOCK").contains(provider)) {
            throw new BusinessException("EMAIL_PROVIDER 必须为 SMTP 或 MOCK");
        }
        return "MOCK".equals(provider);
    }

    public StoredSmtp smtp() {
        return read(SMTP_KEY, StoredSmtp.class, new StoredSmtp("", 465, "TLS", "", "", ""));
    }

    public Channel channel() {
        return read(CHANNEL_KEY, Channel.class, new Channel(false, "", "NEW_ORDER"));
    }

    public SettingsView view() {
        StoredSmtp smtp = smtp();
        return new SettingsView(new SmtpView(smtp.host(), smtp.port(), smtp.encryption(),
                smtp.fromAddress(), smtp.username(), !smtp.encryptedPassword().isBlank()), channel(),
                isMock() ? "MOCK" : "SMTP");
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(EmailSettingsDTO dto) {
        var input = dto.getSmtp();
        String host = input.getHost().trim();
        if (!host.isEmpty() && !host.matches("[a-zA-Z0-9][a-zA-Z0-9.-]*")) {
            throw new BusinessException("smtp.host：请填写主机名，不包含协议、路径或空格");
        }
        String from = input.getFromAddress().trim();
        if (!from.isEmpty()) address(from, "smtp.fromAddress");
        String username = input.getUsername().trim();
        if (hasControls(username)) throw new BusinessException("smtp.username：不能包含控制字符");
        List<String> recipients = recipients(dto.getChannel().getRecipients());
        String password = smtp().encryptedPassword();
        if (input.isClearPassword()) password = "";
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            if (input.isClearPassword()) throw new BusinessException("请勿同时清除和填写授权码");
            if (isMock()) throw new BusinessException("Mock 模式无需填写真实授权码");
            password = secrets.encrypt(input.getPassword());
        }
        StoredSmtp smtp = new StoredSmtp(host, input.getPort(), input.getEncryption(), from, username, password);
        Channel channel = new Channel(dto.getChannel().isEnabled(), String.join(",", recipients), "NEW_ORDER");
        if (channel.enabled()) validateReady(smtp, channel);
        write(SMTP_KEY, smtp);
        write(CHANNEL_KEY, channel);
    }

    public void validateReady(StoredSmtp smtp, Channel channel) {
        if (recipients(channel.recipients()).isEmpty()) throw new BusinessException("channel.recipients：请填写大厨收件邮箱");
        if (!"NEW_ORDER".equals(channel.event())) throw new BusinessException("仅支持新订单通知");
        if (isMock()) return; // Mock 不读取或解密真实密码，也不连接 SMTP。
        if (smtp.host().isBlank() || smtp.fromAddress().isBlank()) throw new BusinessException("请先配置 SMTP 主机和发件邮箱");
        address(smtp.fromAddress(), "smtp.fromAddress");
        if (!smtp.username().isBlank() && smtp.encryptedPassword().isBlank()) {
            throw new BusinessException("请填写系统邮箱授权码");
        }
    }

    public List<String> recipients(String text) {
        if (text == null || text.isBlank()) return List.of();
        if (hasControls(text)) throw new BusinessException("channel.recipients：不能包含换行或控制字符");
        Map<String, String> distinct = new LinkedHashMap<>();
        for (String part : text.split("[,;，；]", -1)) {
            String value = part.trim();
            if (value.isEmpty()) continue;
            address(value, "channel.recipients");
            distinct.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
        }
        if (distinct.size() > 5) throw new BusinessException("channel.recipients：最多填写 5 个邮箱");
        return List.copyOf(distinct.values());
    }

    private void address(String value, String field) {
        try {
            if (value.length() > 254 || hasControls(value) || !value.contains("@")) throw new IllegalArgumentException();
            InternetAddress parsed = new InternetAddress(value, true);
            parsed.validate();
            if (parsed.getPersonal() != null || !value.equals(parsed.getAddress())) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new BusinessException(field + "：邮箱格式不正确，请填写纯邮箱地址");
        }
    }

    private boolean hasControls(String text) {
        return text.codePoints().anyMatch(Character::isISOControl);
    }

    private <T> T read(String key, Class<T> type, T fallback) {
        String value = configs.getConfigValue(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException("邮件设置无法读取，请检查或重新配置系统邮箱");
        }
    }

    private void write(String key, Object value) {
        try {
            jdbc.update("INSERT INTO sys_config(config_key,config_value,remark,update_time) VALUES (?,?,?,CURRENT_TIMESTAMP) "
                            + "ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),update_time=CURRENT_TIMESTAMP",
                    key, json.writeValueAsString(value), "系统邮件通知配置");
        } catch (JsonProcessingException e) {
            throw new BusinessException("邮件设置保存失败");
        }
    }
}
