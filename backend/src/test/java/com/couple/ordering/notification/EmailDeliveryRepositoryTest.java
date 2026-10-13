package com.couple.ordering.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EmailDeliveryRepositoryTest {
    JdbcTemplate jdbc;
    EmailDeliveryRepository repository;
    DriverManagerDataSource source;

    @BeforeEach
    void setup() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/email-schema.sql")).execute(source);
        jdbc = new JdbcTemplate(source);
        repository = new EmailDeliveryRepository(jdbc);
    }

    @Test
    void queuesOnlyBecomeVisibleOnCommitAndRollbackRemovesThem() {
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.executeWithoutResult(status -> {
            repository.enqueue("NEW_ORDER:1", "NEW_ORDER", 1L);
            try (var connection = source.getConnection(); var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM email_delivery")) {
                result.next();
                assertThat(result.getInt(1)).isZero();
            } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
        });
        assertThat(repository.due()).hasSize(1);
        transaction.executeWithoutResult(status -> {
            repository.enqueue("NEW_ORDER:2", "NEW_ORDER", 2L);
            status.setRollbackOnly();
        });
        assertThat(repository.recent()).hasSize(1);
    }

    @Test
    void duplicateEventAndConcurrentClaimsDoNotResend() {
        repository.enqueue("NEW_ORDER:1", "NEW_ORDER", 1L);
        repository.enqueue("NEW_ORDER:1", "NEW_ORDER", 1L);
        long id = repository.recent().get(0).id();
        assertThat(repository.recent()).hasSize(1);
        assertThat(repository.claim(id, "worker-a")).isTrue();
        assertThat(repository.claim(id, "worker-b")).isFalse();
        repository.finish(id, "worker-b", "SENT", 1, "", LocalDateTime.now());
        assertThat(repository.recent().get(0).status()).isEqualTo("SENDING");
        repository.finish(id, "worker-a", "SENT", 1, "", LocalDateTime.now());
        assertThat(repository.claim(id, "worker-a")).isFalse();
        assertThat(repository.due()).isEmpty();
    }

    @Test
    void retryTimeAndAttemptLimitAreEnforcedAndSurviveRepositoryRecreation() {
        repository.enqueue("NEW_ORDER:1", "NEW_ORDER", 1L);
        long id = repository.recent().get(0).id();
        repository.claim(id, "a");
        repository.finish(id, "a", "RETRY", 1, "暂时不可用", LocalDateTime.now().plusMinutes(1));
        assertThat(new EmailDeliveryRepository(jdbc).due()).isEmpty();
        jdbc.update("UPDATE email_delivery SET next_attempt_at=?,attempts=3 WHERE id=?", LocalDateTime.now().minusMinutes(1), id);
        assertThat(repository.claim(id, "a")).isFalse();
        assertThat(repository.due()).isEmpty();
    }

    @Test
    void interruptedSendHasVisibleUnknownResultInsteadOfAutomaticDuplicate() {
        repository.enqueue("NEW_ORDER:1", "NEW_ORDER", 1L);
        long id = repository.recent().get(0).id();
        repository.claim(id, "a");
        jdbc.update("UPDATE email_delivery SET updated_at=?", LocalDateTime.now().minusMinutes(6));
        repository.recoverInterrupted();
        assertThat(repository.recent().get(0).status()).isEqualTo("FAILED");
        assertThat(repository.recent().get(0).errorMessage()).contains("结果未知");
        assertThat(repository.due()).isEmpty();
    }
}
