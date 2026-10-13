package com.couple.ordering.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.entity.OrderItem;
import com.couple.ordering.entity.Orders;
import com.couple.ordering.entity.SysUser;
import com.couple.ordering.mapper.OrderItemMapper;
import com.couple.ordering.mapper.OrdersMapper;
import com.couple.ordering.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEmailNotificationService {
    private final EmailSettingsService settings;
    private final EmailDeliveryRepository deliveries;
    private final SmtpEmailProvider smtp;
    private final MockEmailProvider mock;
    private final OrdersMapper orders;
    private final OrderItemMapper items;
    private final SysUserMapper users;

    /** 与订单共用数据库事务：只有订单提交成功，后台才能读取到待发通知。 */
    public void orderSubmitted(Long orderId) {
        var channel = settings.channel();
        if (channel.enabled() && "NEW_ORDER".equals(channel.event())) {
            deliveries.enqueue("NEW_ORDER:" + orderId, "NEW_ORDER", orderId);
        }
    }

    /** 测试由管理员明确点击触发，使用已保存的最新系统邮箱及收件人。 */
    public void test() {
        settings.validateReady(settings.smtp(), settings.channel());
        deliveries.enqueue("TEST:" + UUID.randomUUID(), "TEST", null);
    }

    @Scheduled(fixedDelayString = "${couple.email.poll-interval-ms:3000}")
    public void dispatch() {
        try {
            deliveries.recoverInterrupted();
            for (var delivery : deliveries.due()) deliver(delivery);
        } catch (Exception e) {
            // 不输出 SMTP 响应、配置或正文，错误投递可在管理页追踪。
            log.error("邮件通知任务暂时不可用，等待下轮处理");
        }
    }

    void deliver(EmailDeliveryRepository.Delivery delivery) {
        String token = UUID.randomUUID().toString();
        if (!deliveries.claim(delivery.id(), token)) return;
        int recipientCount = 0;
        String status;
        String error = "";
        LocalDateTime next = LocalDateTime.now();
        try {
            var channel = settings.channel();
            if ("NEW_ORDER".equals(delivery.eventType()) && !channel.enabled()) {
                deliveries.finish(delivery.id(), token, "SKIPPED", 0, "新订单邮件通知已关闭", next);
                return;
            }
            // 每次发送和重试动态读取配置；Mock 无需读取真实 SMTP 配置。
            var config = settings.isMock()
                    ? new EmailSettingsService.StoredSmtp("", 465, "TLS", "", "", "") : settings.smtp();
            settings.validateReady(config, channel);
            List<String> recipients = settings.recipients(channel.recipients());
            recipientCount = recipients.size();
            EmailProvider.Message message = "TEST".equals(delivery.eventType())
                    ? new EmailProvider.Message("【情侣点餐】大厨新订单通知测试", "大厨你好！\n\n这是一封测试邮件。开启新订单通知后，顾客下单成功会自动发送邮件提醒。\n请到管理端订单管理查看并处理订单。")
                    : render(delivery.orderId());
            (settings.isMock() ? mock : smtp).send(config, recipients, message);
            status = settings.isMock() ? "MOCK" : "SENT";
        } catch (EmailDeliveryException e) {
            error = e.getMessage();
            boolean retry = e.isRetryable() && delivery.attempts() + 1 < 3;
            status = retry ? "RETRY" : "FAILED";
            next = next.plusSeconds(delivery.attempts() == 0 ? 30 : 120);
        } catch (BusinessException e) {
            status = "FAILED";
            error = e.getMessage();
        } catch (Exception e) {
            status = "FAILED";
            error = "通知处理失败，请检查订单及系统邮箱配置";
        }
        deliveries.finish(delivery.id(), token, status, recipientCount, error, next);
        log.info("邮件投递 id={} status={} attempt={}", delivery.id(), status, delivery.attempts() + 1);
    }

    EmailProvider.Message render(Long orderId) {
        Orders order = orders.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在，无法生成邮件通知");
        SysUser user = users.selectById(order.getUserId());
        DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder body = new StringBuilder("大厨，有顾客下单啦！\n\n")
                .append("订单号：").append(order.getOrderNo())
                .append("\n顾客：").append(user == null ? "食客" : text(user.getNickname()))
                .append("\n下单时间：").append(order.getCreateTime() == null ? "—" : time.format(order.getCreateTime()))
                .append("\n期望用餐：").append(order.getExpectTime() == null ? "尽快安排" : time.format(order.getExpectTime()))
                .append("\n当前状态：").append(com.couple.ordering.enums.OrderStatus.from(order.getStatus()).getLabel())
                .append("\n\n菜品清单：\n");
        for (OrderItem item : items.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId))) {
            body.append("• ").append(text(item.getDishName())).append(" × ").append(item.getQuantity())
                    .append("\n  口味：").append(text(item.getTaste()))
                    .append("\n  备注：").append(text(item.getRemark())).append("\n");
        }
        body.append("\n整单备注：").append(text(order.getRemark()))
                .append("\n爱心值：").append(order.getTotalLove())
                .append("\n\n请打开大厨管理端的「订单管理」查看并接单。");
        // 主题只使用系统生成的订单号；用户内容以纯文本写入正文。
        return new EmailProvider.Message("【情侣点餐】新订单 " + order.getOrderNo() + "，请大厨接单", body.toString());
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }
}
