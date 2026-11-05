package com.paymentsapp.platform.error;

import org.springframework.http.HttpStatus;

// Отказы первого уровня — до бизнес-логики: запрос не того формата. Общие для всех сервисов
// (InputErrorHandler), бизнес-отказы — enum'ы ошибок самих сервисов.
public enum InputError implements ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Проверьте данные запроса"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Не удалось прочитать запрос: неверный JSON или тип поля"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Неверный или отсутствующий параметр запроса: %s"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Адрес не найден"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Метод не поддерживается"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Неподдерживаемый формат тела запроса"),
    REQUEST_REJECTED(HttpStatus.BAD_REQUEST, "Запрос отклонён"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервиса, попробуйте позже");

    private final HttpStatus status;
    private final String template;

    InputError(HttpStatus status, String template) {
        this.status = status;
        this.template = template;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String template() {
        return template;
    }
}
