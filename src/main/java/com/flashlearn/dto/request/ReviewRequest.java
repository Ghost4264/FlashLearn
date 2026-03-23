package com.flashlearn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Запрос на оценку карточки после повторения.
 * Качество ответа по шкале SM-2: от 0 (полный провал) до 5 (идеально)
 */
@Data
public class ReviewRequest {

    @NotNull(message = "id карточки обязателен")
    private Long cardId;

    /**
     * Оценка качества ответа по шкале SM-2:
     * 0 — полный провал
     * 1 — неправильно, но ответ знакомый
     * 2 — неправильно, но вспомнил при подсказке
     * 3 — правильно, но с трудом
     * 4 — правильно с небольшой паузой
     * 5 — идеально, без раздумий
     */
    @Min(value = 0, message = "Минимальная оценка — 0")
    @Max(value = 5, message = "Максимальная оценка — 5")
    private int quality;
}
