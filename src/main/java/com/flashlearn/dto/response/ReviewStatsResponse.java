package com.flashlearn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Агрегированная статистика повторений пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    /**
     * Сколько карточек оценено сегодня 
     */
    private long reviewedToday;

    /**
     * Сколько карточек оценено с начала текущей календарной недели 
     */
    private long reviewedThisWeek;

    /**
     * Текущая серия дней подряд с хотя бы одним повторением 
     */
    private int streakDays;
}
