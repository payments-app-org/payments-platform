package com.paymentsapp.platform.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class IdempotentResultsTest {

    private static final Duration TTL = Duration.ofDays(7);
    private static final String PREFIX = "test:";

    record Result(UUID id, String status) {
    }

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final IdempotentResults<Result> results = new IdempotentResults<>(redis, json, PREFIX, TTL, Result.class);
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void firstWriteWins() {
        when(values.setIfAbsent(eq(PREFIX + id), anyString(), eq(TTL))).thenReturn(true);
        Result fresh = new Result(id, "COMPLETED");

        assertThat(results.putIfAbsent(id, fresh)).isSameAs(fresh);
    }

    @Test
    void repeatGetsStoredResult() {
        Result stored = new Result(id, "DECLINED");
        when(values.setIfAbsent(eq(PREFIX + id), anyString(), eq(TTL))).thenReturn(false);
        when(values.get(PREFIX + id)).thenReturn(json.writeValueAsString(stored));

        assertThat(results.putIfAbsent(id, new Result(id, "COMPLETED"))).isEqualTo(stored);
        assertThat(results.find(id)).contains(stored);
    }

    @Test
    void missingKeyIsEmpty() {
        assertThat(results.find(UUID.randomUUID())).isEmpty();
    }
}
