package com.flashlearn.controller;

import com.flashlearn.entity.Deck;
import com.flashlearn.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
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
    @Autowired
    private com.flashlearn.repository.CategoryRepository categoryRepository;


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
    void getMyDecks_filterByCategoryAndSearchAndPagination() throws Exception {
        String token = createUserAndGetToken("filter-owner@example.com", "Owner");
        User owner = userRepository.findByEmail("filter-owner@example.com").orElseThrow();

        com.flashlearn.entity.Category java = createCategory(owner, "Java");
        com.flashlearn.entity.Category math = createCategory(owner, "Math");

        createDeck(owner, "Java basics", false, java);
        createDeck(owner, "Java streams", false, java);
        createDeck(owner, "Algebra", false, math);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("categoryId", String.valueOf(java.getId()));
        params.put("q", "java");
        params.put("page", "0");
        params.put("size", "1");

        performGetDecks(token, params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content[0].title").value(org.hamcrest.Matchers.startsWith("Java")));
    }

    @Test
    void getPublicDecks_searchAndPagination() throws Exception {
        String ownerToken = createUserAndGetToken("public-owner@example.com", "Owner");
        User owner = userRepository.findByEmail("public-owner@example.com").orElseThrow();
        createDeck(owner, "React Public", true);
        createDeck(owner, "React Advanced", true);
        createDeck(owner, "Private React", false);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "react");
        params.put("page", "0");
        params.put("size", "1");

        performGetPublicDecks(ownerToken, params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(1));
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
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Long categoryId = ensureDefaultCategory(user).getId();

        performCreateDeck(token, "Новая колода", "Описание", false, categoryId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Новая колода"))
                .andExpect(jsonPath("$.cardCount").value(0));
    }

    @Test
    void createDeck_emptyTitle_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Long categoryId = ensureDefaultCategory(user).getId();

        performCreateDeck(token, "", null, false, categoryId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
    }

    @Test
    void updateDeck_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        Deck deck = createDeck(user, "Старое название", false);

        performUpdateDeck(token, deck.getId(), "Новое название", "", true, ensureDefaultCategory(user).getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"));
    }

    @Test
    void updateDeck_notOwner_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck deck = createDeck(owner, "Колода", false);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performUpdateDeck(otherToken, deck.getId(), "Взломано", null, false, ensureDefaultCategory(owner).getId())
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

    private ResultActions performGetDecks(String token, Map<String, String> params) throws Exception {
        var requestBuilder = get("/api/decks")
                .header("Authorization", authHeader(token));
        params.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performGetPublicDecks(String token) throws Exception {
        return mockMvc.perform(get("/api/decks/public")
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performGetPublicDecks(String token, Map<String, String> params) throws Exception {
        var requestBuilder = get("/api/decks/public")
                .header("Authorization", authHeader(token));
        params.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performGetDeckById(String token, Long deckId) throws Exception {
        return mockMvc.perform(get("/api/decks/" + deckId)
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performCreateDeck(String token, String title, String description, boolean isPublic, Long categoryId) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("public", isPublic);
        body.put("categoryId", categoryId);
        if (description != null) {
            body.put("description", description);
        }
        return mockMvc.perform(post("/api/decks")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performUpdateDeck(
            String token,
            Long deckId,
            String title,
            String description,
            boolean isPublic,
            Long categoryId
    ) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("public", isPublic);
        body.put("categoryId", categoryId);
        if (description != null) {
            body.put("description", description);
        }
        return mockMvc.perform(put("/api/decks/" + deckId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    void cloneDeck_publicDeck_createsNewDeckWithCards() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck publicDeck = createDeck(owner, "Публичная", true);
        createCard(publicDeck, "Q1", "A1");
        createCard(publicDeck, "Q2", "A2");

        String cloneToken = createUserAndGetToken("clone@example.com", "Cloner");

        performCloneDeck(cloneToken, publicDeck.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Публичная"))
                .andExpect(jsonPath("$.cardCount").value(2));
    }

    @Test
    void cloneDeck_clonedDeckIsPrivate() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck publicDeck = createDeck(owner, "Публичная", true);

        String cloneToken = createUserAndGetToken("clone@example.com", "Cloner");
        User cloner = userRepository.findByEmail("clone@example.com").orElseThrow();

        performCloneDeck(cloneToken, publicDeck.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.public").value(false));

        performGetDecks(cloneToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Публичная"));
    }

    @Test
    void cloneDeck_privateDeck_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck privateDeck = createDeck(owner, "Приватная", false);

        String otherToken = createUserAndGetToken("other@example.com", "Other");

        performCloneDeck(otherToken, privateDeck.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void cloneDeck_unauthenticated_returns403() throws Exception {
        createUserAndGetToken("owner@example.com", "Owner");
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        Deck publicDeck = createDeck(owner, "Публичная", true);

        mockMvc.perform(post("/api/decks/" + publicDeck.getId() + "/clone"))
                .andExpect(status().isForbidden());
    }

    private ResultActions performCloneDeck(String token, Long deckId) throws Exception {
        return mockMvc.perform(post("/api/decks/" + deckId + "/clone")
                .header("Authorization", authHeader(token)));
    }

    private ResultActions performDeleteDeck(String token, Long deckId) throws Exception {
        return mockMvc.perform(delete("/api/decks/" + deckId)
                .header("Authorization", authHeader(token)));
    }

    private com.flashlearn.entity.Card createCard(Deck deck, String front, String back) {
        return cardRepository.save(com.flashlearn.entity.Card.builder()
                .deck(deck)
                .front(front)
                .back(back)
                .position(0)
                .build());
    }

    private Deck createDeck(User user, String title, boolean isPublic) {
        return deckRepository.save(Deck.builder()
                .user(user)
                .title(title)
                .description("")
                .isPublic(isPublic)
                .category(ensureDefaultCategory(user))
                .build());
    }

    private Deck createDeck(User user, String title, boolean isPublic, com.flashlearn.entity.Category category) {
        return deckRepository.save(Deck.builder()
                .user(user)
                .title(title)
                .description("")
                .isPublic(isPublic)
                .category(category)
                .build());
    }

    private com.flashlearn.entity.Category createCategory(User user, String name) {
        return categoryRepository.save(com.flashlearn.entity.Category.builder()
                .user(user)
                .name(name)
                .build());
    }

    private com.flashlearn.entity.Category ensureDefaultCategory(User user) {
        return categoryRepository.findAllByUserIdOrderByName(user.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> createCategory(user, "Разное"));
    }
}
