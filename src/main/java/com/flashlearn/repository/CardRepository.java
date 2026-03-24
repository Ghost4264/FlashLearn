package com.flashlearn.repository;

import com.flashlearn.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findAllByDeckId(Long deckId, Pageable pageable);

    List<Card> findAllByDeckIdOrderByPosition(Long deckId);

    Optional<Card> findByIdAndDeckId(Long id, Long deckId);

    int countByDeckId(Long deckId);

    void deleteAllByDeckId(Long deckId);

    @Query("""
            SELECT c.deck.id, COUNT(c) FROM Card c
            WHERE c.deck.id IN :deckIds
            GROUP BY c.deck.id
            """)
    List<Object[]> countByDeckIdIn(@Param("deckIds") List<Long> deckIds);

    /**
     * Новые карточки пользователя — те, для которых ещё нет записи review_progress
     */
    @Query("""
            SELECT c FROM Card c
            WHERE c.deck.user.id = :userId
              AND NOT EXISTS (
                  SELECT rp FROM ReviewProgress rp
                  WHERE rp.card = c AND rp.user.id = :userId
              )
            ORDER BY c.createdAt ASC
            """)
    List<Card> findNewCards(@Param("userId") Long userId, Pageable pageable);

    /**
     * Новые карточки конкретной колоды для пользователя
     */
    @Query("""
            SELECT c FROM Card c
            WHERE c.deck.id = :deckId
              AND NOT EXISTS (
                  SELECT rp FROM ReviewProgress rp
                  WHERE rp.card = c AND rp.user.id = :userId
              )
            ORDER BY c.position ASC
            """)
    List<Card> findNewCardsByDeck(@Param("userId") Long userId,
                                  @Param("deckId") Long deckId,
                                  Pageable pageable);

    /**
     * Количество новых карточек пользователя во всех колодах
     */
    @Query("""
            SELECT COUNT(c) FROM Card c
            WHERE c.deck.user.id = :userId
              AND c.deck.isPublic = false
              AND NOT EXISTS (
                  SELECT rp FROM ReviewProgress rp
                  WHERE rp.card = c AND rp.user.id = :userId
              )
            """)
    long countNewCards(@Param("userId") Long userId);

    /**
     * Количество новых карточек в конкретной колоде
     */
    @Query("""
            SELECT COUNT(c) FROM Card c
            WHERE c.deck.id = :deckId
              AND NOT EXISTS (
                  SELECT rp FROM ReviewProgress rp
                  WHERE rp.card = c AND rp.user.id = :userId
              )
            """)
    long countNewCardsByDeck(@Param("userId") Long userId, @Param("deckId") Long deckId);

    /**
     * Количество новых карточек для списка колод (bulk)
     */
    @Query("""
            SELECT c.deck.id, COUNT(c) FROM Card c
            WHERE c.deck.id IN :deckIds
              AND NOT EXISTS (
                  SELECT rp FROM ReviewProgress rp
                  WHERE rp.card = c AND rp.user.id = :userId
              )
            GROUP BY c.deck.id
            """)
    List<Object[]> countNewCardsByDeckIdIn(@Param("userId") Long userId,
                                           @Param("deckIds") List<Long> deckIds);
}
