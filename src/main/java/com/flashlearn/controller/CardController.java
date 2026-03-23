package com.flashlearn.controller;

import com.flashlearn.dto.request.CardRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер управления карточками внутри колоды.
 * Все операции выполняются в контексте конкретной колоды ({deckId}).
 */
@Tag(name = "Cards", description = "Управление карточками внутри колоды")
@RestController
@RequestMapping("/api/decks/{deckId}/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Получить карточки колоды постранично, отсортированные по полю position
     */
    @Operation(summary = "Карточки колоды", description = "Постраничный список карточек отсортированных по position")
    @ApiResponse(responseCode = "200", description = "Страница карточек")
    @ApiResponse(responseCode = "403", description = "Нет доступа к колоде")
    @ApiResponse(responseCode = "404", description = "Колода не найдена")
    @GetMapping
    public ResponseEntity<PageResponse<CardResponse>> getByDeck(
            @Parameter(description = "ID колоды") @PathVariable Long deckId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 50, sort = "position", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(cardService.getByDeckId(deckId, user.getId(), pageable));
    }

    /**
     * Создать новую карточку в указанной колоде
     */
    @Operation(summary = "Создать карточку")
    @ApiResponse(responseCode = "201", description = "Карточка создана")
    @ApiResponse(responseCode = "400", description = "Невалидные данные")
    @ApiResponse(responseCode = "403", description = "Нет доступа к колоде")
    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Parameter(description = "ID колоды") @PathVariable Long deckId,
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(deckId, request, user.getId()));
    }

    /**
     * Обновить содержимое карточки
     */
    @Operation(summary = "Обновить карточку")
    @ApiResponse(responseCode = "200", description = "Карточка обновлена")
    @ApiResponse(responseCode = "403", description = "Нет доступа")
    @ApiResponse(responseCode = "404", description = "Карточка не найдена")
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> update(
            @Parameter(description = "ID колоды") @PathVariable Long deckId,
            @Parameter(description = "ID карточки") @PathVariable Long cardId,
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cardService.update(cardId, request, user.getId()));
    }

    /**
     * Удалить карточку
     */
    @Operation(summary = "Удалить карточку")
    @ApiResponse(responseCode = "204", description = "Карточка удалена")
    @ApiResponse(responseCode = "403", description = "Нет доступа")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID колоды") @PathVariable Long deckId,
            @Parameter(description = "ID карточки") @PathVariable Long cardId,
            @AuthenticationPrincipal User user) {
        cardService.delete(cardId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
