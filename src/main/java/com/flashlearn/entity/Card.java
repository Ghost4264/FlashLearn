package com.flashlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Сущность карточки для интервального повторения.
 * Одна карточка содержит вопрос (front) и ответ (back).
 * Принадлежит одной колоде (Deck).
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    /**
     * Уникальный идентификатор карточки
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Колода к которой принадлежит карточка
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    /**
     * Лицевая сторона карточки — вопрос или термин
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String front;

    /**
     * Обратная сторона карточки — ответ или определение
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String back;

    /**
     * Подсказка к карточке
     * Необязательное поле — может быть null
     */
    @Column(columnDefinition = "TEXT")
    private String hint;

    /**
     * Порядковый номер карточки внутри колоды
     */
    @Column(nullable = false)
    private int position;

    /**
     * Дата и время создания карточки
     * Устанавливается один раз через @PrePersist
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего изменения карточки
     * Обновляется через @PreUpdate
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
