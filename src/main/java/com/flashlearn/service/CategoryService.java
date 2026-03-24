package com.flashlearn.service;

import com.flashlearn.dto.request.CategoryRequest;
import com.flashlearn.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getMyCategories(Long userId);

    CategoryResponse create(CategoryRequest request, Long userId);

    void delete(Long categoryId, Long userId);
}
