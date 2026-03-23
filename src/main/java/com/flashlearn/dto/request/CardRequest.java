package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Запрос на создание или обновление карточки
 */
@Data
public class CardRequest {

    @NotBlank(message = "Лицевая сторона карточки обязательна")
    private String front;

    @NotBlank(message = "Обратная сторона карточки обязательна")
    private String back;

    /**
     * Подсказка необязательна
     */
    private String hint;

    private int position;
}
