package com.flashlearn.controller;

import com.flashlearn.entity.Card;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.ReviewProgress;
import com.flashlearn.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest extends BaseControllerTest {

    @Test
    void getDueCards_returnsCardsWithExpiredReview() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user);

        Card dueCard = createCard(deck, "Просроченная");
        Card futureCard = createCard(deck, "Ещё рано");

        createProgress(user, dueCard, LocalDateTime.now().minusDays(1));

        createProgress(user, futureCard, LocalDateTime.now().plusDays(3));

        mockMvc.perform(get("/api/review/due")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].front").value("Просроченная"));
    }

    @Test
    void getDueCards_noDue_returnsEmptyList() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(get("/api/review/due")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getDueCount_returnsCorrectNumber() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user);

        Card card1 = createCard(deck, "Карточка 1");
        Card card2 = createCard(deck, "Карточка 2");
        Card card3 = createCard(deck, "Карточка 3");

        createProgress(user, card1, LocalDateTime.now().minusHours(1));
        createProgress(user, card2, LocalDateTime.now().minusHours(2));
        createProgress(user, card3, LocalDateTime.now().plusDays(5));

        mockMvc.perform(get("/api/review/due/count")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void submitReview_correctAnswer_increasesInterval() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user);
        Card card = createCard(deck, "Вопрос");

        mockMvc.perform(post("/api/review")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", card.getId(),
                                "quality", 5
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(card.getId()))
                .andExpect(jsonPath("$.intervalDays").value(1))  // первое повторение — 1 день
                .andExpect(jsonPath("$.nextReviewAt").isNotEmpty());
    }

    @Test
    void submitReview_wrongAnswer_resetsProgress() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user);
        Card card = createCard(deck, "Вопрос");

        mockMvc.perform(post("/api/review")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", card.getId(),
                                "quality", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(1));
    }

    @Test
    void submitReview_invalidQuality_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user);
        Card card = createCard(deck, "Вопрос");

        mockMvc.perform(post("/api/review")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", card.getId(),
                                "quality", 10
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quality").isNotEmpty());
    }

    @Test
    void submitReview_unknownCard_returns404() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(post("/api/review")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cardId", 99999,
                                "quality", 4
                        ))))
                .andExpect(status().isNotFound());
    }

    private Deck createDeck(User user) {
        return deckRepository.save(Deck.builder()
                .user(user)
                .title("Тестовая колода")
                .description("")
                .isPublic(false)
                .build());
    }

    private Card createCard(Deck deck, String front) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .front(front)
                .back("Ответ")
                .position(1)
                .build());
    }

    private ReviewProgress createProgress(User user, Card card, LocalDateTime nextReviewAt) {
        return reviewProgressRepository.save(ReviewProgress.builder()
                .user(user)
                .card(card)
                .easeFactor(2.5)
                .intervalDays(1)
                .repetitions(0)
                .nextReviewAt(nextReviewAt)
                .build());
    }
}
