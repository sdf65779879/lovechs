package com.couple.ordering.notification;

import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.dto.EmailSettingsDTO;
import com.couple.ordering.service.SysConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailSettingsServiceTest {
    EmailSettingsService service;
    JdbcTemplate jdbc;
    EmailSecrets secrets;
    ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE sys_config(config_key VARCHAR(100) UNIQUE,config_value VARCHAR(2000),remark VARCHAR(100),update_time TIMESTAMP)");
        SysConfigService configs = mock(SysConfigService.class);
        when(configs.getConfigValue(anyString())).thenAnswer(invocation -> {
            List<String> values = jdbc.queryForList("SELECT config_value FROM sys_config WHERE config_key=?", String.class, (String) invocation.getArgument(0));
            return values.isEmpty() ? null : values.get(0);
        });
        secrets = new EmailSecrets(Base64.getEncoder().encodeToString(new byte[32]));
        service = new EmailSettingsService(configs, jdbc, json, secrets);
        ReflectionTestUtils.setField(service, "provider", "SMTP");
    }

    EmailSettingsDTO dto() {
        var dto = new EmailSettingsDTO();
        dto.setSmtp(new EmailSettingsDTO.Smtp());
        dto.setChannel(new EmailSettingsDTO.Channel());
        dto.getSmtp().setHost("smtp.example.com");
        dto.getSmtp().setFromAddress("sender@example.com");
        dto.getSmtp().setUsername("sender@example.com");
        dto.getSmtp().setPassword("test-app-password");
        dto.getChannel().setRecipients(" Chef@example.com ; chef@EXAMPLE.com，second@example.com ");
        dto.getChannel().setEnabled(true);
        return dto;
    }

    @Test
    void encryptsPasswordAndDoesNotExposeItInView() throws Exception {
        service.save(dto());
        String stored = jdbc.queryForObject("SELECT config_value FROM sys_config WHERE config_key='email_system'", String.class);
        assertThat(stored).doesNotContain("test-app-password").contains("v1:");
        assertThat(secrets.decrypt(service.smtp().encryptedPassword())).isEqualTo("test-app-password");
        String view = json.writeValueAsString(service.view());
        assertThat(view).doesNotContain("test-app-password", "encryptedPassword", "v1:").contains("passwordConfigured");
        assertThat(service.channel().recipients()).isEqualTo("Chef@example.com,second@example.com");
    }

    @Test
    void blankPasswordRetainsExistingValueAndClearRemovesIt() {
        var dto = dto();
        service.save(dto);
        String encrypted = service.smtp().encryptedPassword();
        dto.getSmtp().setPassword("");
        dto.getSmtp().setHost("new-smtp.example.com");
        service.save(dto);
        assertThat(service.smtp().encryptedPassword()).isEqualTo(encrypted);
        assertThat(service.smtp().host()).isEqualTo("new-smtp.example.com");
        dto.getChannel().setEnabled(false);
        dto.getSmtp().setClearPassword(true);
        service.save(dto);
        assertThat(service.view().smtp().passwordConfigured()).isFalse();
    }

    @Test
    void recipientsRejectInjectionDisplayNamesAndExcessAddresses() {
        for (String input : List.of("a@example.com\r\nBcc: victim@example.com", "Chef <chef@example.com>", "invalid", "a@b.com,b@b.com,c@b.com,d@b.com,e@b.com,f@b.com")) {
            assertThatThrownBy(() -> service.recipients(input)).isInstanceOf(BusinessException.class);
        }
        assertThat(service.recipients(" ; , ")).isEmpty();
        assertThat(service.recipients("a@example.com,A@example.com")).containsExactly("a@example.com");
    }

    @Test
    void enabledChannelRequiresRecipientsAndReadySmtp() {
        var dto = dto();
        dto.getChannel().setRecipients("");
        assertThatThrownBy(() -> service.save(dto)).hasMessageContaining("收件邮箱");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_config", Integer.class)).isZero();
        dto.getChannel().setRecipients("chef@example.com");
        dto.getSmtp().setHost("");
        assertThatThrownBy(() -> service.save(dto)).hasMessageContaining("SMTP 主机");
    }

    @Test
    void mockNeedsNeitherSmtpNorEncryptionKey() {
        service = new EmailSettingsService(mock(SysConfigService.class), jdbc, json, new EmailSecrets(""));
        ReflectionTestUtils.setField(service, "provider", "MOCK");
        var dto = dto();
        dto.getSmtp().setHost("");
        dto.getSmtp().setPassword(null);
        service.save(dto);
        assertThat(service.view().provider()).isEqualTo("MOCK");
    }

    @Test
    void authenticatedEncryptionRejectsWrongKeyAndTamperedCiphertext() {
        String encrypted = secrets.encrypt("private-password");
        assertThat(secrets.encrypt("private-password")).isNotEqualTo(encrypted);
        byte[] different = new byte[32];
        different[0] = 1;
        var wrong = new EmailSecrets(Base64.getEncoder().encodeToString(different));
        assertThatThrownBy(() -> wrong.decrypt(encrypted)).isInstanceOf(BusinessException.class).hasMessageNotContaining("private-password");
        assertThatThrownBy(() -> secrets.decrypt(encrypted.substring(0, encrypted.length() - 4) + "AAAA"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new EmailSecrets("").encrypt("private-password"))
                .hasMessageContaining("MAIL_ENCRYPTION_KEY").hasMessageNotContaining("private-password");
    }
}
