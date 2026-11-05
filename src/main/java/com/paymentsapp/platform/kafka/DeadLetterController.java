package com.paymentsapp.platform.kafka;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Внутренний API для дежурного: сколько сообщений ждёт в DLT групп сервиса и повторная отправка после исправления
// причины. Только топики, которые сервис читает (platform.kafka.dead-letter.topics), — чужие DLT отсюда не трогаем.
@RestController
@RequestMapping("/api/internal/dlt")
public class DeadLetterController {

    private final DeadLetterReplay replay;
    private final DeadLetterProperties properties;

    public DeadLetterController(DeadLetterReplay replay, DeadLetterProperties properties) {
        this.replay = replay;
        this.properties = properties;
    }

    // Группа → сколько ждёт
    @GetMapping("/{topic}")
    public Map<String, Long> pending(@PathVariable String topic) {
        allowed(topic);
        Map<String, Long> pending = new LinkedHashMap<>();
        properties.groups().forEach(group -> pending.put(group, replay.pending(topic, group)));
        return pending;
    }

    // Группа → сколько отправлено повторно
    @PostMapping("/{topic}/replay")
    public Map<String, Integer> replay(@PathVariable String topic) {
        allowed(topic);
        Map<String, Integer> sent = new LinkedHashMap<>();
        properties.groups().forEach(group -> sent.put(group, replay.replay(topic, group)));
        return sent;
    }

    private void allowed(String topic) {
        if (!properties.topics().contains(topic)) {
            throw new UnknownTopicException(topic);
        }
    }
}
