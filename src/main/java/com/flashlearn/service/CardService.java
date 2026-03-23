package com.flashlearn.service;

import com.flashlearn.dto.request.CardRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CardService {

    /**
     * Получить карточки колоды постранично, отсортированные по позиции
     */
    PageResponse<CardResponse> getByDeckId(Long deckId, Long userId, Pageable pageable);

    /**
     * Создать новую карточку в колоде
     */
    CardResponse create(Long deckId, CardRequest request, Long userId);

    /**
     * Обновить карточку
     */
    CardResponse update(Long cardId, CardRequest request, Long userId);

    /**
     * Удалить карточку
     */
    void delete(Long cardId, Long userId);
}
