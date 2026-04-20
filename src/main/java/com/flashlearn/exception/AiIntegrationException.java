package com.flashlearn.exception;

import org.springframework.http.HttpStatus;

/**
 * Ошибка интеграции с AI-провайдером
 */
public class AiIntegrationException extends RuntimeException {
    private final HttpStatus status;

    public AiIntegrationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
