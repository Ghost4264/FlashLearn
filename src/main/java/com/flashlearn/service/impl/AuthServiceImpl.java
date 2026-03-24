package com.flashlearn.service.impl;

import com.flashlearn.dto.request.LoginRequest;
import com.flashlearn.dto.request.RegisterRequest;
import com.flashlearn.dto.response.AuthResponse;
import com.flashlearn.entity.RefreshToken;
import com.flashlearn.entity.Role;
import com.flashlearn.entity.User;
import com.flashlearn.entity.Category;
import com.flashlearn.exception.EmailAlreadyExistsException;
import com.flashlearn.exception.InvalidTokenException;
import com.flashlearn.repository.RefreshTokenRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.repository.CategoryRepository;
import com.flashlearn.repository.CategoryPresetRepository;
import com.flashlearn.security.JwtService;
import com.flashlearn.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Реализация сервиса аутентификации
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryPresetRepository categoryPresetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Java", "Языки", "Kotlin", "Docker", "Git", "Разное");

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Регистрирует нового пользователя
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        createDefaultCategories(user);
        return buildAuthResponse(user);
    }

    /**
     * Выполняет вход в систему
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Бросает BadCredentialsException если email/пароль неверны
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        return buildAuthResponse(user);
    }

    /**
     * Обновляет access токен по refresh токену.
     * Проверяет что токен существует, не отозван и не истёк.
     */
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh токен не найден"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh токен был отозван");
        }

        if (stored.isExpired()) {
            throw new InvalidTokenException("Refresh токен истёк");
        }

        // Отзываем старый refresh токен и выдаём новую пару
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    /**
     * Выход из системы — отзывает все refresh токены пользователя
     */
    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Генерирует access + refresh токены и формирует ответ
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    /**
     * Создаёт и сохраняет новый refresh токен для пользователя
     */
    private String createRefreshToken(User user) {

        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private void createDefaultCategories(User user) {
        List<String> presetNames = categoryPresetRepository.findAllByOrderByNameAsc().stream()
                .map(com.flashlearn.entity.CategoryPreset::getName)
                .toList();
        List<String> categoryNames = presetNames.isEmpty() ? DEFAULT_CATEGORIES : presetNames;
        var categories = categoryNames.stream()
                .map(name -> Category.builder()
                        .user(user)
                        .name(name)
                        .build())
                .toList();
        categoryRepository.saveAll(categories);
    }
}
