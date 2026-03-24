package com.flashlearn.service;

import com.flashlearn.dto.request.ReviewRequest;
import com.flashlearn.dto.response.ReviewResponse;
import com.flashlearn.entity.Card;
import com.flashlearn.entity.ReviewProgress;
import com.flashlearn.entity.User;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.CardMapper;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewProgressRepository reviewProgressRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").build();
        card = Card.builder().id(10L).build();
    }

    // ─── Вспомогательный метод ────────────────────────────────────────────────

    private ReviewProgress freshProgress() {
        return ReviewProgress.builder()
                .user(user)
                .card(card)
                .easeFactor(2.5)
                .intervalDays(1)
                .repetitions(0)
                .nextReviewAt(LocalDateTime.now())
                .build();
    }

    private ReviewResponse submitWith(int quality, ReviewProgress progress) {
        ReviewRequest request = new ReviewRequest();
        request.setCardId(card.getId());
        request.setQuality(quality);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(reviewProgressRepository.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.of(progress));
        when(reviewProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        return reviewService.submitReview(request, user.getId());
    }

    // ─── Первое повторение (repetitions = 0) ──────────────────────────────────

    @Test
    void firstCorrectAnswer_intervalBecomesOne() {
        ReviewProgress p = freshProgress();

        ReviewResponse result = submitWith(4, p);

        assertThat(result.getIntervalDays()).isEqualTo(1);
        assertThat(p.getRepetitions()).isEqualTo(1);
    }

    // ─── Второе повторение (repetitions = 1) ──────────────────────────────────

    @Test
    void secondCorrectAnswer_intervalBecomesSix() {
        ReviewProgress p = freshProgress();
        p.setRepetitions(1);
        p.setIntervalDays(1);

        ReviewResponse result = submitWith(5, p);

        assertThat(result.getIntervalDays()).isEqualTo(6);
        assertThat(p.getRepetitions()).isEqualTo(2);
    }

    // ─── Третье повторение (repetitions = 2) ──────────────────────────────────

    @Test
    void thirdCorrectAnswer_intervalScalesWithEaseFactor() {
        ReviewProgress p = freshProgress();
        p.setRepetitions(2);
        p.setIntervalDays(6);
        p.setEaseFactor(2.5);

        // ожидаемый интервал = round(6 * 2.5) = 15
        ReviewResponse result = submitWith(5, p);

        assertThat(result.getIntervalDays()).isEqualTo(15);
        assertThat(p.getRepetitions()).isEqualTo(3);
    }

    // ─── easeFactor растёт при хорошем ответе ─────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "5,2.5,2.6",
            "4,2.5,2.5",
            "3,2.5,2.36",
            "2,2.5,2.18",
            "0,1.4,1.3"
    })
    void easeFactor_changesAsExpected(int quality, double initialEaseFactor, double expectedEaseFactor) {
        ReviewProgress p = freshProgress();
        p.setEaseFactor(initialEaseFactor);

        submitWith(quality, p);

        assertThat(p.getEaseFactor()).isCloseTo(expectedEaseFactor, within(0.001));
    }

    // ─── Сброс прогресса при неправильном ответе ──────────────────────────────

    @Test
    void wrongAnswer_resetsRepetitionsAndInterval() {
        ReviewProgress p = freshProgress();
        p.setRepetitions(5);
        p.setIntervalDays(30);

        submitWith(1, p);

        assertThat(p.getRepetitions()).isZero();
        assertThat(p.getIntervalDays()).isOne();
    }

    // ─── nextReviewAt устанавливается корректно ────────────────────────────────

    @Test
    void afterReview_nextReviewAtIsSetToFuture() {
        ReviewProgress p = freshProgress();
        LocalDateTime before = LocalDateTime.now();

        submitWith(5, p);

        assertThat(p.getNextReviewAt()).isAfterOrEqualTo(before.plusDays(1));
    }

    @Test
    void wrongAnswer_nextReviewAtIsTomorrow() {
        ReviewProgress p = freshProgress();
        LocalDateTime before = LocalDateTime.now();

        submitWith(0, p);

        assertThat(p.getNextReviewAt()).isAfterOrEqualTo(before.plusDays(1));
        assertThat(p.getNextReviewAt()).isBefore(before.plusDays(2));
    }

    // ─── Создаётся новый прогресс если не существует ──────────────────────────

    @Test
    void newCard_progressCreatedWithDefaults() {
        ReviewRequest request = new ReviewRequest();
        request.setCardId(card.getId());
        request.setQuality(4);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(reviewProgressRepository.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.empty());
        when(reviewProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse result = reviewService.submitReview(request, user.getId());

        assertThat(result.getIntervalDays()).isEqualTo(1);
    }

    // ─── Исключения ───────────────────────────────────────────────────────────

    @Test
    void unknownCard_throws404() {
        ReviewRequest request = new ReviewRequest();
        request.setCardId(999L);
        request.setQuality(4);
        Long userId = user.getId();

        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
