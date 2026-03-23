package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Запрос на смену пароля текущего пользователя
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Текущий пароль обязателен")
    private String currentPassword;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 8, max = 100, message = "Новый пароль должен содержать от 8 до 100 символов")
    private String newPassword;
}
