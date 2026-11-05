package com.paymentsapp.platform.error;

import org.springframework.http.HttpStatus;

// Код ошибки домена. Реализуется enum'ом в каждом сервисе: статус — HTTP, template — текст для пользователя
// (параметры — как в String.format), code — что видит клиент в поле code. Обычно code = имя константы;
// несколько констант с разными текстами могут отдавать один общий код (контракт с вызывающими сервисами).
public interface ErrorCode {

    String name();

    default String code() {
        return name();
    }

    HttpStatus status();

    String template();

    default String format(Object... args) {
        return args.length == 0 ? template() : template().formatted(args);
    }
}
