package com.flashlearn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashlearn.entity.Role;
import com.flashlearn.entity.User;
import com.flashlearn.repository.CardRepository;
import com.flashlearn.repository.CategoryRepository;
import com.flashlearn.repository.DeckRepository;
import com.flashlearn.repository.RefreshTokenRepository;
import com.flashlearn.repository.ReviewProgressRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Базовый класс для интеграционных тестов контроллеров
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected DeckRepository deckRepository;

    @Autowired
    protected CardRepository cardRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ReviewProgressRepository reviewProgressRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        reviewProgressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        cardRepository.deleteAll();
        deckRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Создаёт пользователя в БД и возвращает JWT-токен для авторизации запросов
     */
    protected String createUserAndGetToken(String email, String name) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .name(name)
                .role(Role.USER)
                .build());
        return jwtService.generateToken(user);
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }
}
