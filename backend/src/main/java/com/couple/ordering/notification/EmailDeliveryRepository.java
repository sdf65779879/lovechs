package com.couple.ordering.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmailDeliveryRepository {
    private final JdbcTemplate jdbc;
    public record Delivery(long id, String eventType, Long orderId, String status, int attempts,
                           int recipientCount, String errorMessage, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {}
    private final RowMapper<Delivery> row = (rs, index) -> new Delivery(rs.getLong("id"),
            rs.getString("event_type"), rs.getObject("order_id", Long.class), rs.getString("status"),
            rs.getInt("attempts"), rs.getInt("recipient_count"), rs.getString("error_message"),
            rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());

    public void enqueue(String key, String event, Long orderId) {
        jdbc.update("INSERT INTO email_delivery(event_key,event_type,order_id) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE event_key=event_key", key, event, orderId);
    }

    public List<Delivery> recent() {
        return jdbc.query("SELECT * FROM email_delivery ORDER BY id DESC LIMIT 20", row);
    }

    public List<Delivery> due() {
        return jdbc.query("SELECT * FROM email_delivery WHERE status IN ('PENDING','RETRY') "
                + "AND attempts < 3 AND next_attempt_at <= CURRENT_TIMESTAMP ORDER BY id LIMIT 10", row);
    }

    public boolean claim(long id, String token) {
        return jdbc.update("UPDATE email_delivery SET status='SENDING',attempts=attempts+1,lock_token=?,updated_at=CURRENT_TIMESTAMP "
                + "WHERE id=? AND status IN ('PENDING','RETRY') AND attempts < 3 AND next_attempt_at<=CURRENT_TIMESTAMP", token, id) == 1;
    }

    public void finish(long id, String token, String status, int recipients, String error, LocalDateTime next) {
        jdbc.update("UPDATE email_delivery SET status=?,recipient_count=?,error_message=?,next_attempt_at=?,"
                + "lock_token=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND lock_token=? AND status='SENDING'",
                status, recipients, error, next, id, token);
    }

    public void recoverInterrupted() {
        // SMTP 发送与数据库确认无法组成原子事务。中断时结果未知，避免自动重发导致重复提醒。
        jdbc.update("UPDATE email_delivery SET status='FAILED',lock_token=NULL,"
                + "error_message='服务中断，投递结果未知，请核对大厨收件箱及订单列表',updated_at=CURRENT_TIMESTAMP "
                + "WHERE status='SENDING' AND updated_at < ?", LocalDateTime.now().minusMinutes(5));
    }
}
