package com.paymentsapp.platform.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

// Включается у сервисов с resilience4j-spring-boot4: реестры настроены из application.yml и отдают метрики
@AutoConfiguration
@ConditionalOnClass({CircuitBreakerRegistry.class, RetryRegistry.class, BulkheadRegistry.class})
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResilientCalls resilientCalls(CircuitBreakerRegistry breakers, RetryRegistry retries, BulkheadRegistry bulkheads) {
        return new ResilientCalls(breakers, retries, bulkheads);
    }
}
