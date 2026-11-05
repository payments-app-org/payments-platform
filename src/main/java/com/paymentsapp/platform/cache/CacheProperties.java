package com.paymentsapp.platform.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Кэш в Redis, общий для всех подов сервиса. ttl — время жизни по имени кэша, иначе defaultTtl.
// version — в префиксе ключей: поменяли формат кэшируемых данных — подняли версию, старые записи не читаются.
@ConfigurationProperties("platform.cache")
public record CacheProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("1h") Duration defaultTtl,
        Map<String, Duration> ttl,
        @DefaultValue("v1") String version
) {
}
