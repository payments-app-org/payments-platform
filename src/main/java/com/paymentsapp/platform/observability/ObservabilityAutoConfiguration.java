package com.paymentsapp.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.StringUtils;

/**
 * Метки метрик и что не наблюдать.
 * <ul>
 *   <li>Метка application = spring.application.name на всех метриках сервиса. По ней готовые графики Grafana
 *       (JVM, Spring Boot) выбирают сервис, а запросы Prometheus отличают сервисы друг от друга.</li>
 *   <li>Запросы к /actuator/** (пробы Kubernetes раз в 5–10 с, сбор метрик Prometheus) — без трасс и без метрик
 *       запросов: иначе в Jaeger их больше, чем настоящих запросов, а время ответа сервиса смешано с проверками.</li>
 * </ul>
 */
@AutoConfiguration
public class ObservabilityAutoConfiguration {

    static final String ACTUATOR_PREFIX = "/actuator";

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> applicationTag(
            @Value("${spring.application.name:}") String application) {
        return registry -> {
            if (StringUtils.hasText(application)) {
                registry.config().commonTags("application", application);
            }
        };
    }

    // Только для сервисов на Spring MVC (сервлеты)
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    static class ServletObservations {

        @Bean
        public ObservationPredicate skipActuatorRequests() {
            return (name, context) -> !(context instanceof ServerRequestObservationContext request
                    && request.getCarrier() != null
                    && request.getCarrier().getRequestURI().startsWith(ACTUATOR_PREFIX));
        }
    }
}
