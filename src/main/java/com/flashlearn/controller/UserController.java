package com.flashlearn.controller;

import com.flashlearn.dto.request.ChangePasswordRequest;
import com.flashlearn.dto.request.UpdateUserRequest;
import com.flashlearn.dto.response.UserResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер профиля пользователя
 */
@Tag(name = "Users", description = "Управление профилем текущего пользователя")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Получить профиль текущего авторизованного пользователя
     */
    @Operation(summary = "Мой профиль", description = "Возвращает данные авторизованного пользователя")
    @ApiResponse(responseCode = "200", description = "Профиль получен")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getMe(user.getId()));
    }

    /**
     * Обновить имя текущего пользователя
     */
    @Operation(summary = "Обновить профиль", description = "Изменяет отображаемое имя пользователя")
    @ApiResponse(responseCode = "200", description = "Профиль обновлён")
    @ApiResponse(responseCode = "400", description = "Невалидные данные")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.updateMe(user.getId(), request));
    }

    /**
     * Сменить пароль
     */
    @Operation(summary = "Сменить пароль", description = "Требует текущий пароль. После смены все сессии инвалидируются")
    @ApiResponse(responseCode = "204", description = "Пароль изменён")
    @ApiResponse(responseCode = "400", description = "Неверный текущий пароль или слишком короткий новый")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User user) {
        userService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
