package com.flashlearn.service.impl;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.ReviewResponse;
import com.flashlearn.dto.response.ReviewStatsResponse;
import com.flashlearn.dto.response.StudyCardResponse;
import com.flashlearn.entity.Card;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.ReviewProgress;
import com.flashlearn.entity.User;
import com.flashlearn.entity.UserStudySettings;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.CardMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.repository.UserStudySettingsRepository;
import com.flashlearn.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация сервиса интервального повторения
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double DEFAULT_EASE_FACTOR = 2.5;
    private static final int DEFAULT_NEW_CARDS_PER_SESSION = 20;
    private static final double DEFAULT_INTERVAL_MODIFIER = 1.0;

    private final ReviewProgressRepository reviewProgressRepository;
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final UserStudySettingsRepository userStudySettingsRepository;
    private final CardMapper cardMapper;

    /**
     * Возвращает карточки для сессии: сначала просроченные (isNew=false), затем новые (isNew=true)
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudyCardResponse> getDueCards(Long userId) {
        UserStudySettings settings = resolveSettings(userId);
        List<StudyCardResponse> result = reviewProgressRepository.findDueCards(userId, LocalDateTime.now())
                .stream()
                .map(rp -> toStudyCard(rp.getCard(), false))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        cardRepository.findNewCards(userId, PageRequest.of(0, settings.getNewCardsPerSession()))
                .stream()
                .map(card -> toStudyCard(card, true))
                .forEach(result::add);
        return result;
    }

    /**
     * Возвращает карточки для сессии по конкретной колоде: просроченные + новые
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudyCardResponse> getDueCardsByDeck(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> ResourceNotFoundException.of("Колода", deckId));
        if (!deck.isPublic() && !deck.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к колоде id=" + deckId);
        }
        UserStudySettings settings = resolveSettings(userId);
        List<StudyCardResponse> result = reviewProgressRepository.findDueCardsByDeck(userId, deckId, LocalDateTime.now())
                .stream()
                .map(rp -> toStudyCard(rp.getCard(), false))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        cardRepository.findNewCardsByDeck(userId, deckId, PageRequest.of(0, settings.getNewCardsPerSession()))
                .stream()
                .map(card -> toStudyCard(card, true))
                .forEach(result::add);
        return result;
    }

    private StudyCardResponse toStudyCard(Card card, boolean isNew) {
        return StudyCardResponse.builder()
                .id(card.getId())
                .deckId(card.getDeck().getId())
                .front(card.getFront())
                .back(card.getBack())
                .hint(card.getHint())
                .position(card.getPosition())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .isNew(isNew)
                .build();
    }

    /**
     * Возвращает количество карточек к изучению: просроченные + новые
     */
    @Override
    @Transactional(readOnly = true)
    public long getDueCount(Long userId) {
        long due = reviewProgressRepository.countDueCards(userId, LocalDateTime.now());
        long newCount = cardRepository.countNewCards(userId);
        return due + newCount;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResponse getStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();
        long reviewedToday = reviewProgressRepository.countReviewsBetween(userId, startToday, endToday);

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime startWeek = weekStart.atStartOfDay();
        long reviewedThisWeek = reviewProgressRepository.countReviewsBetween(userId, startWeek, endToday);

        int streak = computeStreakDays(userId);
        return ReviewStatsResponse.builder()
                .reviewedToday(reviewedToday)
                .reviewedThisWeek(reviewedThisWeek)
                .streakDays(streak)
                .build();
    }

    private int computeStreakDays(Long userId) {
        List<java.sql.Date> raw = reviewProgressRepository.findDistinctReviewDates(userId);
        if (raw.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dates = raw.stream()
                .map(java.sql.Date::toLocalDate)
                .collect(Collectors.toCollection(HashSet::new));
        LocalDate today = LocalDate.now();
        LocalDate anchor = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(anchor)) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = anchor;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * Принимает оценку ответа, применяет SM-2 и сохраняет новый прогресс
     */
    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest request, Long userId) {
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> ResourceNotFoundException.of("Карточка", request.getCardId()));

        Deck deck = card.getDeck();
        if (!deck.isPublic() && !deck.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к карточке id=" + request.getCardId());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));

        // Получаем существующий прогресс или создаём новый
        ReviewProgress progress = reviewProgressRepository
                .findByUserIdAndCardId(userId, card.getId())
                .orElseGet(() -> createInitialProgress(user, card));

        UserStudySettings settings = resolveSettings(userId);
        applySpacedRepetition(progress, request.getQuality(), settings.getIntervalModifier());

        reviewProgressRepository.save(progress);

        log.debug(
                "Оценка повторения: userId={}, cardId={}, deckId={}, quality={}",
                userId,
                card.getId(),
                deck.getId(),
                request.getQuality()
        );
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
    private void applySpacedRepetition(ReviewProgress progress, int quality, double intervalModifier) {
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
            newInterval = Math.max(1, (int) Math.round(newInterval * intervalModifier));
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

    private UserStudySettings resolveSettings(Long userId) {
        return userStudySettingsRepository.findByUserId(userId)
                .orElseGet(() -> UserStudySettings.builder()
                        .newCardsPerSession(DEFAULT_NEW_CARDS_PER_SESSION)
                        .intervalModifier(DEFAULT_INTERVAL_MODIFIER)
                        .build());
    }

}
