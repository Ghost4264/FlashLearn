package com.flashlearn.service.impl;

import com.flashlearn.dto.request.DeckRequest;
import com.flashlearn.dto.response.DeckImportCsvResponse;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.Card;
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
import com.flashlearn.util.DeckCsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static com.flashlearn.config.CacheConfig.PUBLIC_DECK_CATEGORIES;
import static com.flashlearn.config.CacheConfig.PUBLIC_DECKS;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация сервиса управления колодами карточек
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {

    private static final String DEFAULT_IMPORT_CATEGORY = "Разное";

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
     * Возвращает публичные колоды с опциональным поиском по тексту.
     * Результат кешируется на 10 минут по параметрам запроса.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PUBLIC_DECKS,
            key = "#categoryName + ':' + #q + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString() + ':' + #viewerUserId")
    public PageResponse<DeckResponse> getPublicDecks(String categoryName, String q, Pageable pageable, Long viewerUserId) {
        String search = StringUtils.hasText(q) ? q.trim() : null;
        String category = StringUtils.hasText(categoryName) ? categoryName.trim() : null;
        var page = deckRepository.findAllPublicFiltered(category, search, pageable);
        var counts = bulkCounts(page.getContent(), null);

        Set<Long> alreadyClonedIds = viewerUserId != null
                ? Set.copyOf(deckRepository.findClonedFromIdsByUserIdAndPublicDeckIds(
                        viewerUserId,
                        page.getContent().stream().map(Deck::getId).toList()))
                : Set.of();

        return PageResponse.of(page.map(deck -> {
            DeckResponse response = toResponse(deck, counts);
            response.setAlreadyCloned(alreadyClonedIds.contains(deck.getId()));
            return response;
        }));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PUBLIC_DECK_CATEGORIES)
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

        deck = deckRepository.save(deck);
        log.info("Создана колода: userId={}, deckId={}, title={}", userId, deck.getId(), deck.getTitle());
        return toResponse(deck, userId);
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

        deck = deckRepository.save(deck);
        log.info("Обновлена колода: userId={}, deckId={}, title={}", userId, deckId, deck.getTitle());
        return toResponse(deck, userId);
    }

    /**
     * Удаляет колоду вместе со всеми карточками
     */
    @Override
    @Transactional
    @CacheEvict(value = PUBLIC_DECKS, allEntries = true)
    public void delete(Long deckId, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);
        if (deck.isPublic()) {
            throw new AccessDeniedException("Публичные колоды управляются через админ-панель");
        }
        log.info("Удалена колода: userId={}, deckId={}, title={}", userId, deckId, deck.getTitle());
        deckRepository.delete(deck);
    }

    /**
     * Клонирует публичную колоду: копирует колоду и все карточки для пользователя
     */
    @Override
    @Transactional
    @CacheEvict(value = PUBLIC_DECKS, allEntries = true)
    public DeckResponse clone(Long deckId, Long userId) {
        Deck source = findDeck(deckId);
        if (!source.isPublic()) {
            throw new AccessDeniedException("Клонировать можно только публичные колоды");
        }
        if (deckRepository.existsByUserIdAndClonedFromId(userId, source.getId())) {
            throw new AccessDeniedException("Эта публичная колода уже есть в ваших колодах");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        Deck copy = deckRepository.save(Deck.builder()
                .user(user)
                .title(source.getTitle())
                .description(source.getDescription())
                .isPublic(false)
                .clonedFromId(source.getId())
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

        log.info(
                "Клонирована публичная колода: userId={}, sourceDeckId={}, newDeckId={}, copiedCards={}, title={}",
                userId,
                source.getId(),
                copy.getId(),
                copiedCards.size(),
                copy.getTitle()
        );
        return toResponse(copy, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPersonalDeckCsv(Long deckId, Long userId) {
        Deck deck = findOwnedDeck(deckId, userId);
        if (deck.isPublic()) {
            throw new AccessDeniedException("Экспорт в CSV доступен только для личных колод, не для публичных");
        }
        log.info("Экспорт колоды в CSV: userId={}, deckId={}, title={}", userId, deckId, deck.getTitle());
        List<Card> cards = cardRepository.findAllByDeckIdOrderByPosition(deckId);
        StringBuilder sb = new StringBuilder();
        sb.append("title;").append(csvCell(deck.getTitle())).append('\n');
        sb.append("description;").append(csvCell(deck.getDescription() != null ? deck.getDescription() : "")).append('\n');
        String categoryName = deck.getCategory() != null ? deck.getCategory().getName() : "";
        sb.append("category;").append(csvCell(categoryName)).append('\n');
        sb.append('\n');
        sb.append("front;back;hint").append('\n');
        for (Card c : cards) {
            sb.append(csvCell(c.getFront())).append(';')
                    .append(csvCell(c.getBack())).append(';')
                    .append(csvCell(c.getHint() != null ? c.getHint() : "")).append('\n');
        }
        byte[] utf8 = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] out = new byte[bom.length + utf8.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(utf8, 0, out, bom.length, utf8.length);
        return out;
    }

    @Override
    @Transactional
    public DeckImportCsvResponse importPersonalDeckFromCsv(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV файл обязателен");
        }
        DeckCsvParser.assertCsvSizeWithinLimit(file);
        DeckCsvParser.CsvDeckData csvDeckData = DeckCsvParser.parseDeckCsv(file);
        if (csvDeckData.cards().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV не содержит карточек");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        String deckTitle = csvDeckData.title() == null || csvDeckData.title().isBlank()
                ? DeckCsvParser.extractTitleFromFile(file)
                : csvDeckData.title().trim();
        String deckDescription = csvDeckData.description() != null ? csvDeckData.description() : "";
        String categoryName = csvDeckData.categoryName() == null || csvDeckData.categoryName().isBlank()
                ? DEFAULT_IMPORT_CATEGORY
                : csvDeckData.categoryName().trim();

        Category category = resolveOrCreateCategoryByName(user, categoryName);
        Deck deck = deckRepository.save(Deck.builder()
                .user(user)
                .title(deckTitle)
                .description(deckDescription)
                .isPublic(false)
                .category(category)
                .build());

        List<Card> toSave = new ArrayList<>();
        int pos = 1;
        for (Card src : csvDeckData.cards()) {
            toSave.add(Card.builder()
                    .deck(deck)
                    .front(src.getFront())
                    .back(src.getBack())
                    .hint(src.getHint())
                    .position(pos++)
                    .build());
        }
        cardRepository.saveAll(toSave);

        log.info(
                "Импорт колоды из CSV: userId={}, deckId={}, cardsImported={}, fileName={}, title={}",
                userId,
                deck.getId(),
                toSave.size(),
                file.getOriginalFilename(),
                deck.getTitle()
        );
        DeckResponse response = toResponse(deck, userId);
        return DeckImportCsvResponse.builder()
                .deck(response)
                .cardsImported(toSave.size())
                .build();
    }

    private static String csvCell(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r\n", "\n").replace("\r", "\n");
        if (t.contains(";") || t.contains("\n") || t.contains("\"")) {
            return "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
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
