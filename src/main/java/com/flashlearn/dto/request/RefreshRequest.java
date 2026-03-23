package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Запрос на обновление access токена
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refresh токен обязателен")
    private String refreshToken;
}
