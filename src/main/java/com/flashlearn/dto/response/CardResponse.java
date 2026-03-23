package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ответ с данными карточки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    /**
     * Уникальный идентификатор карточки
     */
    private Long id;

    /**
     * Идентификатор колоды к которой принадлежит карточка
     */
    private Long deckId;

    /**
     * Лицевая сторона карточки
     */
    private String front;

    /**
     * Обратная сторона карточки
     */
    private String back;

    /**
     * Подсказка к карточке
     */
    private String hint;

    /**
     * Порядковый номер карточки внутри колоды
     */
    private int position;

    /**
     * Дата и время создания карточки
     */
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего изменения карточки
     */
    private LocalDateTime updatedAt;
}
