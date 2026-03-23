package com.flashlearn.exception;

/**
 * Выбрасывается когда текущий пароль при смене не совпадает
 * Приводит к ответу HTTP 400 Bad Request
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Текущий пароль указан неверно");
    }
}
