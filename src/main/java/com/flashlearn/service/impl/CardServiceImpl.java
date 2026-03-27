package com.flashlearn.service.impl;

import com.flashlearn.dto.request.CardRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.Card;
import com.flashlearn.entity.Deck;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.CardMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса управления карточками
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CardMapper cardMapper;

    /**
     * Возвращает карточки колоды отсортированные по позиции и проверяет владельца
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getByDeckId(Long deckId, Long userId, Pageable pageable) {
        findOwnedDeck(deckId, userId);
        return PageResponse.of(
                cardRepository.findAllByDeckId(deckId, pageable)
                        .map(cardMapper::toResponse)
        );
    }

    /**
     * Создаёт карточку в колоде и проверяет что колода принадлежит пользователю
     */
    @Override
    @Transactional
    public CardResponse create(Long deckId, CardRequest request, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);

        Card card = Card.builder()
                .deck(deck)
                .front(request.getFront())
                .back(request.getBack())
                .hint(request.getHint())
                .position(request.getPosition())
                .build();

        card = cardRepository.save(card);
        log.info("Создана карточка: userId={}, deckId={}, cardId={}", userId, deckId, card.getId());
        return cardMapper.toResponse(card);
    }

    /**
     * Обновляет поля карточки и проверяет что карточка принадлежит пользователю
     */
    @Override
    @Transactional
    public CardResponse update(Long cardId, CardRequest request, Long userId) {
        Card card = findOwnedCard(cardId, userId);

        card.setFront(request.getFront());
        card.setBack(request.getBack());
        card.setHint(request.getHint());
        card.setPosition(request.getPosition());

        card = cardRepository.save(card);
        log.info("Обновлена карточка: userId={}, deckId={}, cardId={}", userId, card.getDeck().getId(), cardId);
        return cardMapper.toResponse(card);
    }

    /**
     * Удаляет карточку и проверяет что карточка принадлежит пользователю
     */
    @Override
    @Transactional
    public void delete(Long cardId, Long userId) {
        Card card = findOwnedCard(cardId, userId);
        log.info("Удалена карточка: userId={}, deckId={}, cardId={}", userId, card.getDeck().getId(), cardId);
        cardRepository.delete(card);
    }

    /**
     * Ищет колоду принадлежащую пользователю и бросает AccessDeniedException если чужая
     */
    private Deck findOwnedDeck(Long deckId, Long userId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Колода не найдена или не принадлежит пользователю"));
    }

    /**
     * Ищет карточку и проверяет через колоду, что она принадлежит пользователю
     */
    private Card findOwnedCard(Long cardId, Long userId) {
        // Ищем карточку и через JOIN проверяем что её колода принадлежит пользователю
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.of("Карточка", cardId));

        if (!card.getDeck().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к карточке id=" + cardId);
        }
        return card;
    }

}
