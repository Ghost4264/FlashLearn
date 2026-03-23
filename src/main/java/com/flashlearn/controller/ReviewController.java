package com.flashlearn.controller;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.ReviewResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Контроллер интервального повторения (SM-2)
 */
@Tag(name = "Review", description = "Интервальное повторение по алгоритму SM-2")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Получить список карточек, которые пора повторить сегодня
     */
    @Operation(summary = "Карточки к повторению", description = "Возвращает карточки у которых nextReviewAt <= now")
    @ApiResponse(responseCode = "200", description = "Список карточек")
    @GetMapping("/due")
    public ResponseEntity<List<CardResponse>> getDueCards(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.getDueCards(user.getId()));
    }

    /**
     * Получить количество карточек к повторению
     */
    @Operation(summary = "Количество к повторению", description = "Возвращает {\"count\": N}")
    @ApiResponse(responseCode = "200", description = "Количество карточек")
    @GetMapping("/due/count")
    public ResponseEntity<Map<String, Long>> getDueCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", reviewService.getDueCount(user.getId())));
    }

    /**
     * Оценить карточку после повторения
     */
    @Operation(
            summary = "Оценить карточку",
            description = "Применяет SM-2 и возвращает новый интервал и дату следующего повторения. " +
                    "quality: 0=провал, 3=с трудом, 5=идеально"
    )
    @ApiResponse(responseCode = "200", description = "Прогресс обновлён")
    @ApiResponse(responseCode = "400", description = "quality вне диапазона 0-5")
    @ApiResponse(responseCode = "404", description = "Карточка не найдена")
    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.submitReview(request, user.getId()));
    }
}
