package com.flashlearn.service.impl;

import com.flashlearn.dto.request.ChangePasswordRequest;
import com.flashlearn.dto.request.UpdateUserRequest;
import com.flashlearn.dto.response.UserResponse;
import com.flashlearn.entity.User;
import com.flashlearn.exception.InvalidPasswordException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.mapper.UserMapper;
import com.flashlearn.repository.RefreshTokenRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса управления профилем пользователя
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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
        return userMapper.toResponse(userRepository.save(user));
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
    }

    /**
     * Ищет пользователя по id — бросает ResourceNotFoundException если не найден
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));
    }

}
