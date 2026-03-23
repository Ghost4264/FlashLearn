package com.flashlearn.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {

    @Test
    void getMe_returnsCurrentUser() throws Exception {
        String token = createUserAndGetToken("user@example.com", "Иван Петров");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("Иван Петров"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void getMe_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_twoUsers_returnsOwnProfile() throws Exception {
        String tokenA = createUserAndGetToken("alice@example.com", "Alice");
        String tokenB = createUserAndGetToken("bob@example.com", "Bob");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", authHeader(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", authHeader(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    // --- PUT /api/users/me ---

    @Test
    void updateMe_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "Старое Имя");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Новое Имя"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое Имя"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void updateMe_emptyName_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
    }

    @Test
    void updateMe_nameTooLong_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");
        String longName = "а".repeat(101);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", longName
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
    }

    @Test
    void updateMe_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Имя"
                        ))))
                .andExpect(status().isForbidden());
    }

    // --- PATCH /api/users/me/password ---

    @Test
    void changePassword_success() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "password123",
                                "newPassword", "newpassword456"
                        ))))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "wrongpassword",
                                "newPassword", "newpassword456"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_shortNewPassword_returns400() throws Exception {
        String token = createUserAndGetToken("user@example.com", "User");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "password123",
                                "newPassword", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").isNotEmpty());
    }
}
