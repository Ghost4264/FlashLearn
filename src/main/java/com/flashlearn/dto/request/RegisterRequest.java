package com.flashlearn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Запрос на регистрацию нового пользователя
 */
@Data
public class RegisterRequest {

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Имя не может быть длиннее 100 символов")
    private String name;
}
