package com.paymentsapp.platform.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

// Часы и часовой пояс одним бином: код не вызывает now() без Clock, а тесты подменяют время
@AutoConfiguration
@EnableConfigurationProperties(TimeProperties.class)
public class TimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZoneId businessZone(TimeProperties properties) {
        return properties.zone();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock(ZoneId businessZone) {
        return Clock.system(businessZone);
    }
}
