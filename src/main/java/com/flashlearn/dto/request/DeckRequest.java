package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Запрос на создание или обновление колоды карточек
 */
@Data
public class DeckRequest {

    @NotBlank(message = "Название колоды обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String title;

    private String description;

    private boolean isPublic;
}
