package com.flashlearn.service.impl;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.CardResponse;
import com.flashlearn.dto.response.ReviewResponse;
import com.flashlearn.entity.Card;
import com.flashlearn.entity.ReviewProgress;
import com.flashlearn.entity.User;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.CardMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Реализация сервиса интервального повторения
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double DEFAULT_EASE_FACTOR = 2.5;

    private final ReviewProgressRepository reviewProgressRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    /**
     * Возвращает карточки у которых nextReviewAt <= текущего времени
     */
    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getDueCards(Long userId) {
        return reviewProgressRepository.findDueCards(userId, LocalDateTime.now())
                .stream()
                .map(rp -> cardMapper.toResponse(rp.getCard()))
                .toList();
    }

    /**
     * Возвращает количество карточек готовых к повторению прямо сейчас
     */
    @Override
    @Transactional(readOnly = true)
    public long getDueCount(Long userId) {
        return reviewProgressRepository.countDueCards(userId, LocalDateTime.now());
    }

    /**
     * Принимает оценку ответа, применяет SM-2 и сохраняет новый прогресс
     */
    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest request, Long userId) {
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> ResourceNotFoundException.of("Карточка", request.getCardId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        // Получаем существующий прогресс или создаём новый
        ReviewProgress progress = reviewProgressRepository
                .findByUserIdAndCardId(userId, card.getId())
                .orElseGet(() -> createInitialProgress(user, card));

        // Применение SM-2
        applySpacedRepetition(progress, request.getQuality());

        reviewProgressRepository.save(progress);

        return ReviewResponse.builder()
                .cardId(card.getId())
                .intervalDays(progress.getIntervalDays())
                .easeFactor(progress.getEaseFactor())
                .nextReviewAt(progress.getNextReviewAt())
                .build();
    }

    // SM-2 алгоритм 

    /**
     * Применяет алгоритм SuperMemo SM-2 для расчёта следующего интервала повторения
     *
     * @param progress текущий прогресс карточки
     * @param quality  оценка качества ответа (0-5)
     */
    private void applySpacedRepetition(ReviewProgress progress, int quality) {
        double oldEaseFactor = progress.getEaseFactor();

        // SM-2: коэффициент лёгкости пересчитывается при любом качестве ответа
        double newEaseFactor = oldEaseFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        progress.setEaseFactor(Math.max(MIN_EASE_FACTOR, newEaseFactor));

        if (quality >= 3) {
            // Правильный ответ — увеличиваем интервал на основе старого EF
            int newInterval = switch (progress.getRepetitions()) {
                case 0 -> 1;
                case 1 -> 6;
                default -> (int) Math.round(progress.getIntervalDays() * oldEaseFactor);
            };
            progress.setRepetitions(progress.getRepetitions() + 1);
            progress.setIntervalDays(newInterval);
        } else {
            // Неправильный ответ — сбрасываем прогресс, EF уже снижен выше
            progress.setRepetitions(0);
            progress.setIntervalDays(1);
        }

        progress.setLastReviewAt(LocalDateTime.now());
        progress.setNextReviewAt(LocalDateTime.now().plusDays(progress.getIntervalDays()));
    }

    /**
     * Создаёт начальный прогресс для карточки которую повторяют впервые
     */
    private ReviewProgress createInitialProgress(User user, Card card) {
        return ReviewProgress.builder()
                .user(user)
                .card(card)
                .easeFactor(DEFAULT_EASE_FACTOR)
                .intervalDays(1)
                .repetitions(0)
                .nextReviewAt(LocalDateTime.now())
                .build();
    }

}
