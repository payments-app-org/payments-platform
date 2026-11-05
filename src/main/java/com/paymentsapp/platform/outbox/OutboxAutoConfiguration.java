package com.paymentsapp.platform.outbox;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import tools.jackson.databind.json.JsonMapper;

// Включается сервисом: platform.outbox.enabled=true. Нужны JdbcClient, Kafka и @EnableScheduling.
@AutoConfiguration
@ConditionalOnClass({JdbcClient.class, KafkaTemplate.class})
@ConditionalOnProperty(prefix = "platform.outbox", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    @Bean
    public Outbox outbox(JdbcClient jdbc, JsonMapper json, Clock clock) {
        return new Outbox(jdbc, json, clock);
    }

    @Bean
    public OutboxPublisher outboxPublisher(JdbcClient jdbc, KafkaTemplate<String, String> kafka,
                                           OutboxProperties properties, Clock clock) {
        return new OutboxPublisher(jdbc, kafka, properties, clock);
    }

    @Bean
    public OutboxCleanup outboxCleanup(JdbcClient jdbc, OutboxProperties properties, Clock clock) {
        return new OutboxCleanup(jdbc, properties, clock);
    }

    // Расписание из OutboxProperties, а не из аннотаций: интервалы — в конфигурации сервиса
    @Bean
    public SchedulingConfigurer outboxSchedule(OutboxPublisher publisher, OutboxCleanup cleanup, OutboxProperties properties) {
        return (ScheduledTaskRegistrar registrar) -> {
            registrar.addFixedDelayTask(publisher::publish, properties.publishInterval());
            registrar.addCronTask(cleanup::cleanup, properties.cleanupCron());
        };
    }
}
