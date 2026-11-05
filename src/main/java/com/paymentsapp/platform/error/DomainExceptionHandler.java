package com.paymentsapp.platform.error;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Второй уровень — бизнес-отказы сервиса (DomainException). Порядок: свои @ExceptionHandler сервиса и этот —
// раньше InputErrorHandler, у которого есть «последний рубеж» на любое исключение
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DomainExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail domainRejected(DomainException e) {
        return Problems.of(e);
    }
}
