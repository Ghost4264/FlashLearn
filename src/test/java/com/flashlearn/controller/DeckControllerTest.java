package com.flashlearn.controller;

import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeckControllerTest extends BaseControllerTest {

    @Test
    void getMyDecks_returnsOnlyOwnDecks() throws Exception {
        String token = createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();

        createDeck(owner, "Моя колода", false);
        createDeck(owner, "Ещё колода", false);

        createUserAndGetToken("other@example.com", "Other");
        User other = userRepository.findByEmail("other@example.com").orElseThrow();
        createDeck(other, "Чужая колода", false);

        performGetDecks(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Ещё колода", "Моя колода")));
    }

    @Test
    void getMyDecks_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPublicDecks_returnsOnlyPublic() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();

        createDeck(user, "Публичная", true);
        createDeck(user, "Приватная", false);

        performGetPublicDecks(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Публичная"));
    }

    @Test
    void getDeckById_ownDeck_returnsOk() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Моя колода", false);

        performGetDeckById(token, deck.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deck.getId()))
                .andExpect(jsonPath("$.title").value("Моя колода"));
    }

    @Test
    void getDeckById_publicDeck_accessibleByAnyone() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck publicDeck = createDeck(owner, "Публичная", true);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performGetDeckById(otherToken, publicDeck.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Публичная"));
    }

    @Test
    void getDeckById_privateDeckOfOther_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck privateDeck = createDeck(owner, "Приватная", false);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performGetDeckById(otherToken, privateDeck.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void createDeck_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        performCreateDeck(token, "Новая колода", "Описание", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Новая колода"))
                .andExpect(jsonPath("$.cardCount").value(0));
    }

    @Test
    void createDeck_emptyTitle_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        performCreateDeck(token, "", null, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
    }

    @Test
    void updateDeck_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Старое название", false);

        performUpdateDeck(token, deck.getId(), "Новое название", "", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"));
    }

    @Test
    void updateDeck_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Колода", false);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performUpdateDeck(otherToken, deck.getId(), "Взломано", null, false)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteDeck_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "На удаление", false);

        performDeleteDeck(token, deck.getId())
                .andExpect(status().isNoContent());

        performGetDeckById(token, deck.getId())
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeck_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Защищённая", false);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performDeleteDeck(otherToken, deck.getId())
                .andExpect(status().isForbidden());
    }

    private ResultActions performGetDecks(String token) throws Exception {
        return mockMvc.perform(get("/api/decks")
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performGetPublicDecks(String token) throws Exception {
        return mockMvc.perform(get("/api/decks/public")
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performGetDeckById(String token, Long deckId) throws Exception {
        return mockMvc.perform(get("/api/decks/" + deckId)
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performCreateDeck(String token, String title, String description, boolean isPublic) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("public", isPublic);
        if (description != null) {
            body.put("description", description);
        }
        return mockMvc.perform(post("/api/decks")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performUpdateDeck(String token, Long deckId, String title, String description, boolean isPublic) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("public", isPublic);
        if (description != null) {
            body.put("description", description);
        }
        return mockMvc.perform(put("/api/decks/" + deckId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteDeck(String token, Long deckId) throws Exception {
        return mockMvc.perform(delete("/api/decks/" + deckId)
                .header("Authorization", authHeader(token)));
    }

    private Deck createDeck(User user, String title, boolean isPublic) {
        return deckRepository.save(Deck.builder()
                .user(user)
                .title(title)
                .description("")
                .isPublic(isPublic)
                .build());
    }
}
