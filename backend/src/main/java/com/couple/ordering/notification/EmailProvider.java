package com.couple.ordering.notification;

import java.util.List;

public interface EmailProvider {
    record Message(String subject, String body) {}
    void send(EmailSettingsService.StoredSmtp settings, List<String> recipients, Message message);
}
