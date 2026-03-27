package com.flashlearn.service.impl;

import com.flashlearn.dto.response.AdminDeckImportResponse;
import com.flashlearn.dto.response.AdminBulkDeckResponse;
import com.flashlearn.dto.response.CategoryResponse;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.entity.Card;
import com.flashlearn.entity.Category;
import com.flashlearn.entity.CategoryPreset;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.DeckMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.CategoryPresetRepository;
import com.flashlearn.repository.CategoryRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.AdminService;
import com.flashlearn.util.DeckCsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static com.flashlearn.config.CacheConfig.PUBLIC_DECK_CATEGORIES;
import static com.flashlearn.config.CacheConfig.PUBLIC_DECKS;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private static final String DEFAULT_CATEGORY_NAME = "Разное";
    private final CategoryPresetRepository categoryPresetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckMapper deckMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryPresets() {
        return categoryPresetRepository.findAllByOrderByNameAsc().stream()
                .map(p -> CategoryResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse addCategoryPreset(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название категории обязательно");
        }
        if (categoryPresetRepository.existsByNameIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Категория уже существует");
        }

        CategoryPreset preset = categoryPresetRepository.save(CategoryPreset.builder().name(normalized).build());

        List<User> users = userRepository.findAll();
        List<Category> toInsert = new ArrayList<>();
        for (User user : users) {
            if (!categoryRepository.existsByUserIdAndNameIgnoreCase(user.getId(), normalized)) {
                toInsert.add(Category.builder().user(user).name(normalized).build());
            }
        }
        if (!toInsert.isEmpty()) {
            categoryRepository.saveAll(toInsert);
        }

        log.info("Добавлена пресет-категория: presetId={}, name={}", preset.getId(), preset.getName());
        return CategoryResponse.builder().id(preset.getId()).name(preset.getName()).build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {PUBLIC_DECKS, PUBLIC_DECK_CATEGORIES}, allEntries = true)
    public AdminDeckImportResponse importDeckFromCsv(
            Long userId,
            String title,
            String description,
            boolean isPublic,
            Long categoryId,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV файл обязателен");
        }
        DeckCsvParser.assertCsvSizeWithinLimit(file);
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название колоды обязательно");
        }
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Категория обязательна");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new AccessDeniedException("Категория не найдена"));

        List<Card> cards = DeckCsvParser.parseSimpleCards(file);
        if (cards.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV не содержит карточек");
        }

        Deck deck = deckRepository.save(Deck.builder()
                .user(user)
                .title(normalizedTitle)
                .description(description)
                .isPublic(isPublic)
                .category(category)
                .build());

        int pos = 1;
        for (Card c : cards) {
            c.setDeck(deck);
            c.setPosition(pos++);
        }
        cardRepository.saveAll(cards);

        DeckResponse response = deckMapper.toResponse(deck);
        response.setCardCount(cards.size());
        response.setDueCardCount(cards.size());

        log.info(
                "Админ: импорт колоды из CSV (простой формат): adminUserId={}, deckId={}, importedCards={}, isPublic={}",
                userId,
                deck.getId(),
                cards.size(),
                isPublic
        );
        return AdminDeckImportResponse.builder()
                .deck(response)
                .importedCards(cards.size())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {PUBLIC_DECKS, PUBLIC_DECK_CATEGORIES}, allEntries = true)
    public AdminBulkDeckResponse importPublicDeckFromCsv(Long adminUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV файл обязателен");
        }
        DeckCsvParser.assertCsvSizeWithinLimit(file);
        DeckCsvParser.CsvDeckData csvDeckData = DeckCsvParser.parseDeckCsv(file);
        if (csvDeckData.cards().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV не содержит карточек");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", adminUserId));

        String deckTitle = csvDeckData.title() == null || csvDeckData.title().isBlank()
                ? DeckCsvParser.extractTitleFromFile(file)
                : csvDeckData.title();
        String deckDescription = csvDeckData.description() == null
                ? "Добавлено администратором"
                : csvDeckData.description();
        String categoryName = csvDeckData.categoryName() == null || csvDeckData.categoryName().isBlank()
                ? DEFAULT_CATEGORY_NAME
                : csvDeckData.categoryName();

        Category category = resolveOrCreateCategory(admin, categoryName);
        Deck deck = deckRepository.save(Deck.builder()
                .user(admin)
                .title(deckTitle)
                .description(deckDescription)
                .isPublic(true)
                .category(category)
                .build());

        List<Card> cards = new ArrayList<>();
        int pos = 1;
        for (Card src : csvDeckData.cards()) {
            cards.add(Card.builder()
                    .deck(deck)
                    .front(src.getFront())
                    .back(src.getBack())
                    .hint(src.getHint())
                    .position(pos++)
                    .build());
        }
        cardRepository.saveAll(cards);

        log.info(
                "Админ: импорт публичной колоды из CSV: adminUserId={}, deckId={}, cardsCreated={}, title={}",
                adminUserId,
                deck.getId(),
                cards.size(),
                deck.getTitle()
        );
        return AdminBulkDeckResponse.builder()
                .decksCreated(1)
                .cardsCreated(cards.size())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {PUBLIC_DECKS, PUBLIC_DECK_CATEGORIES}, allEntries = true)
    public AdminBulkDeckResponse createPublicDeck(Long adminUserId, String title, String description, String categoryName) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название колоды обязательно");
        }
        String normalizedCategory = categoryName == null || categoryName.trim().isEmpty()
                ? DEFAULT_CATEGORY_NAME
                : categoryName.trim();

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", adminUserId));

        Category category = resolveOrCreateCategory(admin, normalizedCategory);
        Deck created = deckRepository.save(Deck.builder()
                .user(admin)
                .title(normalizedTitle)
                .description(description)
                .isPublic(true)
                .category(category)
                .build());

        log.info(
                "Админ: создана публичная колода: adminUserId={}, deckId={}, title={}",
                adminUserId,
                created.getId(),
                created.getTitle()
        );
        return AdminBulkDeckResponse.builder()
                .decksCreated(1)
                .cardsCreated(0)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {PUBLIC_DECKS, PUBLIC_DECK_CATEGORIES}, allEntries = true)
    public void deletePublicDeck(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> ResourceNotFoundException.of("Колода", deckId));
        if (!deck.isPublic()) {
            throw new AccessDeniedException("Это не публичная колода");
        }
        log.info("Админ: удалена публичная колода: deckId={}, title={}", deckId, deck.getTitle());
        deckRepository.delete(deck);
    }

    private Category resolveOrCreateCategory(User user, String categoryName) {
        return categoryRepository.findAllByUserIdOrderByName(user.getId()).stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .user(user)
                        .name(categoryName)
                        .build()));
    }
}
