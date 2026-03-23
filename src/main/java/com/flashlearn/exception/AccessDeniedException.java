package com.flashlearn.exception;

/**
 * Выбрасывается когда пользователь пытается получить доступ к чужому ресурсу
 * Приводит к ответу HTTP 403 Forbidden
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
