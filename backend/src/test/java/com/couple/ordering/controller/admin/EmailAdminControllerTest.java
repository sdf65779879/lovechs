package com.couple.ordering.controller.admin;

import com.couple.ordering.common.security.*;
import com.couple.ordering.common.exception.GlobalExceptionHandler;
import com.couple.ordering.config.SecurityConfig;
import com.couple.ordering.notification.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailAdminController.class)
@ContextConfiguration(classes = {EmailAdminController.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class EmailAdminControllerTest {
    @Autowired MockMvc mvc;
    @MockBean EmailSettingsService settings;
    @MockBean OrderEmailNotificationService notifications;
    @MockBean EmailDeliveryRepository deliveries;
    @MockBean JwtUtils jwt;
    @MockBean UserDetailsService users;

    @Test
    void anonymousCannotReadSettingsOrSendTestMail() throws Exception {
        mvc.perform(get("/v1/admin/email/settings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/admin/email/test")).andExpect(status().isUnauthorized());
        verifyNoInteractions(settings, notifications);
    }

    @Test
    @WithMockUser(roles = "USER")
    void customerCannotChangeEmailSettingsOrReadDeliveryLog() throws Exception {
        mvc.perform(get("/v1/admin/email/deliveries")).andExpect(status().isForbidden());
        mvc.perform(post("/v1/admin/email/test")).andExpect(status().isForbidden());
        verifyNoInteractions(notifications, deliveries);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanReadSafeViewAndExplicitlyTriggerTest() throws Exception {
        when(settings.view()).thenReturn(new EmailSettingsService.SettingsView(
                new EmailSettingsService.SmtpView("smtp.example.com", 465, "TLS", "sender@example.com", "sender", true),
                new EmailSettingsService.Channel(true, "chef@example.com", "NEW_ORDER"), "SMTP"));
        mvc.perform(get("/v1/admin/email/settings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.smtp.passwordConfigured").value(true))
                .andExpect(jsonPath("$.data.smtp.password").doesNotExist())
                .andExpect(jsonPath("$.data.smtp.encryptedPassword").doesNotExist());
        mvc.perform(post("/v1/admin/email/test")).andExpect(jsonPath("$.code").value(0));
        verify(notifications).test();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidPortOrEventNeverReachesSave() throws Exception {
        mvc.perform(put("/v1/admin/email/settings").contentType("application/json").content("""
                {"smtp":{"host":"smtp.example.com","port":0,"encryption":"TLS"},"channel":{"event":"OTHER"}}
                """)).andExpect(jsonPath("$.code").value(400));
        verify(settings, never()).save(any());
    }
}
