package com.paymentsapp.platform.outbox;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

// Публикует неотправленные события в Kafka. Гарантия «хотя бы один раз»: упали после отправки до коммита —
// событие уйдёт повторно, получатели отсекают дубли. FOR UPDATE SKIP LOCKED: несколько подов делят события,
// не отправляя одно и то же дважды.
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxPublisher(JdbcClient jdbc, KafkaTemplate<String, String> kafka, OutboxProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void publish() {
        List<PendingEvent> batch = jdbc.sql("""
                        select id, topic, aggregate_id, payload::text as payload from outbox
                        where published_at is null
                        order by created_at
                        limit :batch
                        for update skip locked
                        """)
                .param("batch", properties.batchSize())
                .query(PendingEvent.class)
                .list();
        for (PendingEvent event : batch) {
            try {
                kafka.send(event.topic(), event.aggregateId(), event.payload())
                        .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Остальные события пачки — в следующий раз, чтобы не нарушить порядок
                log.warn("Не удалось отправить событие {} в Kafka: {}", event.id(), e.getMessage());
                return;
            }
            jdbc.sql("update outbox set published_at = :now where id = :id")
                    .param("now", OffsetDateTime.now(clock))
                    .param("id", event.id())
                    .update();
        }
    }

    record PendingEvent(UUID id, String topic, String aggregateId, String payload) {
    }
}
