package com.flashlearn.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сущность колоды карточек
 */
@Entity
@Table(name = "decks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deck {

    /**
     * Уникальный идентификатор колоды
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Владелец колоды
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Название колоды, например "Английский язык" или "Математика"
     */
    @Column(nullable = false)
    private String title;

    /**
     * Описание колоды
     * Необязательное поле — может быть null
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Флаг публичности
     * Если true — колода видна другим пользователям
     */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    /**
     * Дата и время создания колоды
     * Устанавливается один раз через @PrePersist.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего изменения
     * Обновляется через @PreUpdate
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Категория колоды (обязательна): Программирование, Языки, Математика и т.д.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * ID исходной публичной колоды, если эта колода была склонирована
     * null — если колода создана вручную
     */
    @Column(name = "cloned_from_id")
    private Long clonedFromId;

    /**
     * Карточки в этой колоде
     * orphanRemoval — карточка удалённая из списка удаляется из БД
     */
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards;

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
