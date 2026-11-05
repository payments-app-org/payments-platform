package com.paymentsapp.platform.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;

// Разбор DLT группы: сколько сообщений ждёт и повторная отправка в исходный топик — после того как причину сбоя
// устранили (исправили код, подняли базу). Где мы остановились, хранит своя группа <сервис>-dlt-replay: повтор
// не отправит одно сообщение дважды. Отправляем в исходный топик — значит, его снова получат и другие группы-читатели,
// которые сообщение уже обработали: поэтому читатели обязаны быть идемпотентными (у нас — уникальные индексы по paymentId).
public class DeadLetterReplay {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterReplay.class);
    private static final Duration POLL = Duration.ofSeconds(1);
    private static final int MAX_EMPTY_POLLS = 5;

    private final ConsumerFactory<Object, Object> consumers;
    private final KafkaOperations<Object, Object> kafka;
    private final DeadLetterProperties properties;
    private final String group;

    public DeadLetterReplay(ConsumerFactory<Object, Object> consumers, KafkaOperations<Object, Object> kafka,
                            DeadLetterProperties properties, String applicationName) {
        this.consumers = consumers;
        this.kafka = kafka;
        this.properties = properties;
        this.group = applicationName + properties.replayGroupSuffix();
    }

    // Сколько сообщений в DLT группы ещё не отправлено повторно
    public long pending(String topic, String group) {
        try (Consumer<Object, Object> consumer = consumer()) {
            List<TopicPartition> partitions = assign(consumer, properties.dlt(topic, group));
            Map<TopicPartition, Long> end = consumer.endOffsets(partitions);
            return partitions.stream().mapToLong(p -> end.get(p) - consumer.position(p)).sum();
        }
    }

    // Отправляет в исходный топик всё, что лежало в DLT группы на момент вызова
    public int replay(String topic, String group) {
        String dlt = properties.dlt(topic, group);
        try (Consumer<Object, Object> consumer = consumer()) {
            List<TopicPartition> partitions = assign(consumer, dlt);
            Map<TopicPartition, Long> end = consumer.endOffsets(partitions);
            Map<TopicPartition, OffsetAndMetadata> done = new HashMap<>();
            int sent = 0;
            int empty = 0;
            while (!reached(consumer, partitions, end) && empty < MAX_EMPTY_POLLS) {
                var records = consumer.poll(POLL);
                empty = records.isEmpty() ? empty + 1 : 0;
                for (ConsumerRecord<Object, Object> r : records) {
                    TopicPartition p = new TopicPartition(r.topic(), r.partition());
                    if (r.offset() >= end.get(p)) {
                        continue;
                    }
                    kafka.send(new ProducerRecord<>(topic, null, r.key(), r.value())).join();
                    done.put(p, new OffsetAndMetadata(r.offset() + 1));
                    sent++;
                }
                // Фиксируем после каждой пачки: упадём посреди — повтор продолжит отсюда
                consumer.commitSync(done);
            }
            log.info("DLT {}: отправлено повторно в {} сообщений: {}", dlt, topic, sent);
            return sent;
        }
    }

    private Consumer<Object, Object> consumer() {
        Properties overrides = new Properties();
        overrides.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return consumers.createConsumer(group, null, null, overrides);
    }

    // Читаем DLT с места, где остановился прошлый повтор (или с начала)
    private List<TopicPartition> assign(Consumer<Object, Object> consumer, String dlt) {
        var infos = consumer.partitionsFor(dlt);
        List<TopicPartition> partitions = infos == null ? List.of()
                : infos.stream().map(i -> new TopicPartition(dlt, i.partition())).toList();
        consumer.assign(partitions);
        Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(Set.copyOf(partitions));
        for (TopicPartition p : partitions) {
            OffsetAndMetadata at = committed.get(p);
            if (at == null) {
                consumer.seekToBeginning(List.of(p));
            } else {
                consumer.seek(p, at.offset());
            }
        }
        return partitions;
    }

    private static boolean reached(Consumer<Object, Object> consumer, List<TopicPartition> partitions,
                                   Map<TopicPartition, Long> end) {
        return partitions.stream().allMatch(p -> consumer.position(p) >= end.get(p));
    }
}
