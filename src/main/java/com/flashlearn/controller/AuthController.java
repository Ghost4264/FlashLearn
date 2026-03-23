package com.flashlearn.controller;

import com.flashlearn.dto.request.LoginRequest;
import com.flashlearn.dto.request.RefreshRequest;
import com.flashlearn.dto.request.RegisterRequest;
import com.flashlearn.dto.response.AuthResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер аутентификации
 */
@Tag(name = "Auth", description = "Регистрация, вход, обновление и отзыв токенов")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Регистрация нового пользователя
     */
    @Operation(summary = "Регистрация", description = "Создаёт нового пользователя и возвращает пару токенов")
    @ApiResponse(responseCode = "201", description = "Пользователь создан")
    @ApiResponse(responseCode = "400", description = "Невалидные данные")
    @ApiResponse(responseCode = "409", description = "Email уже занят")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Вход в систему по email и паролю
     */
    @Operation(summary = "Вход", description = "Аутентификация по email и паролю")
    @ApiResponse(responseCode = "200", description = "Успешный вход")
    @ApiResponse(responseCode = "401", description = "Неверный email или пароль")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Обновление access токена по refresh токену
     */
    @Operation(summary = "Обновить токен", description = "Выдаёт новую пару токенов по действующему refresh токену")
    @ApiResponse(responseCode = "200", description = "Токены обновлены")
    @ApiResponse(responseCode = "401", description = "Refresh токен недействителен или отозван")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Выход из системы
     */
    @Operation(summary = "Выход", description = "Отзывает все refresh токены пользователя")
    @ApiResponse(responseCode = "204", description = "Выход выполнен")
    @ApiResponse(responseCode = "403", description = "Не авторизован")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        authService.logout(user.getId());
        return ResponseEntity.noContent().build();
    }
}
