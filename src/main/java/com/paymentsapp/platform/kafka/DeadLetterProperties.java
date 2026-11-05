package com.paymentsapp.platform.kafka;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Сообщение, которое консьюмер не смог обработать после повторов, уходит в DLT своей группы: <topic>.<group><topicSuffix>.
// topics и groups — какие топики и какими группами читает сервис: для них доступен разбор DLT (/api/internal/dlt/{topic}).
@ConfigurationProperties("platform.kafka.dead-letter")
public record DeadLetterProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("-dlt") String topicSuffix,
        @DefaultValue("3") int maxRetries,
        @DefaultValue("1s") Duration initialInterval,
        @DefaultValue("2.0") double multiplier,
        @DefaultValue("10s") Duration maxInterval,
        @DefaultValue Set<String> topics,
        @DefaultValue Set<String> groups,
        @DefaultValue("-dlt-replay") String replayGroupSuffix
) {

    private static final String SEPARATOR = ".";

    public String dlt(String topic, String group) {
        return topic + SEPARATOR + group + topicSuffix;
    }
}
