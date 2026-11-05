package com.paymentsapp.platform.cache;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

// @Cacheable поверх Redis (platform.cache.enabled=true). Ошибка Redis — не ошибка запроса: CacheErrorHandler
// пишет в лог, вызов идёт мимо кэша. null не кэшируем (Optional.empty — тоже): «нет данных» не должно залипать.
@AutoConfiguration(afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@ConditionalOnClass(RedisCacheManager.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "platform.cache", name = "enabled", havingValue = "true")
@EnableCaching
// Инфраструктурный бин: CachingConfigurer создаётся рано, его не нужно обрабатывать прочими BeanPostProcessor (наблюдение и т. п.)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration implements CachingConfigurer {

    private static final String APPLICATION_NAME = "spring.application.name";
    private static final String SEPARATOR = ":";
    private static final boolean LOG_STACK_TRACES = false;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redis, CacheProperties properties, Environment environment) {
        String prefix = environment.getRequiredProperty(APPLICATION_NAME) + SEPARATOR + properties.version() + SEPARATOR;
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.defaultTtl())
                .disableCachingNullValues()
                .computePrefixWith(cache -> prefix + cache + SEPARATOR);
        Map<String, RedisCacheConfiguration> perCache = properties.ttl() == null ? Map.of()
                : properties.ttl().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> defaults.entryTtl(e.getValue())));
        return RedisCacheManager.builder(redis).cacheDefaults(defaults).withInitialCacheConfigurations(perCache).build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(LOG_STACK_TRACES);
    }
}
