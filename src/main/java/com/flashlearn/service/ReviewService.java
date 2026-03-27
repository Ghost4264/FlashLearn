package com.flashlearn.service;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.ReviewResponse;
import com.flashlearn.dto.response.ReviewStatsResponse;
import com.flashlearn.dto.response.StudyCardResponse;

import java.util.List;

public interface ReviewService {

    /**
     * Получить карточки которые пора повторить сегодня (все колоды)
     */
    List<StudyCardResponse> getDueCards(Long userId);

    /**
     * Получить карточки к повторению из конкретной колоды
     */
    List<StudyCardResponse> getDueCardsByDeck(Long userId, Long deckId);

    /**
     * Получить количество карточек к повторению
     */
    long getDueCount(Long userId);

    /**
     * Статистика повторений: сегодня, за текущую неделю, серия дней
     */
    ReviewStatsResponse getStats(Long userId);

    /**
     * Оценить карточку после повторения
     */
    ReviewResponse submitReview(ReviewRequest request, Long userId);
}
