package com.flashlearn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Запрос на вход в систему
 */
@Data
public class LoginRequest {

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
