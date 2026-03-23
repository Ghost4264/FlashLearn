package com.flashlearn.service;

import com.flashlearn.dto.request.LoginRequest;
import com.flashlearn.dto.request.RegisterRequest;
import com.flashlearn.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Регистрация нового пользователя. Возвращает пару access/refresh токенов
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Вход в систему. Возвращает пару access/refresh токенов
     */
    AuthResponse login(LoginRequest request);

    /**
     * Обновление access токена по refresh токену
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Выход из системы — отзывает все refresh токены пользователя
     */
    void logout(Long userId);
}
