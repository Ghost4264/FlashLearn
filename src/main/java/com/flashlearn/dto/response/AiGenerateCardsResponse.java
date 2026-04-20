package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ с результатом генерации карточек
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateCardsResponse {
    private String provider;
    private String model;
    private List<AiCardDraftResponse> cards;
}
