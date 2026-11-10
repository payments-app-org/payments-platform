package com.paymentsapp.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Метка application = spring.application.name на всех метриках сервиса. По ней готовые графики Grafana
 * (JVM, Spring Boot) выбирают сервис, а запросы Prometheus отличают сервисы друг от друга.
 */
@AutoConfiguration
public class ObservabilityAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> applicationTag(
            @Value("${spring.application.name:}") String application) {
        return registry -> {
            if (StringUtils.hasText(application)) {
                registry.config().commonTags("application", application);
            }
        };
    }
}
