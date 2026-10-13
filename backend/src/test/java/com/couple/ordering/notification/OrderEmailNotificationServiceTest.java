package com.couple.ordering.notification;

import com.couple.ordering.entity.*;
import com.couple.ordering.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderEmailNotificationServiceTest {
    EmailSettingsService settings = mock(EmailSettingsService.class);
    EmailDeliveryRepository repository = mock(EmailDeliveryRepository.class);
    SmtpEmailProvider smtp = mock(SmtpEmailProvider.class);
    MockEmailProvider mock = mock(MockEmailProvider.class);
    OrdersMapper orders = mock(OrdersMapper.class);
    OrderItemMapper items = mock(OrderItemMapper.class);
    SysUserMapper users = mock(SysUserMapper.class);
    OrderEmailNotificationService service = new OrderEmailNotificationService(settings, repository, smtp, mock, orders, items, users);
    EmailSettingsService.StoredSmtp config = new EmailSettingsService.StoredSmtp("host", 465, "TLS", "sender@example.com", "sender", "encrypted");

    @BeforeEach
    void setup() {
        when(settings.channel()).thenReturn(new EmailSettingsService.Channel(true, "chef@example.com", "NEW_ORDER"));
        when(settings.smtp()).thenReturn(config);
        when(settings.recipients(anyString())).thenReturn(List.of("chef@example.com"));
        when(repository.claim(anyLong(), anyString())).thenReturn(true);
    }

    EmailDeliveryRepository.Delivery delivery(int attempts, String event) {
        return new EmailDeliveryRepository.Delivery(1L, event, "TEST".equals(event) ? null : 5L, "PENDING", attempts, 0, "", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void disabledChannelDoesNotQueueAndEnabledUsesStableEventKey() {
        when(settings.channel()).thenReturn(new EmailSettingsService.Channel(false, "", "NEW_ORDER"));
        service.orderSubmitted(5L);
        verifyNoInteractions(repository);
        when(settings.channel()).thenReturn(new EmailSettingsService.Channel(true, "chef@example.com", "NEW_ORDER"));
        service.orderSubmitted(5L);
        verify(repository).enqueue("NEW_ORDER:5", "NEW_ORDER", 5L);
    }

    @Test
    void mockDispatchDoesNotReadSmtpOrCallRealProvider() {
        when(settings.isMock()).thenReturn(true);
        service.deliver(delivery(0, "TEST"));
        verify(settings, never()).smtp();
        verify(mock).send(any(), eq(List.of("chef@example.com")), any());
        verifyNoInteractions(smtp);
        verify(repository).finish(eq(1L), anyString(), eq("MOCK"), eq(1), eq(""), any());
    }

    @Test
    void temporaryFailureRetriesWithFreshConfigAndStopsAfterThreeAttempts() {
        doThrow(new EmailDeliveryException("临时故障", true)).when(smtp).send(any(), anyList(), any());
        service.deliver(delivery(0, "TEST"));
        verify(repository).finish(eq(1L), anyString(), eq("RETRY"), eq(1), eq("临时故障"), any());
        var updated = new EmailSettingsService.StoredSmtp("new-host", 587, "STARTTLS", "new@example.com", "", "");
        when(settings.smtp()).thenReturn(updated);
        service.deliver(delivery(2, "TEST"));
        verify(smtp).send(eq(updated), anyList(), any());
        verify(repository).finish(eq(1L), anyString(), eq("FAILED"), eq(1), eq("临时故障"), any());
    }

    @Test
    void permanentFailureDoesNotRetryAndDisabledPendingOrdersAreSkipped() {
        doThrow(new EmailDeliveryException("认证失败", false)).when(smtp).send(any(), anyList(), any());
        service.deliver(delivery(0, "TEST"));
        verify(repository).finish(eq(1L), anyString(), eq("FAILED"), eq(1), eq("认证失败"), any());
        reset(smtp);
        when(settings.channel()).thenReturn(new EmailSettingsService.Channel(false, "chef@example.com", "NEW_ORDER"));
        service.deliver(delivery(0, "NEW_ORDER"));
        verifyNoInteractions(smtp);
        verify(repository).finish(eq(1L), anyString(), eq("SKIPPED"), eq(0), anyString(), any());
    }

    @Test
    void rendersCustomerAndOrderSnapshotsAsPlainText() {
        Orders order = new Orders();
        order.setId(5L); order.setOrderNo("202610130000000001"); order.setUserId(2L);
        order.setStatus("PENDING"); order.setTotalLove(8); order.setRemark("不要葱");
        order.setExpectTime(LocalDateTime.of(2026, 10, 13, 19, 0));
        SysUser user = new SysUser(); user.setNickname("宝贝");
        OrderItem item = new OrderItem(); item.setDishName("番茄牛腩拌面"); item.setQuantity(2); item.setTaste("微辣"); item.setRemark("少盐");
        when(orders.selectById(5L)).thenReturn(order);
        when(users.selectById(2L)).thenReturn(user);
        when(items.selectList(any())).thenReturn(List.of(item));
        var message = service.render(5L);
        assertThat(message.subject()).contains(order.getOrderNo());
        assertThat(message.body()).contains("宝贝", "番茄牛腩拌面 × 2", "微辣", "少盐", "不要葱", "2026-10-13 19:00");
    }
}
