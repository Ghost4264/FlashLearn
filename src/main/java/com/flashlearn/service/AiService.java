package com.flashlearn.service;

import com.flashlearn.dto.request.AiGenerateCardsRequest;
import com.flashlearn.dto.response.AiGenerateCardsResponse;

public interface AiService {
    /**
     * Генерирует черновики карточек из текста пользователя.
     */
    AiGenerateCardsResponse generateCards(AiGenerateCardsRequest request, Long userId);
}
