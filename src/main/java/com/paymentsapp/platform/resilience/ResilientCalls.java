package com.paymentsapp.platform.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;

// Вызов соседа: ограничение одновременных вызовов (bulkhead) → circuit breaker → повторы (retry снаружи).
// Имена экземпляров и их настройки — в application.yml (resilience4j.*). По умолчанию retry называется как сосед;
// для вызова с другой политикой повторов (например, только при ошибке соединения) передаётся своё имя retry.
public class ResilientCalls {

    private final CircuitBreakerRegistry breakers;
    private final RetryRegistry retries;
    private final BulkheadRegistry bulkheads;

    public ResilientCalls(CircuitBreakerRegistry breakers, RetryRegistry retries, BulkheadRegistry bulkheads) {
        this.breakers = breakers;
        this.retries = retries;
        this.bulkheads = bulkheads;
    }

    public <T> T call(String backend, Supplier<T> call) {
        return call(backend, backend, call);
    }

    public <T> T call(String backend, String retryPolicy, Supplier<T> call) {
        Supplier<T> limited = Bulkhead.decorateSupplier(bulkheads.bulkhead(backend), call);
        Supplier<T> guarded = CircuitBreaker.decorateSupplier(breakers.circuitBreaker(backend), limited);
        Supplier<T> retried = Retry.decorateSupplier(retries.retry(retryPolicy), guarded);
        try {
            return retried.get();
        } catch (CallNotPermittedException | BulkheadFullException e) {
            throw new ServiceUnavailableException(backend, e);
        }
    }

    public void run(String backend, Runnable call) {
        run(backend, backend, call);
    }

    public void run(String backend, String retryPolicy, Runnable call) {
        call(backend, retryPolicy, () -> {
            call.run();
            return null;
        });
    }
}
