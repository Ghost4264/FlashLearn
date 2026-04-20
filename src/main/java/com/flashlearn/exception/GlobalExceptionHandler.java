package com.flashlearn.exception;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 — запрошенный ресурс не найден в БД
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * 403 — попытка обратиться к чужому ресурсу
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * 401 — невалидный, истёкший или отозванный токен
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * 409 — попытка зарегистрироваться с уже занятым email
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailConflict(EmailAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 401 — неверный email или пароль при входе
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
    }

    /**
     * 400 — текущий пароль при смене указан неверно
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 400 — передано несуществующее поле в параметре sort
     * Пример: GET /api/decks?sort=badField
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSortField(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Недопустимое поле сортировки: '" + ex.getPropertyName() + "'"
        );
    }

    /**
     * 400 — параметр запроса имеет неверный тип
     * Пример: GET /api/decks?page=abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Некорректное значение параметра '" + ex.getName() + "': " + ex.getValue()
        );
    }

    /**
     * 400 — ошибки валидации полей
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "некорректное значение",
                        (first, second) -> first
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Ошибка валидации входных данных"
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Ошибки интеграции с AI-провайдером
     */
    @ExceptionHandler(AiIntegrationException.class)
    public ProblemDetail handleAiIntegration(AiIntegrationException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }
}
