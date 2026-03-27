package com.flashlearn.controller;

import com.flashlearn.dto.request.DeckRequest;
import com.flashlearn.dto.response.DeckResponse;
import com.flashlearn.dto.response.PageResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * Контроллер управления колодами карточек
 */
@Tag(name = "Decks", description = "Управление колодами карточек")
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    /**
     * Получить колоды текущего пользователя постранично, отсортированные по дате создания
     */
    @Operation(summary = "Мои колоды", description = "Постраничный список колод авторизованного пользователя")
    @ApiResponse(responseCode = "200", description = "Страница колод")
    @GetMapping
    public ResponseEntity<PageResponse<DeckResponse>> getMyDecks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(deckService.getMyDecks(user.getId(), categoryId, q, pageable));
    }

    /**
     * Получить все публичные колоды платформы постранично
     */
    @Operation(summary = "Публичные колоды", description = "Доступно без авторизации")
    @ApiResponse(responseCode = "200", description = "Страница публичных колод")
    @SecurityRequirements
    @GetMapping("/public")
    public ResponseEntity<PageResponse<DeckResponse>> getPublicDecks(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long viewerUserId = user != null ? user.getId() : null;
        return ResponseEntity.ok(deckService.getPublicDecks(categoryName, q, pageable, viewerUserId));
    }

    @Operation(summary = "Категории публичных колод")
    @SecurityRequirements
    @GetMapping("/public/categories")
    public ResponseEntity<java.util.List<String>> getPublicDeckCategories() {
        return ResponseEntity.ok(deckService.getPublicDeckCategories());
    }

    /**
     * Получить колоду по идентификатору
     */
    @Operation(summary = "Колода по ID", description = "Владелец видит свои приватные колоды; чужие приватные возвращают 404")
    @ApiResponse(responseCode = "200", description = "Колода найдена")
    @ApiResponse(responseCode = "404", description = "Колода не найдена")
    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> getById(
            @Parameter(description = "ID колоды") @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deckService.getById(id, user.getId()));
    }

    @Operation(summary = "Экспорт личной колоды в CSV", description = "Только для непубличных колод владельца; UTF-8 с BOM")
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportPersonalDeckCsv(
            @Parameter(description = "ID колоды") @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        byte[] bytes = deckService.exportPersonalDeckCsv(id, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deck-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * Создать новую колоду для текущего пользователя
     */
    @Operation(summary = "Создать колоду")
    @ApiResponse(responseCode = "201", description = "Колода создана")
    @ApiResponse(responseCode = "400", description = "Невалидные данные")
    @PostMapping
    public ResponseEntity<DeckResponse> create(
            @Valid @RequestBody DeckRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request, user.getId()));
    }

    /**
     * Обновить существующую колоду
     */
    @Operation(summary = "Обновить колоду")
    @ApiResponse(responseCode = "200", description = "Колода обновлена")
    @ApiResponse(responseCode = "403", description = "Нет доступа")
    @ApiResponse(responseCode = "404", description = "Колода не найдена")
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> update(
            @Parameter(description = "ID колоды") @PathVariable Long id,
            @Valid @RequestBody DeckRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deckService.update(id, request, user.getId()));
    }

    /**
     * Удалить колоду вместе со всеми карточками
     */
    @Operation(summary = "Удалить колоду", description = "Каскадно удаляет все карточки")
    @ApiResponse(responseCode = "204", description = "Колода удалена")
    @ApiResponse(responseCode = "403", description = "Нет доступа")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID колоды") @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        deckService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Клонировать публичную колоду в свою коллекцию
     */
    @Operation(summary = "Клонировать колоду", description = "Копирует публичную колоду и все карточки текущему пользователю")
    @ApiResponse(responseCode = "201", description = "Колода склонирована")
    @ApiResponse(responseCode = "403", description = "Колода не публичная")
    @ApiResponse(responseCode = "404", description = "Колода не найдена")
    @PostMapping("/{id}/clone")
    public ResponseEntity<DeckResponse> clone(
            @Parameter(description = "ID источника") @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.clone(id, user.getId()));
    }
}
