package com.flashlearn.service;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    /**
     * Получить карточки которые пора повторить сегодня
     */
    List<CardResponse> getDueCards(Long userId);

    /**
     * Получить количество карточек к повторению
     */
    long getDueCount(Long userId);

    /**
     * Оценить карточку после повторения
     */
    ReviewResponse submitReview(ReviewRequest request, Long userId);
}
