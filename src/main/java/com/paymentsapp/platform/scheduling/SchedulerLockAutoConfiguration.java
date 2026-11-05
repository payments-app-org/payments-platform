package com.paymentsapp.platform.scheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

// @SchedulerLock на задаче планировщика: при нескольких подах её выполняет только один. Блокировка — в Redis.
// После автоконфигурации Redis из Spring Boot: блокировки включаются, только если у сервиса есть Redis
@AutoConfiguration(afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@ConditionalOnClass({RedisLockProvider.class, RedisConnectionFactory.class})
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "platform.scheduling.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableSchedulerLock(defaultLockAtMostFor = "${platform.scheduling.lock.at-most-for:PT10M}")
public class SchedulerLockAutoConfiguration {

    private static final String APPLICATION_NAME = "spring.application.name";

    // Префикс ключей — имя сервиса: блокировки разных сервисов не пересекаются
    @Bean
    @ConditionalOnMissingBean
    public LockProvider lockProvider(RedisConnectionFactory redis, Environment environment) {
        return new RedisLockProvider(redis, environment.getRequiredProperty(APPLICATION_NAME));
    }
}
