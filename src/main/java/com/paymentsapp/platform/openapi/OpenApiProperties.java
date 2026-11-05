package com.paymentsapp.platform.openapi;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Описание API сервиса в Swagger. errorCodes — enum кодов ошибок сервиса (реализуют ErrorCode): их коды и тексты
// попадут в схему Problem, чтобы клиент видел, какие отказы возможны.
@ConfigurationProperties("platform.openapi")
public record OpenApiProperties(
        String description,
        @DefaultValue("0.1.0") String version,
        List<Class<?>> errorCodes
) {
}
