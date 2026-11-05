package com.paymentsapp.platform.openapi;

import com.paymentsapp.platform.error.ErrorCode;
import com.paymentsapp.platform.error.Problems;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

// Swagger у сервиса со springdoc: заголовок — имя сервиса, у каждой операции — ответы 4XX/5XX в формате ProblemDetail
// (application/problem+json) с полем code — список кодов из enum ошибок сервиса (platform.openapi.error-codes).
@AutoConfiguration
@ConditionalOnClass(OpenApiCustomizer.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

    static final String PROBLEM = "Problem";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String CLIENT_ERRORS = "4XX";
    private static final String SERVER_ERRORS = "5XX";
    private static final String SCHEMA_REF = "#/components/schemas/" + PROBLEM;
    private static final String APPLICATION_NAME = "spring.application.name";
    private static final String CODE_LINE = "%s — %s";
    private static final String LINE_BREAK = "\n\n";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI serviceOpenApi(Environment environment, OpenApiProperties props) {
        return new OpenAPI().info(new Info()
                .title(environment.getRequiredProperty(APPLICATION_NAME))
                .description(props.description())
                .version(props.version()));
    }

    @Bean
    public OpenApiCustomizer problemResponses(OpenApiProperties props) {
        Map<String, String> codes = errorCodes(props.errorCodes());
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM, problemSchema(codes));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
                op.getResponses().computeIfAbsent(CLIENT_ERRORS, k -> problemResponse("Отказ: неверный запрос или правило домена"));
                op.getResponses().computeIfAbsent(SERVER_ERRORS, k -> problemResponse("Сбой или недоступный сосед"));
            }));
        };
    }

    private static ApiResponse problemResponse(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(new Schema<>().$ref(SCHEMA_REF))));
    }

    private static Schema<?> problemSchema(Map<String, String> codes) {
        StringSchema code = new StringSchema();
        code.description(codes.isEmpty() ? "Код ошибки для программы"
                : "Код ошибки для программы:" + LINE_BREAK + codes.entrySet().stream()
                .map(e -> CODE_LINE.formatted(e.getKey(), e.getValue())).collect(Collectors.joining(LINE_BREAK)));
        if (!codes.isEmpty()) {
            code.setEnum(List.copyOf(codes.keySet()));
        }
        return new ObjectSchema()
                .description("Отказ в формате RFC 9457 (ProblemDetail): detail — текст для пользователя, code — для программы")
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty(Problems.CODE_PROPERTY, code);
    }

    // Код → текст; у нескольких констант может быть один код — берём первый текст
    private static Map<String, String> errorCodes(List<Class<?>> enums) {
        Map<String, String> codes = new LinkedHashMap<>();
        if (enums == null) {
            return codes;
        }
        for (Class<?> type : enums) {
            if (!type.isEnum() || !ErrorCode.class.isAssignableFrom(type)) {
                throw new IllegalStateException("platform.openapi.error-codes: " + type.getName() + " — не enum ErrorCode");
            }
            Arrays.stream(type.getEnumConstants()).map(ErrorCode.class::cast)
                    .forEach(c -> codes.putIfAbsent(c.code(), c.template()));
        }
        return codes;
    }
}
