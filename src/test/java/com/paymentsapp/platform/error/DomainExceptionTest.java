package com.paymentsapp.platform.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class DomainExceptionTest {

    enum TestError implements ErrorCode {
        LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Превышен лимит: %s"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "Не найдено");

        private final HttpStatus status;
        private final String template;

        TestError(HttpStatus status, String template) {
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

    @Test
    void messageIsFormattedFromTemplate() {
        DomainException e = new DomainException(TestError.LIMIT_EXCEEDED, "50 000 ₽");
        assertThat(e.getMessage()).isEqualTo("Превышен лимит: 50 000 ₽");
        assertThat(e.getCode()).isEqualTo(TestError.LIMIT_EXCEEDED);
    }

    @Test
    void problemCarriesStatusDetailAndCode() {
        ProblemDetail problem = Problems.of(new DomainException(TestError.NOT_FOUND));
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Не найдено");
        assertThat(problem.getProperties()).containsEntry(Problems.CODE_PROPERTY, "NOT_FOUND");
    }

    @Test
    void upstreamMessageIsKept() {
        DomainException e = DomainException.withMessage(TestError.NOT_FOUND, "Поставщик не найден");
        assertThat(e.getMessage()).isEqualTo("Поставщик не найден");
    }
}
