package com.flashlearn.service.impl;

import com.flashlearn.dto.request.ChangePasswordRequest;
import com.flashlearn.dto.request.UpdateStudySettingsRequest;
import com.flashlearn.dto.request.UpdateUserRequest;
import com.flashlearn.dto.response.StudySettingsResponse;
import com.flashlearn.dto.response.UserResponse;
import com.flashlearn.entity.UserStudySettings;
import com.flashlearn.entity.User;
import com.flashlearn.exception.InvalidPasswordException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.UserMapper;
import com.flashlearn.repository.RefreshTokenRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.repository.UserStudySettingsRepository;
import com.flashlearn.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса управления профилем пользователя
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int DEFAULT_NEW_CARDS_PER_SESSION = 20;
    private static final double DEFAULT_INTERVAL_MODIFIER = 1.0;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserStudySettingsRepository userStudySettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Возвращает профиль пользователя по id
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    /**
     * Обновляет имя пользователя и возвращает обновлённый профиль
     */
    @Override
    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.setName(request.getName());
        userRepository.save(user);
        log.info("Обновлён профиль: userId={}", userId);
        return userMapper.toResponse(user);
    }

    /**
     * Проверяет текущий пароль, хэширует новый и сбрасывает все refresh токены —
     * после смены пароля все активные сессии инвалидируются
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Инвалидируем все сессии — пользователю придётся войти заново
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Смена пароля: userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudySettingsResponse getStudySettings(Long userId) {
        findUser(userId);
        UserStudySettings settings = userStudySettingsRepository.findByUserId(userId)
                .orElseGet(() -> buildDefaultSettings(userId));
        return StudySettingsResponse.builder()
                .newCardsPerSession(settings.getNewCardsPerSession())
                .intervalModifier(settings.getIntervalModifier())
                .build();
    }

    @Override
    @Transactional
    public StudySettingsResponse updateStudySettings(Long userId, UpdateStudySettingsRequest request) {
        User user = findUser(userId);
        UserStudySettings settings = userStudySettingsRepository.findByUserId(userId)
                .orElseGet(() -> UserStudySettings.builder()
                        .user(user)
                        .newCardsPerSession(DEFAULT_NEW_CARDS_PER_SESSION)
                        .intervalModifier(DEFAULT_INTERVAL_MODIFIER)
                        .build());

        settings.setNewCardsPerSession(request.getNewCardsPerSession());
        settings.setIntervalModifier(request.getIntervalModifier());
        UserStudySettings saved = userStudySettingsRepository.save(settings);
        log.info(
                "Обновлены настройки учёбы: userId={}, newCardsPerSession={}, intervalModifier={}",
                userId,
                saved.getNewCardsPerSession(),
                saved.getIntervalModifier()
        );
        return StudySettingsResponse.builder()
                .newCardsPerSession(saved.getNewCardsPerSession())
                .intervalModifier(saved.getIntervalModifier())
                .build();
    }

    /**
     * Ищет пользователя по id — бросает ResourceNotFoundException если не найден
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));
    }

    private UserStudySettings buildDefaultSettings(Long userId) {
        User user = findUser(userId);
        return UserStudySettings.builder()
                .user(user)
                .newCardsPerSession(DEFAULT_NEW_CARDS_PER_SESSION)
                .intervalModifier(DEFAULT_INTERVAL_MODIFIER)
                .build();
    }

}
