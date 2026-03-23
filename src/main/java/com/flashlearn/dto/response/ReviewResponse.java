package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ответ после оценки карточки.
 * Показывает обновлённое состояние алгоритма SM-2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long cardId;

    /**
     * Новый интервал в днях до следующего повторения
     */
    private int intervalDays;

    /**
     * Обновлённый коэффициент лёгкости
     */
    private double easeFactor;

    /**
     * Дата и время следующего запланированного повторения
     */
    private LocalDateTime nextReviewAt;
}
