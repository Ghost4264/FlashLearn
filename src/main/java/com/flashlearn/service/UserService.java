package com.flashlearn.service;

import com.flashlearn.dto.request.ChangePasswordRequest;
import com.flashlearn.dto.request.UpdateUserRequest;
import com.flashlearn.dto.response.UserResponse;

public interface UserService {

    /**
     * Получить профиль текущего пользователя
     */
    UserResponse getMe(Long userId);

    /**
     * Обновить имя пользователя
     */
    UserResponse updateMe(Long userId, UpdateUserRequest request);

    /**
     * Сменить пароль — проверяет текущий пароль перед обновлением
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
