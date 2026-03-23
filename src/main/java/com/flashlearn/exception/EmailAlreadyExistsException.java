package com.flashlearn.exception;

/**
 * Выбрасывается при попытке зарегистрироваться с уже занятым email
 * Приводит к ответу HTTP 409 Conflict
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email уже зарегистрирован: " + email);
    }
}
