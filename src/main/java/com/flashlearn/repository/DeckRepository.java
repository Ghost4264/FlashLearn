package com.flashlearn.repository;

import com.flashlearn.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    Page<Deck> findAllByUserId(Long userId, Pageable pageable);

    Page<Deck> findAllByIsPublicTrue(Pageable pageable);

    Optional<Deck> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
