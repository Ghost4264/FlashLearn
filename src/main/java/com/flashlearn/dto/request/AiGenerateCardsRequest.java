package com.flashlearn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Запрос на генерацию карточек из произвольного текста
 */
@Data
public class AiGenerateCardsRequest {

    @NotBlank(message = "Текст для генерации обязателен")
    private String sourceText;

    @Min(value = 1, message = "Количество карточек должно быть не меньше 1")
    @Max(value = 30, message = "Количество карточек должно быть не больше 30")
    private Integer desiredCount;
}
