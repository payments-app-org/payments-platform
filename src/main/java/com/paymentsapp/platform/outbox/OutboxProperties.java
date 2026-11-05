package com.paymentsapp.platform.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Outbox включает сервис, который публикует события (таблица outbox в его схеме)
@ConfigurationProperties("platform.outbox")
public record OutboxProperties(
        @DefaultValue("false") boolean enabled,
        // Как часто забирать неотправленные события
        @DefaultValue("1s") Duration publishInterval,
        @DefaultValue("100") int batchSize,
        // Сколько ждать подтверждения Kafka по одному событию
        @DefaultValue("5s") Duration sendTimeout,
        // Сколько хранить отправленные события (для разбора инцидентов), потом удаляем
        @DefaultValue("7d") Duration retention,
        @DefaultValue("0 30 3 * * *") String cleanupCron,
        // Удаляем пачками, чтобы не держать долгую транзакцию
        @DefaultValue("1000") int cleanupBatchSize
) {
}
