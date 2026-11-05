package com.paymentsapp.platform.error;

import org.springframework.http.ProblemDetail;

// Единый формат ответа об ошибке: RFC 9457 ProblemDetail + поле code — по нему клиент отличает причины
public final class Problems {

    public static final String CODE_PROPERTY = "code";

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty(CODE_PROPERTY, code.name());
        return problem;
    }

    public static ProblemDetail of(DomainException e) {
        return of(e.getCode(), e.getMessage());
    }
}
