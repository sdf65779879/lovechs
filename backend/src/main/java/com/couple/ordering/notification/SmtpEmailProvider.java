package com.couple.ordering.notification;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class SmtpEmailProvider implements EmailProvider {
    private final EmailSecrets secrets;

    JavaMailSenderImpl sender(EmailSettingsService.StoredSmtp settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setDefaultEncoding("UTF-8");
        boolean auth = !settings.username().isBlank();
        if (auth) {
            sender.setUsername(settings.username());
            sender.setPassword(secrets.decrypt(settings.encryptedPassword()));
        }
        Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(auth));
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString("TLS".equals(settings.encryption())));
        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString("STARTTLS".equals(settings.encryption())));
        properties.setProperty("mail.smtp.starttls.required", Boolean.toString("STARTTLS".equals(settings.encryption())));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
        properties.setProperty("mail.smtp.sendpartial", "false");
        properties.setProperty("mail.debug", "false");
        return sender;
    }

    @Override
    public void send(EmailSettingsService.StoredSmtp settings, List<String> recipients, Message message) {
        try {
            JavaMailSenderImpl sender = sender(settings);
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(settings.fromAddress());
            helper.setBcc(recipients.toArray(String[]::new));
            helper.setSubject(message.subject());
            helper.setText(message.body(), false);
            sender.send(mime);
        } catch (MailException | MessagingException e) {
            throw safeFailure(e);
        }
    }

    static EmailDeliveryException safeFailure(Throwable error) {
        List<Throwable> errors = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(error, errors, visited);
        if (errors.stream().anyMatch(e -> e instanceof MailAuthenticationException || e instanceof AuthenticationFailedException)) {
            return new EmailDeliveryException("邮箱认证失败，请检查用户名、授权码及 SMTP 服务开关", false);
        }
        if (errors.stream().anyMatch(e -> e instanceof SSLException)) {
            return new EmailDeliveryException("邮件安全连接失败，请检查加密方式、端口及服务器证书", false);
        }
        // SMTP 明确返回的 4xx 为临时故障；5xx/拒收不可盲目重试。
        for (Throwable e : errors) {
            if (e instanceof org.eclipse.angus.mail.smtp.SMTPAddressFailedException smtp) {
                return responseFailure(smtp.getReturnCode());
            }
            if (e instanceof org.eclipse.angus.mail.smtp.SMTPSendFailedException smtp) {
                return responseFailure(smtp.getReturnCode());
            }
        }
        if (errors.stream().anyMatch(e -> e instanceof SendFailedException)) {
            return new EmailDeliveryException("邮件被拒收，请核对发件地址、收件地址及邮箱发送限制", false);
        }
        if (errors.stream().anyMatch(e -> e instanceof IOException)) {
            return new EmailDeliveryException("邮件服务暂时无法连接，将按策略重试；请检查网络和 SMTP 设置", true);
        }
        return new EmailDeliveryException("邮件发送失败，请检查系统邮箱设置或联系邮箱服务商", false);
    }

    private static EmailDeliveryException responseFailure(int code) {
        return code >= 400 && code < 500
                ? new EmailDeliveryException("邮箱服务暂时不可用，将按策略重试", true)
                : new EmailDeliveryException("邮件被拒收，请核对发件地址、收件地址及邮箱发送限制", false);
    }

    private static void collect(Throwable e, List<Throwable> errors, Set<Throwable> visited) {
        if (e == null || !visited.add(e)) return;
        errors.add(e);
        collect(e.getCause(), errors, visited);
        if (e instanceof MessagingException messaging) collect(messaging.getNextException(), errors, visited);
        if (e instanceof MailSendException send) {
            for (Exception nested : send.getMessageExceptions()) collect(nested, errors, visited);
        }
    }
}
