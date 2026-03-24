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
    AdminBulkDeckResponse importPublicDeckFromCsv(Long adminUserId, MultipartFile file);
    AdminBulkDeckResponse createPublicDeck(Long adminUserId, String title, String description, String categoryName);
    void deletePublicDeck(Long deckId);
}
