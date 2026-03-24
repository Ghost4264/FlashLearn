package com.flashlearn.repository;

import com.flashlearn.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    Page<Deck> findAllByUserId(Long userId, Pageable pageable);

    Page<Deck> findAllByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    Page<Deck> findAllByIsPublicTrue(Pageable pageable);

    Optional<Deck> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Поиск по колодам пользователя с опциональной фильтрацией по категории и строке поиска
     */
    @Query("""
            SELECT d FROM Deck d
            WHERE d.user.id = :userId
              AND d.isPublic = false
              AND (:categoryId IS NULL OR d.category.id = :categoryId)
              AND (COALESCE(:q, '') = ''
                   OR LOWER(d.title) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%'))
                   OR LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')))
            """)
    Page<Deck> findAllByUserIdFiltered(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Поиск по публичным колодам с опциональной строкой поиска
     */
    @Query("""
            SELECT d FROM Deck d
            WHERE d.isPublic = true
              AND (COALESCE(:q, '') = ''
                   OR LOWER(d.title) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%'))
                   OR LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')))
            """)
    Page<Deck> findAllPublicFiltered(@Param("q") String q, Pageable pageable);
}
