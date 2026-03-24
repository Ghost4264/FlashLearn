package com.flashlearn.controller;

import com.flashlearn.dto.request.CategoryRequest;
import com.flashlearn.dto.response.CategoryResponse;
import com.flashlearn.entity.User;
import com.flashlearn.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories", description = "Категории колод")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Мои категории")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getMyCategories(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.getMyCategories(user.getId()));
    }

    @Operation(summary = "Создать категорию")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request, user.getId()));
    }

    @Operation(summary = "Удалить категорию", description = "Колоды теряют категорию, но не удаляются")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        categoryService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
