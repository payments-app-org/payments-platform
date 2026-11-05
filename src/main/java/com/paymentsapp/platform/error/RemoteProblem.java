package com.paymentsapp.platform.error;

import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Отказ соседнего сервиса в формате ProblemDetail: код (поле code) и текст для пользователя (detail).
// Тело не ProblemDetail (прокси, HTML) — кода нет, текст — по умолчанию.
public record RemoteProblem(int status, String code, String detail) {

    private static final String CODE = Problems.CODE_PROPERTY;
    private static final String DETAIL = "detail";

    public static RemoteProblem of(RestClientResponseException e, JsonMapper json, String defaultDetail) {
        try {
            JsonNode body = json.readTree(e.getResponseBodyAsString());
            return new RemoteProblem(e.getStatusCode().value(), body.path(CODE).asString(null),
                    body.path(DETAIL).asString(defaultDetail));
        } catch (JacksonException notJson) {
            return new RemoteProblem(e.getStatusCode().value(), null, defaultDetail);
        }
    }
}
