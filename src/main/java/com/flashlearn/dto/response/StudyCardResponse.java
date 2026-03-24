package com.flashlearn.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Карточка для сессии изучения
 * Содержит стандартные поля карточки и флаг isNew —
 * true если у пользователя ещё нет review_progress для этой карточки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyCardResponse {

    private Long id;
    private Long deckId;
    private String front;
    private String back;
    private String hint;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * true — карточка изучается впервые (нет review_progress)
     * false — карточка уже встречалась и пришла на повторение
     */
    @JsonProperty("isNew")
    private boolean isNew;
}
