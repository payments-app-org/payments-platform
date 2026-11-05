package com.paymentsapp.platform.error;

import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Первый уровень проверки — формат запроса (Bean Validation на DTO, заголовки, параметры, JSON). Здесь все такие
// ошибки Spring MVC превращаются в один формат: ProblemDetail + code (InputError) + errors — список полей с причиной.
// Второй уровень — бизнес-правила в сервисах (DomainException, DomainExceptionHandler).
// Последний рубеж — непредвиденная ошибка: 500 без подробностей наружу (стек — только в лог).
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class InputErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InputErrorHandler.class);
    static final String ERRORS_PROPERTY = "errors";
    private static final String DETAIL_SEPARATOR = ": ";
    private static final String FIELD_SEPARATOR = " — ";
    private static final String ERRORS_SEPARATOR = "; ";

    // Ограничения на параметрах метода контроллера (@Size на заголовке и т. п.) при @Validated на классе
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraintViolated(ConstraintViolationException e) {
        List<FieldProblem> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldProblem(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .sorted(Comparator.comparing(FieldProblem::field)).toList();
        return withErrors(Problems.of(InputError.VALIDATION_FAILED, InputError.VALIDATION_FAILED.template()), errors);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Необработанная ошибка: {}", e.toString(), e);
        return Problems.of(InputError.INTERNAL_ERROR, InputError.INTERNAL_ERROR.template());
    }

    // Все стандартные исключения Spring MVC проходят здесь: подставляем наш код и понятный текст
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = switch (e) {
            case MethodArgumentNotValidException invalid -> withErrors(problem(InputError.VALIDATION_FAILED),
                    fieldErrors(invalid));
            case HandlerMethodValidationException invalid -> withErrors(problem(InputError.VALIDATION_FAILED),
                    invalid.getParameterValidationResults().stream()
                            .flatMap(r -> r.getResolvableErrors().stream().map(err -> new FieldProblem(
                                    r.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                            .toList());
            case BindException invalid -> withErrors(problem(InputError.VALIDATION_FAILED), fieldErrors(invalid));
            case HttpMessageNotReadableException ignored -> problem(InputError.MALFORMED_REQUEST);
            case MissingRequestHeaderException missing -> problem(InputError.INVALID_PARAMETER, missing.getHeaderName());
            case MissingServletRequestParameterException missing ->
                    problem(InputError.INVALID_PARAMETER, missing.getParameterName());
            case MethodArgumentTypeMismatchException mismatch -> problem(InputError.INVALID_PARAMETER, mismatch.getName());
            case NoResourceFoundException ignored -> problem(InputError.NOT_FOUND);
            case HttpRequestMethodNotSupportedException ignored -> problem(InputError.METHOD_NOT_ALLOWED);
            case HttpMediaTypeNotSupportedException ignored -> problem(InputError.UNSUPPORTED_MEDIA_TYPE);
            default -> {
                ProblemDetail fallback = ProblemDetail.forStatusAndDetail(status, InputError.REQUEST_REJECTED.template());
                fallback.setProperty(Problems.CODE_PROPERTY, InputError.REQUEST_REJECTED.code());
                yield fallback;
            }
        };
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    private static ProblemDetail problem(InputError error, Object... args) {
        return Problems.of(error, error.format(args));
    }

    private static List<FieldProblem> fieldErrors(BindException e) {
        Stream<FieldProblem> fields = e.getFieldErrors().stream().map(f -> new FieldProblem(f.getField(), message(f)));
        Stream<FieldProblem> global = e.getGlobalErrors().stream().map(g -> new FieldProblem(g.getObjectName(),
                g.getDefaultMessage()));
        return Stream.concat(fields, global).sorted(Comparator.comparing(FieldProblem::field)).toList();
    }

    private static String message(FieldError f) {
        return f.getDefaultMessage();
    }

    // В detail — те же причины одной строкой: клиент, который показывает только detail, тоже поймёт, что не так
    private static ProblemDetail withErrors(ProblemDetail problem, List<FieldProblem> errors) {
        problem.setProperty(ERRORS_PROPERTY, errors);
        if (!errors.isEmpty()) {
            problem.setDetail(problem.getDetail() + DETAIL_SEPARATOR + errors.stream()
                    .map(e -> e.field() + FIELD_SEPARATOR + e.message()).collect(Collectors.joining(ERRORS_SEPARATOR)));
        }
        return problem;
    }

    // «create.key» → «key»
    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    // Поле запроса и что с ним не так. Значение поля не возвращаем: там могут быть персональные данные.
    public record FieldProblem(String field, String message) {
    }
}
