package com.paymentsapp.platform.error;

// Отказ по правилам домена: код + текст для пользователя. В ответ уходит ProblemDetail с полем code.
public class DomainException extends RuntimeException {

    private final transient ErrorCode code;

    public DomainException(ErrorCode code, Object... args) {
        this(code, code.format(args), null);
    }

    private DomainException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // Текст пришёл от соседнего сервиса (например, отказ biller) — передаём его пользователю как есть
    public static DomainException withMessage(ErrorCode code, String message) {
        return new DomainException(code, message, null);
    }

    public static DomainException withCause(ErrorCode code, Throwable cause, Object... args) {
        return new DomainException(code, code.format(args), cause);
    }

    public ErrorCode getCode() {
        return code;
    }
}
