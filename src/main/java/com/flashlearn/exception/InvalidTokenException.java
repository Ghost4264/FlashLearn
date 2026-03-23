package com.flashlearn.exception;

/**
 * Выбрасывается когда JWT или refresh токен невалиден, истёк или отозван
 * Приводит к ответу HTTP 401 Unauthorized
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
