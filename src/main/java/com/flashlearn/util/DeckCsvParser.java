package com.flashlearn.util;

import com.flashlearn.entity.Card;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Разбор CSV для колод: полный формат (мета + front/back/hint), как при экспорте личной колоды,
 * и упрощённый (только карточки).
 */
public final class DeckCsvParser {

    /** Ограничение размера загружаемого CSV (защита от злоупотреблений). */
    public static final long MAX_CSV_BYTES = 5 * 1024 * 1024;

    private DeckCsvParser() {
    }

    public static void assertCsvSizeWithinLimit(MultipartFile file) {
        if (file != null && file.getSize() > MAX_CSV_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл слишком большой (макс. 5 МБ)");
        }
    }

    public record CsvDeckData(
            String title,
            String description,
            String categoryName,
            boolean isPublicFromFile,
            List<Card> cards
    ) {
    }

    /**
     * Упрощённый CSV: строки front;back;hint (первая строка может быть заголовком)
     */
    public static List<Card> parseSimpleCards(MultipartFile file) {
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
                    String first = parts.length > 0 ? parts[0].trim().toLowerCase(Locale.ROOT) : "";
                    String second = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "";
                    if (("front".equals(first) || "вопрос".equals(first))
                            && ("back".equals(second) || "ответ".equals(second))) {
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

    /**
     * Полный формат: строки title;…, description;…, category;…, затем блок карточек (как при экспорте)
     */
    public static CsvDeckData parseDeckCsv(MultipartFile file) {
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
                    if (("front".equals(first) || "вопрос".equals(first) || "название".equals(first))
                            && ("back".equals(second) || "ответ".equals(second))) {
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

    public static String extractTitleFromFile(MultipartFile file) {
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

    private static String[] splitCsvLine(String line) {
        if (line.contains(";")) {
            return line.split(";", -1);
        }
        return line.split(",", -1);
    }

    private static boolean isMetaKey(String key) {
        return "title".equals(key) || "название".equals(key) || "deck".equals(key)
                || "description".equals(key) || "описание".equals(key)
                || "category".equals(key) || "тип".equals(key) || "категория".equals(key)
                || "public".equals(key) || "ispublic".equals(key) || "публичная".equals(key);
    }

    private static boolean parseBoolean(String value) {
        String normalized = normalizeToken(value);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "да".equals(normalized);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
