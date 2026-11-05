package com.paymentsapp.platform.outbox;

import java.time.Clock;
import java.time.OffsetDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

// Удаляет отправленные события старше retention. Пачками: таблица под нагрузкой не блокируется надолго.
public class OutboxCleanup {

    static final String LOCK = "outbox-cleanup";

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanup.class);

    private final JdbcClient jdbc;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxCleanup(JdbcClient jdbc, OutboxProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
    }

    @SchedulerLock(name = LOCK)
    public void cleanup() {
        OffsetDateTime before = OffsetDateTime.now(clock).minus(properties.retention());
        int total = 0;
        int deleted;
        do {
            deleted = jdbc.sql("""
                            delete from outbox where id in (
                                select id from outbox where published_at < :before limit :batch)
                            """)
                    .param("before", before)
                    .param("batch", properties.cleanupBatchSize())
                    .update();
            total += deleted;
        } while (deleted == properties.cleanupBatchSize());
        if (total > 0) {
            log.info("outbox: удалено {} отправленных событий старше {}", total, before);
        }
    }
}
