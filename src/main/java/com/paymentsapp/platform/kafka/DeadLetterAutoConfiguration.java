package com.paymentsapp.platform.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;

// Ошибка в консьюмере: несколько повторов с растущей паузой, потом — в DLT. Битый JSON не повторяем.
// Без этого Spring Kafka после 10 быстрых повторов просто пропускает сообщение — событие терялось бы.
@AutoConfiguration
@ConditionalOnClass(KafkaOperations.class)
@ConditionalOnProperty(prefix = "platform.kafka.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DeadLetterProperties.class)
public class DeadLetterAutoConfiguration {

    // Партиция -1: Kafka выберет сама, DLT может быть меньше исходного топика
    private static final int ANY_PARTITION = -1;

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafka, DeadLetterProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, e) -> new TopicPartition(record.topic() + properties.topicSuffix(), ANY_PARTITION));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(properties.maxRetries());
        backOff.setInitialInterval(properties.initialInterval().toMillis());
        backOff.setMultiplier(properties.multiplier());
        backOff.setMaxInterval(properties.maxInterval().toMillis());
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}
