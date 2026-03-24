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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        return CategoryResponse.builder().id(preset.getId()).name(preset.getName()).build();
    }

    @Override
    @Transactional
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

        List<Card> cards = parseCards(file);
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

        return AdminDeckImportResponse.builder()
                .deck(response)
                .importedCards(cards.size())
                .build();
    }

    @Override
    @Transactional
    public AdminBulkDeckResponse importDeckFromCsvForAllUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV файл обязателен");
        }
        CsvDeckData csvDeckData = parseDeckCsv(file);
        if (csvDeckData.cards().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV не содержит карточек");
        }

        String baseTitle = csvDeckData.title() == null || csvDeckData.title().isBlank()
                ? extractTitleFromFile(file)
                : csvDeckData.title();
        String baseDescription = csvDeckData.description() == null
                ? "Импортировано администратором из CSV"
                : csvDeckData.description();
        boolean baseIsPublic = csvDeckData.isPublic();
        String baseCategory = csvDeckData.categoryName() == null || csvDeckData.categoryName().isBlank()
                ? DEFAULT_CATEGORY_NAME
                : csvDeckData.categoryName();

        List<User> users = userRepository.findAll();
        int decksCreated = 0;
        int cardsCreated = 0;

        for (User user : users) {
            Category category = resolveOrCreateCategory(user, baseCategory);
            Deck deck = deckRepository.save(Deck.builder()
                    .user(user)
                    .title(baseTitle)
                    .description(baseDescription)
                    .isPublic(baseIsPublic)
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
            decksCreated++;
            cardsCreated += cards.size();
        }

        return AdminBulkDeckResponse.builder()
                .decksCreated(decksCreated)
                .cardsCreated(cardsCreated)
                .build();
    }

    @Override
    @Transactional
    public AdminBulkDeckResponse createDeckForAllUsers(String title, String description, boolean isPublic, String categoryName) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название колоды обязательно");
        }
        String normalizedCategory = categoryName == null || categoryName.trim().isEmpty()
                ? DEFAULT_CATEGORY_NAME
                : categoryName.trim();

        List<User> users = userRepository.findAll();
        int decksCreated = 0;
        for (User user : users) {
            Category category = resolveOrCreateCategory(user, normalizedCategory);
            deckRepository.save(Deck.builder()
                    .user(user)
                    .title(normalizedTitle)
                    .description(description)
                    .isPublic(isPublic)
                    .category(category)
                    .build());
            decksCreated++;
        }

        return AdminBulkDeckResponse.builder()
                .decksCreated(decksCreated)
                .cardsCreated(0)
                .build();
    }

    private List<Card> parseCards(MultipartFile file) {
        List<Card> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerChecked = false;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = splitCsvLine(trimmed);
                if (!headerChecked) {
                    headerChecked = true;
                    String first = parts.length > 0 ? parts[0].trim().toLowerCase() : "";
                    String second = parts.length > 1 ? parts[1].trim().toLowerCase() : "";
                    if (("front".equals(first) || "вопрос".equals(first)) &&
                            ("back".equals(second) || "ответ".equals(second))) {
                        continue;
                    }
                }
                if (parts.length < 2) {
                    continue;
                }
                String front = parts[0].trim();
                String back = parts[1].trim();
                String hint = parts.length >= 3 ? parts[2].trim() : null;
                if (front.isEmpty() || back.isEmpty()) {
                    continue;
                }
                cards.add(Card.builder()
                        .front(front)
                        .back(back)
                        .hint(hint == null || hint.isEmpty() ? null : hint)
                        .build());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать CSV");
        }
        return cards;
    }

    private CsvDeckData parseDeckCsv(MultipartFile file) {
        String title = null;
        String description = null;
        String categoryName = null;
        boolean isPublic = false;
        List<Card> cards = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean cardsSectionStarted = false;
            boolean headerChecked = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = splitCsvLine(trimmed);
                if (!cardsSectionStarted) {
                    String key = normalizeToken(parts.length > 0 ? parts[0] : "");
                    if (isMetaKey(key) && parts.length >= 2) {
                        String value = parts[1].trim();
                        if ("title".equals(key) || "название".equals(key) || "deck".equals(key)) {
                            title = value;
                            continue;
                        }
                        if ("description".equals(key) || "описание".equals(key)) {
                            description = value;
                            continue;
                        }
                        if ("category".equals(key) || "тип".equals(key) || "категория".equals(key)) {
                            categoryName = value;
                            continue;
                        }
                        if ("public".equals(key) || "ispublic".equals(key) || "публичная".equals(key)) {
                            isPublic = parseBoolean(value);
                            continue;
                        }
                    }
                    cardsSectionStarted = true;
                }

                if (!headerChecked) {
                    headerChecked = true;
                    String first = normalizeToken(parts.length > 0 ? parts[0] : "");
                    String second = normalizeToken(parts.length > 1 ? parts[1] : "");
                    if (("front".equals(first) || "вопрос".equals(first) || "название".equals(first)) &&
                            ("back".equals(second) || "ответ".equals(second))) {
                        continue;
                    }
                }
                if (parts.length < 2) {
                    continue;
                }
                String front = parts[0].trim();
                String back = parts[1].trim();
                String hint = parts.length >= 3 ? parts[2].trim() : null;
                if (front.isEmpty() || back.isEmpty()) {
                    continue;
                }
                cards.add(Card.builder()
                        .front(front)
                        .back(back)
                        .hint(hint == null || hint.isEmpty() ? null : hint)
                        .build());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать CSV");
        }

        return new CsvDeckData(title, description, categoryName, isPublic, cards);
    }

    private String[] splitCsvLine(String line) {
        if (line.contains(";")) {
            return line.split(";", -1);
        }
        return line.split(",", -1);
    }

    private String extractTitleFromFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.trim().isEmpty()) {
            return "Новая колода";
        }
        String trimmed = name.trim();
        int dotIdx = trimmed.toLowerCase(Locale.ROOT).lastIndexOf(".csv");
        if (dotIdx > 0) {
            return trimmed.substring(0, dotIdx);
        }
        return trimmed;
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

    private boolean isMetaKey(String key) {
        return "title".equals(key) || "название".equals(key) || "deck".equals(key)
                || "description".equals(key) || "описание".equals(key)
                || "category".equals(key) || "тип".equals(key) || "категория".equals(key)
                || "public".equals(key) || "ispublic".equals(key) || "публичная".equals(key);
    }

    private boolean parseBoolean(String value) {
        String normalized = normalizeToken(value);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "да".equals(normalized);
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CsvDeckData(
            String title,
            String description,
            String categoryName,
            boolean isPublic,
            List<Card> cards
    ) {
    }
}
