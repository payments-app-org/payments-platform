package com.paymentsapp.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    @SuppressWarnings("unchecked")
    void tagsEveryMeterWithApplicationName() {
        runner.withPropertyValues("spring.application.name=payment").run(context -> {
            MeterRegistry registry = new SimpleMeterRegistry();
            context.getBean(MeterRegistryCustomizer.class).customize(registry);

            registry.counter("payments.created").increment();

            assertThat(registry.get("payments.created").counter().getId().getTag("application"))
                    .isEqualTo("payment");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void noTagWithoutApplicationName() {
        runner.run(context -> {
            MeterRegistry registry = new SimpleMeterRegistry();
            context.getBean(MeterRegistryCustomizer.class).customize(registry);

            registry.counter("payments.created").increment();

            assertThat(registry.get("payments.created").counter().getId().getTag("application")).isNull();
        });
    }

    @Test
    void skipsActuatorRequestsButKeepsApiRequests() {
        runner.run(context -> {
            ObservationPredicate predicate = context.getBean(ObservationPredicate.class);

            assertThat(predicate.test("http.server.requests", request("/actuator/health/readiness"))).isFalse();
            assertThat(predicate.test("http.server.requests", request("/actuator/prometheus"))).isFalse();
            assertThat(predicate.test("http.server.requests", request("/api/payments"))).isTrue();
            assertThat(predicate.test("payment.events", new Observation.Context())).isTrue();
        });
    }

    private static ServerRequestObservationContext request(String uri) {
        return new ServerRequestObservationContext(new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
    }
}
