package com.flashlearn.service;

import com.flashlearn.dto.request.DeckRequest;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface DeckService {

    /**
     * Получить колоды текущего пользователя постранично
     */
    PageResponse<DeckResponse> getMyDecks(Long userId, Long categoryId, String q, Pageable pageable);

    /**
     * Получить публичные колоды постранично с фильтром по категории
     */
    PageResponse<DeckResponse> getPublicDecks(String categoryName, String q, Pageable pageable, Long viewerUserId);

    /**
     * Список уникальных категорий публичных колод
     */
    java.util.List<String> getPublicDeckCategories();

    /**
     * Получить колоду по id + проверка права доступа
     */
    DeckResponse getById(Long deckId, Long userId);

    /**
     * Создать новую колоду для пользователя
     */
    DeckResponse create(DeckRequest request, Long userId);

    /**
     * Обновить существующую колоду + проверка, что колода принадлежит пользователю
     */
    DeckResponse update(Long deckId, DeckRequest request, Long userId);

    /**
     * Удалить колоду + проверка, что колода принадлежит пользователю
     */
    void delete(Long deckId, Long userId);

    /**
     * Клонировать публичную колоду — создаёт копию колоды и всех карточек для пользователя
     */
    DeckResponse clone(Long deckId, Long userId);

    /**
     * Экспорт личной (непубличной) колоды владельца в CSV UTF-8 с BOM для импорта в другом месте
     */
    byte[] exportPersonalDeckCsv(Long deckId, Long userId);
}
