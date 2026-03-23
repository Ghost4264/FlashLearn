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

    Optional<Card> findByIdAndDeckId(Long id, Long deckId);

    int countByDeckId(Long deckId);

    void deleteAllByDeckId(Long deckId);

    @Query("""
            SELECT c.deck.id, COUNT(c) FROM Card c
            WHERE c.deck.id IN :deckIds
            GROUP BY c.deck.id
            """)
    List<Object[]> countByDeckIdIn(@Param("deckIds") List<Long> deckIds);
}
