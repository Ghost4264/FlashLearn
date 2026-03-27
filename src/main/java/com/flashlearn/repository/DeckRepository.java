package com.flashlearn.repository;

import com.flashlearn.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
     * Поиск по публичным колодам с опциональной фильтрацией по категории и строке поиска
     */
    @Query("""
            SELECT d FROM Deck d
            WHERE d.isPublic = true
              AND (:categoryName IS NULL OR d.category.name = :categoryName)
              AND (COALESCE(:q, '') = ''
                   OR LOWER(d.title) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%'))
                   OR LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')))
            """)
    Page<Deck> findAllPublicFiltered(@Param("categoryName") String categoryName, @Param("q") String q, Pageable pageable);

    /**
     * Уникальные имена категорий публичных колод (для фильтра)
     */
    @Query("SELECT DISTINCT d.category.name FROM Deck d WHERE d.isPublic = true AND d.category IS NOT NULL ORDER BY d.category.name")
    List<String> findDistinctCategoryNamesInPublicDecks();

    /**
     * Проверяет, склонировал ли пользователь уже данную публичную колоду
     */
    boolean existsByUserIdAndClonedFromId(Long userId, Long clonedFromId);

    /**
     * Возвращает множество ID публичных колод, которые пользователь уже склонировал
     */
    @Query("SELECT d.clonedFromId FROM Deck d WHERE d.user.id = :userId AND d.clonedFromId IS NOT NULL AND d.clonedFromId IN :publicDeckIds")
    List<Long> findClonedFromIdsByUserIdAndPublicDeckIds(@Param("userId") Long userId, @Param("publicDeckIds") List<Long> publicDeckIds);
}
