package com.flashlearn.controller;

import com.flashlearn.dto.request.AiGenerateCardsRequest;
import com.flashlearn.dto.response.AiGenerateCardsResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-эндпоинты для генерации учебных карточек
 */
@Tag(name = "AI", description = "AI-инструменты для работы с карточками")
@RestController
@RequestMapping("/api/ai/cards")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "Сгенерировать карточки из текста")
    @ApiResponse(responseCode = "200", description = "Карточки успешно сгенерированы")
    @ApiResponse(responseCode = "503", description = "AI временно недоступен")
    @PostMapping("/generate")
    public ResponseEntity<AiGenerateCardsResponse> generate(
            @Valid @RequestBody AiGenerateCardsRequest request,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(aiService.generateCards(request, userId));
    }
}
