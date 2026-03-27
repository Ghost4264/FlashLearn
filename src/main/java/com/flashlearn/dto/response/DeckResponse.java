package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ответ с данными колоды карточек
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckResponse {

    /**
     * Уникальный идентификатор колоды
     */
    private Long id;

    /**
     * Название колоды
     */
    private String title;

    /**
     * Описание колоды
     */
    private String description;

    /**
     * Флаг публичности
     */
    private boolean isPublic;

    /**
     * Кол-во карточек в этой колоде
     */
    private int cardCount;

    /**
     * Количество карточек которые пора повторить прямо сейчас
     */
    private long dueCardCount;

    /**
     * Дата и время создания колоды
     */
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего изменения
     */
    private LocalDateTime updatedAt;

    /**
     * ID категории (null — без категории)
     */
    private Long categoryId;

    /**
     * Название категории (null — без категории)
     */
    private String categoryName;

    /**
     * ID публичной колоды-источника, если эта колода была склонирована (null иначе)
     */
    private Long clonedFromId;

    /**
     * true — текущий пользователь уже склонировал эту публичную колоду.
     * Заполняется только в списке публичных колод для авторизованного пользователя.
     */
    private boolean alreadyCloned;
}
