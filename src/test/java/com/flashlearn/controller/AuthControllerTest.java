package com.flashlearn.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseControllerTest {

    @Test
    void authFlow_registerRefreshLogout_thenRefreshReturns401() throws Exception {
        String registerResponse = performRegister("flow@example.com", "password123", "Flow User")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String accessToken = registerJson.get("accessToken").asText();
        String refreshToken = registerJson.get("refreshToken").asText();

        String refreshedResponse = performRefresh(refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshedResponse).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", authHeader(accessToken)))
                .andExpect(status().isNoContent());

        performRefresh(rotatedRefreshToken)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_success() throws Exception {
        performRegister("user@example.com", "password123", "Test User")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createUserAndGetToken("dup@example.com", "User One");

        performRegister("dup@example.com", "password123", "User Two")
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        performRegister("not-an-email", "password123", "Test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        performRegister("user@example.com", "short", "Test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    @Test
    void login_success() throws Exception {
        createUserAndGetToken("login@example.com", "Login User");

        performLogin("login@example.com", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        createUserAndGetToken("login@example.com", "Login User");

        performLogin("login@example.com", "wrongpassword")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        performLogin("ghost@example.com", "password123")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_authenticated_returns204() throws Exception {
        String token = createUserAndGetToken("logout@example.com", "Logout User");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_success() throws Exception {

        createUserAndGetToken("refresh@example.com", "Refresh User");

        String loginResponse = performLogin("refresh@example.com", "password123")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        performRefresh(refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        performRefresh("invalid-token-that-does-not-exist")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedToken_returns401() throws Exception {
        createUserAndGetToken("revoke@example.com", "Revoke User");

        String loginResponse = performLogin("revoke@example.com", "password123")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        performRefresh(refreshToken)
                .andExpect(status().isOk());

        performRefresh(refreshToken)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions performRegister(String email, String password, String name) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password,
                        "name", name
                ))));
    }

    private ResultActions performLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password
                ))));
    }

    private ResultActions performRefresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "refreshToken", refreshToken
                ))));
    }
}
