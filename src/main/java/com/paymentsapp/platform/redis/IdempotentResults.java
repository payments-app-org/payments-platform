package com.paymentsapp.platform.redis;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

// Итог операции по ключу идемпотентности в Redis с TTL: первый записанный побеждает (SET NX), повтор получает его же.
// Для заглушек внешних систем (процессинг, шлюзы поставщиков): переживает рестарт и один на все поды.
public class IdempotentResults<T> {

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final String prefix;
    private final Duration ttl;
    private final Class<T> type;

    public IdempotentResults(StringRedisTemplate redis, JsonMapper json, String prefix, Duration ttl, Class<T> type) {
        this.redis = redis;
        this.json = json;
        this.prefix = prefix;
        this.ttl = ttl;
        this.type = type;
    }

    // Сохраняет result, если по ключу ещё ничего нет; иначе возвращает сохранённый раньше
    public T putIfAbsent(Object id, T result) {
        Boolean stored = redis.opsForValue().setIfAbsent(key(id), json.writeValueAsString(result), ttl);
        return Boolean.TRUE.equals(stored) ? result : find(id).orElse(result);
    }

    public Optional<T> find(Object id) {
        return Optional.ofNullable(redis.opsForValue().get(key(id))).map(value -> json.readValue(value, type));
    }

    private String key(Object id) {
        return prefix + id;
    }
}
