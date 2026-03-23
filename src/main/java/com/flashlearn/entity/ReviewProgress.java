package com.flashlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Прогресс повторения конкретной карточки конкретным пользователем
 */
@Entity
@Table(name = "review_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь которому принадлежит этот прогресс
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Карточка для которой отслеживается прогресс
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /**
     * SM-2: коэффициент лёгкости карточки
     * Начинается с 2.5, минимум 1.3
     */
    @Column(name = "ease_factor", nullable = false)
    private double easeFactor;

    /**
     * SM-2: интервал в днях до следующего повторения
     * Начинается с 1, растёт с каждым успешным повторением
     */
    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    /**
     * SM-2: количество успешных повторений подряд
     * Сбрасывается в 0 при неправильном ответе
     */
    @Column(nullable = false)
    private int repetitions;

    /**
     * Дата и время следующего запланированного повторения
     */
    @Column(name = "next_review_at", nullable = false)
    private LocalDateTime nextReviewAt;

    /**
     * Дата и время последнего повторения.
     * Null если карточка ещё не повторялась.
     */
    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;
}
