package com.flashlearn.service.impl;

import com.flashlearn.dto.request.DeckRequest;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.DeckMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Реализация сервиса управления колодами карточек
 */
@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final ReviewProgressRepository reviewProgressRepository;
    private final DeckMapper deckMapper;

    /**
     * Возвращает все колоды пользователя с количеством карточек и дедлайнами
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getMyDecks(Long userId, Pageable pageable) {
        var page = deckRepository.findAllByUserId(userId, pageable);
        var counts = bulkCounts(page.getContent(), userId);
        return PageResponse.of(page.map(deck -> toResponse(deck, counts)));
    }

    /**
     * Возвращает публичные колоды постранично — доступны без авторизации
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getPublicDecks(Pageable pageable) {
        var page = deckRepository.findAllByIsPublicTrue(pageable);
        var counts = bulkCounts(page.getContent(), null);
        return PageResponse.of(page.map(deck -> toResponse(deck, counts)));
    }

    /**
     * Возвращает колоду по id
     * Публичную — всем, приватную — только владельцу
     */
    @Override
    @Transactional(readOnly = true)
    public DeckResponse getById(Long deckId, Long userId) {
        Deck deck = findDeck(deckId);
        // Логика для проверки публичности
        if (!deck.isPublic() && !deck.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к колоде id=" + deckId);
        }
        return toResponse(deck, userId);
    }

    /**
     * Создаёт новую колоду и привязывает её к пользователю
     */
    @Override
    @Transactional
    public DeckResponse create(DeckRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        Deck deck = Deck.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.isPublic())
                .build();

        return toResponse(deckRepository.save(deck), userId);
    }

    /**
     * Обновляет поля колоды
     */
    @Override
    @Transactional
    public DeckResponse update(Long deckId, DeckRequest request, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);

        deck.setTitle(request.getTitle());
        deck.setDescription(request.getDescription());
        deck.setPublic(request.isPublic());

        return toResponse(deckRepository.save(deck), userId);
    }

    /**
     * Удаляет колоду вместе со всеми карточками
     */
    @Override
    @Transactional
    public void delete(Long deckId, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);
        deckRepository.delete(deck);
    }


    /**
     * Ищет колоду по id
     */
    private Deck findDeck(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> ResourceNotFoundException.of("Колода", deckId));
    }

    /**
     * Ищет колоду принадлежащую пользователю
     * Бросает AccessDeniedException если чужая
     */
    private Deck findOwnedDeck(Long deckId, Long userId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Колода не найдена или не принадлежит пользователю"));
    }

    /**
     * Конвертирует Entity в DTO, используя предзагруженные счётчики из bulkCounts
     */
    private DeckResponse toResponse(Deck deck, DeckCounts counts) {
        DeckResponse response = deckMapper.toResponse(deck);
        response.setCardCount(counts.cardCounts().getOrDefault(deck.getId(), 0L).intValue());
        response.setDueCardCount(counts.dueCounts().getOrDefault(deck.getId(), 0L).intValue());
        return response;
    }

    /**
     * Конвертирует Entity в DTO через маппер и добавляет вычисляемые счётчики (одиночный вызов)
     */
    private DeckResponse toResponse(Deck deck, Long userId) {
        DeckResponse response = deckMapper.toResponse(deck);
        response.setCardCount(cardRepository.countByDeckId(deck.getId()));
        response.setDueCardCount(userId != null
                ? reviewProgressRepository.countDueCardsByDeck(userId, deck.getId(), LocalDateTime.now())
                : 0);
        return response;
    }

    /**
     * Загружает счётчики карточек и дедлайнов для списка колод двумя запросами вместо N*2
     */
    private DeckCounts bulkCounts(List<Deck> decks, Long userId) {
        List<Long> deckIds = decks.stream().map(Deck::getId).toList();
        if (deckIds.isEmpty()) {
            return new DeckCounts(Map.of(), Map.of());
        }

        Map<Long, Long> cardCounts = cardRepository.countByDeckIdIn(deckIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> dueCounts = userId != null
                ? reviewProgressRepository.countDueCardsByDeckIdIn(userId, deckIds, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ))
                : Map.of();

        return new DeckCounts(cardCounts, dueCounts);
    }

    private record DeckCounts(Map<Long, Long> cardCounts, Map<Long, Long> dueCounts) {}
}
