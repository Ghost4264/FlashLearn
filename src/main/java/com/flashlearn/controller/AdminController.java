package com.flashlearn.controller;

import com.flashlearn.dto.request.AdminCategoryPresetRequest;
import com.flashlearn.dto.request.AdminCreateDeckRequest;
import com.flashlearn.dto.response.AdminBulkDeckResponse;
import com.flashlearn.dto.response.AdminDeckImportResponse;
import com.flashlearn.dto.response.CategoryResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin", description = "Административные операции")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @Operation(summary = "Список категорий для всех пользователей")
    @GetMapping("/categories/presets")
    public ResponseEntity<List<CategoryResponse>> getCategoryPresets() {
        return ResponseEntity.ok(adminService.getCategoryPresets());
    }

    @Operation(summary = "Добавить новую категорию для всех пользователей")
    @PostMapping("/categories/presets")
    public ResponseEntity<CategoryResponse> addCategoryPreset(@Valid @RequestBody AdminCategoryPresetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addCategoryPreset(request.getName()));
    }

    @Operation(summary = "Импорт колоды из CSV")
    @PostMapping(value = "/decks/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminDeckImportResponse> importDeckFromCsv(
            @AuthenticationPrincipal User user,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("isPublic") boolean isPublic,
            @RequestPart("categoryId") Long categoryId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.importDeckFromCsv(user.getId(), title, description, isPublic, categoryId, file));
    }

    @Operation(summary = "Импорт колоды из CSV для всех пользователей")
    @PostMapping(value = "/decks/import-csv-all-users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminBulkDeckResponse> importDeckFromCsvForAllUsers(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.importDeckFromCsvForAllUsers(file));
    }

    @Operation(summary = "Создать колоду вручную для всех пользователей")
    @PostMapping("/decks/create-for-all-users")
    public ResponseEntity<AdminBulkDeckResponse> createDeckForAllUsers(@Valid @RequestBody AdminCreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDeckForAllUsers(
                request.getTitle(),
                request.getDescription(),
                request.isPublic(),
                request.getCategoryName()
        ));
    }
}
