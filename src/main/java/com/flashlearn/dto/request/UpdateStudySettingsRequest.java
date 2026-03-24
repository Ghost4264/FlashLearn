package com.flashlearn.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStudySettingsRequest {

    @NotNull(message = "newCardsPerSession обязателен")
    @Min(value = 1, message = "Минимум 1 новая карточка за сессию")
    @Max(value = 100, message = "Максимум 100 новых карточек за сессию")
    private Integer newCardsPerSession;

    @NotNull(message = "intervalModifier обязателен")
    @DecimalMin(value = "0.5", message = "Минимум 0.5")
    @DecimalMax(value = "2.0", message = "Максимум 2.0")
    private Double intervalModifier;
}
