package com.paymentsapp.platform.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class ResilientCallsTest {

    private static final int ATTEMPTS = 3;
    private static final int WINDOW = 4;

    private final CircuitBreakerRegistry breakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
            .slidingWindowSize(WINDOW).minimumNumberOfCalls(WINDOW).failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1)).build());
    private final RetryRegistry retries = RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(ATTEMPTS).waitDuration(Duration.ofMillis(1))
            .retryExceptions(ResourceAccessException.class).build());
    private final ResilientCalls calls = new ResilientCalls(breakers, retries, BulkheadRegistry.ofDefaults());

    @Test
    void retriesTransientFailureAndReturnsResult() {
        AtomicInteger attempts = new AtomicInteger();
        String result = calls.call("account", () -> {
            if (attempts.incrementAndGet() < ATTEMPTS) {
                throw new ResourceAccessException("connection refused");
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(ATTEMPTS);
    }

    @Test
    void openCircuitFailsFastAsServiceUnavailable() {
        breakers.circuitBreaker("fraud").transitionToOpenState();
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> calls.call("fraud", attempts::incrementAndGet))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(attempts).hasValue(0);
    }

    @Test
    void connectFailureDistinguishesRefusedFromReadTimeout() {
        ConnectFailure predicate = new ConnectFailure();
        assertThat(predicate.test(new ResourceAccessException("refused", new ConnectException("refused")))).isTrue();
        assertThat(predicate.test(new ResourceAccessException("timeout", new SocketTimeoutException("read")))).isFalse();
    }
}
