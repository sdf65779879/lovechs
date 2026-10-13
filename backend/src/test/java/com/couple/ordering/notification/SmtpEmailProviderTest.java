package com.couple.ordering.notification;

import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpEmailProviderTest {
    @Test
    void sendsUtf8MimeWithCorrectEnvelopeToIsolatedLoopbackServer() throws Exception {
        // 仅监听本地回环，不使用真实邮箱或外部 SMTP。
        var executor = Executors.newSingleThreadExecutor();
        try (var server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5000);
            List<String> envelope = new ArrayList<>();
            Future<String> captured = executor.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    writer.print("220 local-test ESMTP\r\n"); writer.flush();
                    StringBuilder mime = new StringBuilder();
                    boolean data = false;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (data) {
                            if (line.equals(".")) { data = false; writer.print("250 accepted\r\n"); writer.flush(); }
                            else mime.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                        } else if (line.startsWith("DATA")) {
                            data = true; writer.print("354 end with dot\r\n"); writer.flush();
                        } else if (line.startsWith("QUIT")) {
                            writer.print("221 bye\r\n"); writer.flush(); break;
                        } else {
                            if (line.startsWith("MAIL FROM:") || line.startsWith("RCPT TO:")) envelope.add(line);
                            writer.print("250 OK\r\n"); writer.flush();
                        }
                    }
                    return mime.toString();
                }
            });
            var secrets = mock(EmailSecrets.class);
            new SmtpEmailProvider(secrets).send(new EmailSettingsService.StoredSmtp("127.0.0.1", server.getLocalPort(), "NONE", "sender@example.com", "", ""),
                    List.of("chef@example.com", "second@example.com"), new EmailProvider.Message("大厨，新订单来了", "番茄面 × 2\n不要葱"));
            MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(captured.get(5, TimeUnit.SECONDS).getBytes(StandardCharsets.UTF_8)));
            assertThat(mime.getSubject()).isEqualTo("大厨，新订单来了");
            assertThat(mime.getContent().toString()).contains("番茄面 × 2", "不要葱");
            assertThat(envelope).contains("MAIL FROM:<sender@example.com>", "RCPT TO:<chef@example.com>", "RCPT TO:<second@example.com>");
            assertThat(mime.getHeader("Bcc")).isNull();
            verifyNoInteractions(secrets);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void configuresEncryptionTimeoutsAndAuthenticationForAllModes() {
        var secrets = mock(EmailSecrets.class);
        when(secrets.decrypt("encrypted")).thenReturn("app-password");
        var provider = new SmtpEmailProvider(secrets);
        for (String mode : new String[]{"TLS", "STARTTLS", "NONE"}) {
            var sender = provider.sender(new EmailSettingsService.StoredSmtp("smtp.example.com", 465, mode, "sender@example.com", "sender", "encrypted"));
            var properties = sender.getJavaMailProperties();
            assertThat(properties.getProperty("mail.smtp.ssl.enable")).isEqualTo(Boolean.toString(mode.equals("TLS")));
            assertThat(properties.getProperty("mail.smtp.starttls.required")).isEqualTo(Boolean.toString(mode.equals("STARTTLS")));
            assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
            assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("5000");
            assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("5000");
            assertThat(properties.getProperty("mail.debug")).isEqualTo("false");
            assertThat(sender.getPassword()).isEqualTo("app-password");
        }
    }

    @Test
    void anonymousInternalRelayDoesNotDecryptSecrets() {
        var secrets = mock(EmailSecrets.class);
        var sender = new SmtpEmailProvider(secrets).sender(new EmailSettingsService.StoredSmtp("localhost", 25, "NONE", "sender@example.com", "", ""));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("false");
        verifyNoInteractions(secrets);
    }

    @Test
    void classifiesNestedFailuresWithoutLeakingResponsesOrCredentials() {
        String sensitive = "smtp-private-host secret-token chef@private.example";
        var auth = SmtpEmailProvider.safeFailure(new MailSendException("send", null, Map.of("message", new AuthenticationFailedException(sensitive))));
        assertThat(auth.isRetryable()).isFalse();
        assertThat(auth.getMessage()).contains("认证失败").doesNotContain(sensitive);
        var network = SmtpEmailProvider.safeFailure(new MailSendException("send", new SocketTimeoutException(sensitive)));
        assertThat(network.isRetryable()).isTrue();
        var tls = SmtpEmailProvider.safeFailure(new SSLHandshakeException(sensitive));
        assertThat(tls.isRetryable()).isFalse();
        var temporary = SmtpEmailProvider.safeFailure(new org.eclipse.angus.mail.smtp.SMTPSendFailedException("DATA", 451, sensitive, null, null, null, null));
        assertThat(temporary.isRetryable()).isTrue();
        var rejected = SmtpEmailProvider.safeFailure(new org.eclipse.angus.mail.smtp.SMTPSendFailedException("DATA", 550, sensitive, null, null, null, null));
        assertThat(rejected.isRetryable()).isFalse();
        assertThat(rejected.getMessage()).doesNotContain(sensitive);
    }
}
