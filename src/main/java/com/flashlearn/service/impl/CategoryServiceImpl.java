package com.flashlearn.service.impl;

import com.flashlearn.dto.request.CategoryRequest;
import com.flashlearn.dto.response.CategoryResponse;
import com.flashlearn.entity.Category;
import com.flashlearn.entity.User;
import com.flashlearn.exception.AccessDeniedException;
import com.flashlearn.exception.ResourceNotFoundException;
import com.flashlearn.repository.CategoryRepository;
import com.flashlearn.repository.UserRepository;
import com.flashlearn.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getMyCategories(Long userId) {
        return categoryRepository.findAllByUserIdOrderByName(userId).stream()
                .map(c -> CategoryResponse.builder().id(c.getId()).name(c.getName()).build())
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request, Long userId) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Категория «" + request.getName() + "» уже существует");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Пользователь", userId));
        Category saved = categoryRepository.save(
                Category.builder()
                        .user(user)
                        .name(request.getName().trim())
                        .build()
        );
        return CategoryResponse.builder().id(saved.getId()).name(saved.getName()).build();
    }

    @Override
    @Transactional
    public void delete(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Категория не найдена или не принадлежит пользователю"));
        categoryRepository.delete(category);
    }
}
