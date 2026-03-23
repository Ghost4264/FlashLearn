package com.flashlearn.controller;

import com.flashlearn.entity.Card;
import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardControllerTest extends BaseControllerTest {

    @Test
    void getCards_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Колода");

        createCard(deck, "Вопрос 1", "Ответ 1", 1);
        createCard(deck, "Вопрос 2", "Ответ 2", 2);

        performGetCards(token, deck.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getCards_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Чужая колода");

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performGetCards(otherToken, deck.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Колода");

        performCreateCard(token, deck.getId(), "Что такое Spring?", "Java-фреймворк", "Подсказка", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.front").value("Что такое Spring?"))
                .andExpect(jsonPath("$.back").value("Java-фреймворк"))
                .andExpect(jsonPath("$.deckId").value(deck.getId()));
    }

    @Test
    void createCard_missingFront_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Колода");

        performCreateCard(token, deck.getId(), null, "Ответ", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.front").isNotEmpty());
    }

    @Test
    void createCard_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Колода");

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performCreateCard(otherToken, deck.getId(), "Вопрос", "Ответ", null, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCard_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Колода");
        Card card = createCard(deck, "Старый вопрос", "Старый ответ", 1);

        performUpdateCard(token, deck.getId(), card.getId(), "Новый вопрос", "Новый ответ", null, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("Новый вопрос"))
                .andExpect(jsonPath("$.back").value("Новый ответ"));
    }

    @Test
    void updateCard_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Колода");
        Card card = createCard(deck, "Вопрос", "Ответ", 1);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performUpdateCard(otherToken, deck.getId(), card.getId(), "Взломано", "Взломано", null, 1)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Колода");
        Card card = createCard(deck, "Вопрос", "Ответ", 1);

        performDeleteCard(token, deck.getId(), card.getId())
                .andExpect(status().isNoContent());

        performGetCards(token, deck.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteCard_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Колода");
        Card card = createCard(deck, "Вопрос", "Ответ", 1);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performDeleteCard(otherToken, deck.getId(), card.getId())
                .andExpect(status().isForbidden());
    }

    private ResultActions performGetCards(String token, Long deckId) throws Exception {
        return mockMvc.perform(get("/api/decks/" + deckId + "/cards")
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performCreateCard(String token, Long deckId, String front, String back, String hint, Integer position)
            throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (front != null) {
            body.put("front", front);
        }
        if (back != null) {
            body.put("back", back);
        }
        if (hint != null) {
            body.put("hint", hint);
        }
        if (position != null) {
            body.put("position", position);
        }
        return mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performUpdateCard(String token, Long deckId, Long cardId, String front, String back, String hint, Integer position)
            throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (front != null) {
            body.put("front", front);
        }
        if (back != null) {
            body.put("back", back);
        }
        if (hint != null) {
            body.put("hint", hint);
        }
        if (position != null) {
            body.put("position", position);
        }
        return mockMvc.perform(put("/api/decks/" + deckId + "/cards/" + cardId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteCard(String token, Long deckId, Long cardId) throws Exception {
        return mockMvc.perform(delete("/api/decks/" + deckId + "/cards/" + cardId)
                .header("Authorization", authHeader(token)));
    }

    private Deck createDeck(User user, String title) {
        return deckRepository.save(Deck.builder()
                .user(user)
                .title(title)
                .description("")
                .isPublic(false)
                .build());
    }

    private Card createCard(Deck deck, String front, String back, int position) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .front(front)
                .back(back)
                .position(position)
                .build());
    }
}
