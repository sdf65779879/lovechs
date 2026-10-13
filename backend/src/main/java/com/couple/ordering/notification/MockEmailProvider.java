package com.couple.ordering.notification;

import org.springframework.stereotype.Component;
import java.util.List;

/** 模拟投递，不读密钥，不记录邮件正文/收件人，不建立网络连接。 */
@Component
public class MockEmailProvider implements EmailProvider {
    @Override
    public void send(EmailSettingsService.StoredSmtp settings, List<String> recipients, Message message) {
        // 成功由工作流记录为 MOCK，不能冒充真实发送成功。
    }
}
