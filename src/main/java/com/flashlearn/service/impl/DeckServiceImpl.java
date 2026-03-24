package com.flashlearn.service.impl;

import com.flashlearn.dto.request.DeckRequest;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.Category;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.DeckMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.CategoryRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final CategoryRepository categoryRepository;
    private final DeckMapper deckMapper;

    /**
     * Возвращает колоды пользователя с опциональным фильтром по категории и поиском по тексту
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getMyDecks(Long userId, Long categoryId, String q, Pageable pageable) {
        String search = StringUtils.hasText(q) ? q.trim() : null;
        var page = deckRepository.findAllByUserIdFiltered(userId, categoryId, search, pageable);
        var counts = bulkCounts(page.getContent(), userId);
        return PageResponse.of(page.map(deck -> toResponse(deck, counts)));
    }

    /**
     * Возвращает публичные колоды с опциональным поиском по тексту
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getPublicDecks(String categoryName, String q, Pageable pageable) {
        String search = StringUtils.hasText(q) ? q.trim() : null;
        String category = StringUtils.hasText(categoryName) ? categoryName.trim() : null;
        var page = deckRepository.findAllPublicFiltered(category, search, pageable);
        var counts = bulkCounts(page.getContent(), null);
        return PageResponse.of(page.map(deck -> toResponse(deck, counts)));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getPublicDeckCategories() {
        return deckRepository.findDistinctCategoryNamesInPublicDecks();
    }

    /**
     * Возвращает колоду по id
     * Публичную — всем, приватную — только владельцу
     */
    @Override
    @Transactional(readOnly = true)
    public DeckResponse getById(Long deckId, Long userId) {
        Deck deck = findDeck(deckId);
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
                .isPublic(false)
                .category(resolveCategory(request.getCategoryId(), userId))
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
        deck.setPublic(false);
        deck.setCategory(resolveCategory(request.getCategoryId(), userId));

        return toResponse(deckRepository.save(deck), userId);
    }

    /**
     * Удаляет колоду вместе со всеми карточками
     */
    @Override
    @Transactional
    public void delete(Long deckId, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);
        if (deck.isPublic()) {
            throw new AccessDeniedException("Публичные колоды управляются через админ-панель");
        }
        deckRepository.delete(deck);
    }

    /**
     * Клонирует публичную колоду: копирует колоду и все карточки для пользователя
     */
    @Override
    @Transactional
    public DeckResponse clone(Long deckId, Long userId) {
        Deck source = findDeck(deckId);
        if (!source.isPublic()) {
            throw new AccessDeniedException("Клонировать можно только публичные колоды");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        Deck copy = deckRepository.save(Deck.builder()
                .user(user)
                .title(source.getTitle())
                .description(source.getDescription())
                .isPublic(false)
                .category(resolveOrCreateCategoryByName(
                        user,
                        source.getCategory() != null ? source.getCategory().getName() : "Разное"
                ))
                .build());

        List<com.flashlearn.entity.Card> sourceCards = cardRepository.findAllByDeckIdOrderByPosition(source.getId());
        List<com.flashlearn.entity.Card> copiedCards = sourceCards.stream()
                .map(card -> com.flashlearn.entity.Card.builder()
                        .deck(copy)
                        .front(card.getFront())
                        .back(card.getBack())
                        .hint(card.getHint())
                        .position(card.getPosition())
                        .build())
                .toList();
        cardRepository.saveAll(copiedCards);

        return toResponse(copy, userId);
    }

    private Deck findDeck(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> ResourceNotFoundException.of("Колода", deckId));
    }

    private Deck findOwnedDeck(Long deckId, Long userId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Колода не найдена или не принадлежит пользователю"));
    }

    /**
     * Разрешает Category по ID. Null → null (без категории)
     */
    private Category resolveCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new AccessDeniedException("Категория не найдена"));
    }

    private Category resolveOrCreateCategoryByName(User user, String categoryName) {
        return categoryRepository.findAllByUserIdOrderByName(user.getId())
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .user(user)
                        .name(categoryName)
                        .build()));
    }

    private DeckResponse toResponse(Deck deck, DeckCounts counts) {
        DeckResponse response = deckMapper.toResponse(deck);
        response.setCardCount(counts.cardCounts().getOrDefault(deck.getId(), 0L).intValue());
        long due = counts.dueCounts().getOrDefault(deck.getId(), 0L);
        long newCards = counts.newCounts().getOrDefault(deck.getId(), 0L);
        response.setDueCardCount(due + newCards);
        return response;
    }

    private DeckResponse toResponse(Deck deck, Long userId) {
        DeckResponse response = deckMapper.toResponse(deck);
        response.setCardCount(cardRepository.countByDeckId(deck.getId()));
        if (userId != null) {
            long due = reviewProgressRepository.countDueCardsByDeck(userId, deck.getId(), LocalDateTime.now());
            long newCards = cardRepository.countNewCardsByDeck(userId, deck.getId());
            response.setDueCardCount(due + newCards);
        }
        return response;
    }

    private DeckCounts bulkCounts(List<Deck> decks, Long userId) {
        List<Long> deckIds = decks.stream().map(Deck::getId).toList();
        if (deckIds.isEmpty()) {
            return new DeckCounts(Map.of(), Map.of(), Map.of());
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

        Map<Long, Long> newCounts = userId != null
                ? cardRepository.countNewCardsByDeckIdIn(userId, deckIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ))
                : Map.of();

        return new DeckCounts(cardCounts, dueCounts, newCounts);
    }

    private record DeckCounts(Map<Long, Long> cardCounts, Map<Long, Long> dueCounts, Map<Long, Long> newCounts) {}
}
