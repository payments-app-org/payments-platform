package com.paymentsapp.platform.error;

import org.springframework.http.HttpStatus;

// Код ошибки домена. Реализуется enum'ом в каждом сервисе: имя — код для клиента, статус — HTTP, template — текст
// для пользователя (параметры — как в String.format).
public interface ErrorCode {

    String name();

    HttpStatus status();

    String template();

    default String format(Object... args) {
        return args.length == 0 ? template() : template().formatted(args);
    }
}
