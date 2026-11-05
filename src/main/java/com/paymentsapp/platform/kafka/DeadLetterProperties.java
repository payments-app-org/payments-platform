package com.paymentsapp.platform.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Сообщение, которое консьюмер не смог обработать после повторов, уходит в топик <topic><topicSuffix>
@ConfigurationProperties("platform.kafka.dead-letter")
public record DeadLetterProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("-dlt") String topicSuffix,
        @DefaultValue("3") int maxRetries,
        @DefaultValue("1s") Duration initialInterval,
        @DefaultValue("2.0") double multiplier,
        @DefaultValue("10s") Duration maxInterval
) {
}
