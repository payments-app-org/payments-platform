package com.paymentsapp.platform.kafka;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.KafkaUtils;
import tools.jackson.core.JacksonException;

// Ошибка в консьюмере: несколько повторов с растущей паузой, потом — в DLT своей группы (<топик>.<группа>-dlt):
// у топика может быть несколько групп-читателей, и сбой одной не должен попадать в разбор другой. Битый JSON не повторяем.
// Без этого Spring Kafka после 10 быстрых повторов просто пропускает сообщение — событие терялось бы.
// Каждое сообщение в DLT — ERROR в логе (по нему настроим оповещение на этапе наблюдаемости);
// разбор — DeadLetterController (сколько ждёт, повторная отправка).
@AutoConfiguration(afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@ConditionalOnClass(KafkaOperations.class)
@ConditionalOnProperty(prefix = "platform.kafka.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DeadLetterProperties.class)
public class DeadLetterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterAutoConfiguration.class);
    // Партиция -1: Kafka выберет сама, DLT может быть меньше исходного топика
    private static final int ANY_PARTITION = -1;
    private static final String APPLICATION_NAME = "spring.application.name";

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafka, DeadLetterProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka, (record, e) -> {
            // Ключ (paymentId) и место в топике — без тела: в нём персональные данные
            log.error("Сообщение {}-{}@{} (ключ {}) не обработано после повторов, отправлено в DLT: {}",
                    record.topic(), record.partition(), record.offset(), record.key(), rootMessage(e));
            return new TopicPartition(properties.dlt(record.topic(), KafkaUtils.getConsumerGroupId()), ANY_PARTITION);
        });
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(properties.maxRetries());
        backOff.setInitialInterval(properties.initialInterval().toMillis());
        backOff.setMultiplier(properties.multiplier());
        backOff.setMaxInterval(properties.maxInterval().toMillis());
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }

    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    @ConditionalOnProperty(prefix = "platform.kafka.dead-letter", name = "topics[0]")
    public DeadLetterReplay deadLetterReplay(ConsumerFactory<?, ?> consumers, KafkaOperations<?, ?> kafka,
                                             DeadLetterProperties properties, Environment environment) {
        return new DeadLetterReplay(cast(consumers), cast(kafka), properties, environment.getRequiredProperty(APPLICATION_NAME));
    }

    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    @ConditionalOnProperty(prefix = "platform.kafka.dead-letter", name = "topics[0]")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public DeadLetterController deadLetterController(DeadLetterReplay replay, DeadLetterProperties properties) {
        return new DeadLetterController(replay, properties);
    }

    // Читатели у нас получают строки (JSON), ключ и тело отправляем как есть
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // Причина без тела сообщения: у ошибки разбора JSON в тексте — кусок самого сообщения
    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root instanceof JacksonException json ? json.getOriginalMessage() : root.getMessage();
        return root.getClass().getSimpleName() + ": " + message;
    }
}
