package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Черновик карточки, предложенный AI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCardDraftResponse {
    private String front;
    private String back;
    private String hint;
    private int position;
}
