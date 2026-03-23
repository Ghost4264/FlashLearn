package com.flashlearn.exception;

/**
 * Выбрасывается когда запрошенный ресурс не найден в БД
 * Приводит к ответу HTTP 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Long id) {
        return new ResourceNotFoundException(entity + " с id=" + id + " не найден");
    }
}
