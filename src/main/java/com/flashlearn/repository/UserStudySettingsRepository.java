package com.flashlearn.repository;

import com.flashlearn.entity.UserStudySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStudySettingsRepository extends JpaRepository<UserStudySettings, Long> {
    Optional<UserStudySettings> findByUserId(Long userId);
}
