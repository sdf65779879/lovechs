package com.couple.ordering.notification;

import com.couple.ordering.common.security.LoginUser;
import com.couple.ordering.common.utils.IdempotencyStore;
import com.couple.ordering.dto.OrderSubmitDTO;
import com.couple.ordering.entity.*;
import com.couple.ordering.mapper.*;
import com.couple.ordering.service.OrderService;
import com.couple.ordering.service.SysConfigService;
import com.couple.ordering.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 使用真实事务代理、真实持久化队列及模拟订单 Mapper，验证下单通知路由与回滚。 */
@SpringJUnitConfig(OrderSubmissionEmailTest.Config.class)
class OrderSubmissionEmailTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource source() {
            var source = new DriverManagerDataSource("jdbc:h2:mem:orderEmail;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            new ResourceDatabasePopulator(new ClassPathResource("db/email-schema.sql")).execute(source);
            return source;
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean EmailDeliveryRepository deliveries(JdbcTemplate jdbc) { return new EmailDeliveryRepository(jdbc); }
        @Bean OrdersMapper orders() { return mock(OrdersMapper.class); }
        @Bean OrderItemMapper items() { return mock(OrderItemMapper.class); }
        @Bean CartItemMapper carts() { return mock(CartItemMapper.class); }
        @Bean DishMapper dishes() { return mock(DishMapper.class); }
        @Bean SysUserMapper users() { return mock(SysUserMapper.class); }
        @Bean SysConfigService configs() { return mock(SysConfigService.class); }
        @Bean EmailSettingsService settings() { return mock(EmailSettingsService.class); }
        @Bean SmtpEmailProvider smtp() { return mock(SmtpEmailProvider.class); }
        @Bean MockEmailProvider mockProvider() { return new MockEmailProvider(); }
        @Bean IdempotencyStore idempotency() { return new IdempotencyStore(); }
        @Bean OrderEmailNotificationService notifications(EmailSettingsService settings, EmailDeliveryRepository deliveries,
                SmtpEmailProvider smtp, MockEmailProvider mock, OrdersMapper orders, OrderItemMapper items, SysUserMapper users) {
            return new OrderEmailNotificationService(settings, deliveries, smtp, mock, orders, items, users);
        }
        @Bean OrderService orderService(OrdersMapper orders, OrderItemMapper items, CartItemMapper carts, DishMapper dishes,
                SysUserMapper users, SysConfigService configs, IdempotencyStore idempotency, OrderEmailNotificationService notifications) {
            return new OrderServiceImpl(orders, items, carts, dishes, users, configs, idempotency, notifications);
        }
    }

    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmailDeliveryRepository deliveries;
    @Autowired OrdersMapper orders;
    @Autowired OrderItemMapper items;
    @Autowired CartItemMapper carts;
    @Autowired DishMapper dishes;
    @Autowired SysConfigService configs;
    @Autowired EmailSettingsService settings;
    @Autowired SmtpEmailProvider smtp;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM email_delivery");
        reset(orders, items, carts, dishes, configs, settings, smtp);
        var principal = new LoginUser(2L, "love", "", true, "USER", List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", List.of()));
        when(configs.isBusinessOpen()).thenReturn(true);
        when(settings.channel()).thenReturn(new EmailSettingsService.Channel(true, "chef@example.com", "NEW_ORDER"));
        CartItem cart = new CartItem(); cart.setDishId(1L); cart.setQuantity(2);
        when(carts.selectList(any())).thenReturn(List.of(cart));
        Dish dish = new Dish(); dish.setId(1L); dish.setName("番茄面"); dish.setStatus(1); dish.setLoveValue(5);
        when(dishes.selectById(1L)).thenReturn(dish);
        when(orders.selectCount(any())).thenReturn(0L);
        when(orders.insert(any(Orders.class))).thenAnswer(invocation -> {
            Orders order = invocation.getArgument(0); order.setId(10L);
            when(orders.selectById(10L)).thenReturn(order);
            return 1;
        });
        when(items.selectList(any())).thenReturn(List.of());
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void successfulNewOrderQueuesOnceAndIdempotentReplayDoesNotQueueAgain() {
        service.submit(new OrderSubmitDTO(), "mail-test-success");
        service.submit(new OrderSubmitDTO(), "mail-test-success");
        assertThat(deliveries.recent()).hasSize(1);
        assertThat(deliveries.recent().get(0).orderId()).isEqualTo(10L);
        verify(orders, times(1)).insert(any(Orders.class));
        verifyNoInteractions(smtp);
    }

    @Test
    void failedOrderRollsBackQueuedNotification() {
        when(items.selectList(any())).thenThrow(new IllegalStateException("模拟订单详情生成失败"));
        assertThatThrownBy(() -> service.submit(new OrderSubmitDTO(), null)).isInstanceOf(IllegalStateException.class);
        assertThat(deliveries.recent()).isEmpty();
        verifyNoInteractions(smtp);
    }

    @Test
    void closedBusinessAndEmptyCartDoNotQueueNotifications() {
        when(configs.isBusinessOpen()).thenReturn(false);
        assertThatThrownBy(() -> service.submit(new OrderSubmitDTO(), null)).hasMessageContaining("不营业");
        when(configs.isBusinessOpen()).thenReturn(true);
        when(carts.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.submit(new OrderSubmitDTO(), null)).hasMessageContaining("购物车");
        assertThat(deliveries.recent()).isEmpty();
    }
}
