package com.paymentsapp.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
}
