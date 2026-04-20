package com.flashlearn.service;

public interface AiAbuseProtectionService {
    /**
     * Проверка лимитов генерации AI для пользователя
     */
    void validateGenerationAllowed(Long userId);
}
