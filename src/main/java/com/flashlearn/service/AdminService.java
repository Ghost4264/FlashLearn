package com.flashlearn.service;

import com.flashlearn.dto.response.AdminDeckImportResponse;
import com.flashlearn.dto.response.AdminBulkDeckResponse;
import com.flashlearn.dto.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminService {
    List<CategoryResponse> getCategoryPresets();
    CategoryResponse addCategoryPreset(String name);
    AdminDeckImportResponse importDeckFromCsv(Long userId, String title, String description, boolean isPublic, Long categoryId, MultipartFile file);
    AdminBulkDeckResponse importDeckFromCsvForAllUsers(MultipartFile file);
    AdminBulkDeckResponse createDeckForAllUsers(String title, String description, boolean isPublic, String categoryName);
}
