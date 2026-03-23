package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на успешную аутентификацию (регистрацию или вход)
 * Содержит JWT access токен и refresh токен
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Краткосрочный JWT токен для авторизации запросов
     * Срок жизни — 24 часа
     */
    private String accessToken;

    /**
     * Долгосрочный токен для получения нового access токена
     * Срок жизни — 7 дней
     */
    private String refreshToken;

    /**
     * Тип токена
     */
    private String tokenType = "Bearer";
}
