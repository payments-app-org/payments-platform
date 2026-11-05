package com.paymentsapp.platform.outbox;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

// Запись события в outbox внутри бизнес-транзакции: событие сохранится только вместе с изменениями
public class Outbox {

    static final String EVENT_TYPE = "eventType";
    static final String OCCURRED_AT = "occurredAt";

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public Outbox(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String topic, String aggregateId, String eventType, Map<String, ?> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(EVENT_TYPE, eventType);
        OffsetDateTime now = OffsetDateTime.now(clock);
        payload.put(OCCURRED_AT, now.toString());
        payload.putAll(data);
        jdbc.sql("""
                        insert into outbox (id, topic, aggregate_id, event_type, payload, created_at)
                        values (:id, :topic, :aggregateId, :eventType, cast(:payload as jsonb), :createdAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("topic", topic)
                .param("aggregateId", aggregateId)
                .param("eventType", eventType)
                .param("payload", json.writeValueAsString(payload))
                .param("createdAt", now)
                .update();
    }
}
