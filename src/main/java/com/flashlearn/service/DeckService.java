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
     * Получить публичные колоды постранично
     */
    PageResponse<DeckResponse> getPublicDecks(String q, Pageable pageable);

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
}
