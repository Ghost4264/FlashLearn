package com.flashlearn.repository;

import com.flashlearn.entity.CategoryPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryPresetRepository extends JpaRepository<CategoryPreset, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<CategoryPreset> findAllByOrderByNameAsc();
}
