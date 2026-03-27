package com.flashlearn.repository;

import com.flashlearn.entity.ReviewProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewProgressRepository extends JpaRepository<ReviewProgress, Long> {

    Optional<ReviewProgress> findByUserIdAndCardId(Long userId, Long cardId);

    @Query("""
            SELECT rp FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.card.deck.isPublic = false
              AND rp.nextReviewAt <= :now
            ORDER BY rp.nextReviewAt ASC
            """)
    List<ReviewProgress> findDueCards(@Param("userId") Long userId,
                                      @Param("now") LocalDateTime now);

    @Query("""
            SELECT rp FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.card.deck.id = :deckId
              AND rp.nextReviewAt <= :now
            ORDER BY rp.nextReviewAt ASC
            """)
    List<ReviewProgress> findDueCardsByDeck(@Param("userId") Long userId,
                                            @Param("deckId") Long deckId,
                                            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(rp) FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.card.deck.isPublic = false
              AND rp.nextReviewAt <= :now
            """)
    long countDueCards(@Param("userId") Long userId,
                       @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(rp) FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.card.deck.id = :deckId
              AND rp.nextReviewAt <= :now
            """)
    long countDueCardsByDeck(@Param("userId") Long userId,
                             @Param("deckId") Long deckId,
                             @Param("now") LocalDateTime now);

    @Query("""
            SELECT rp.card.deck.id, COUNT(rp) FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.card.deck.id IN :deckIds
              AND rp.nextReviewAt <= :now
            GROUP BY rp.card.deck.id
            """)
    List<Object[]> countDueCardsByDeckIdIn(@Param("userId") Long userId,
                                           @Param("deckIds") List<Long> deckIds,
                                           @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(rp) FROM ReviewProgress rp
            WHERE rp.user.id = :userId
              AND rp.lastReviewAt >= :from
              AND rp.lastReviewAt < :to
            """)
    long countReviewsBetween(@Param("userId") Long userId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT DISTINCT CAST(rp.last_review_at AS DATE)
            FROM review_progress rp
            WHERE rp.user_id = :userId AND rp.last_review_at IS NOT NULL
            """, nativeQuery = true)
    List<java.sql.Date> findDistinctReviewDates(@Param("userId") Long userId);
}
