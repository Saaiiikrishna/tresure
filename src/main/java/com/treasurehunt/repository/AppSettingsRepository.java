package com.treasurehunt.repository;

import com.treasurehunt.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for AppSettings entity
 */
@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    
    /**
     * Find setting by key
     * @param settingKey The setting key
     * @return Optional AppSettings
     */
    Optional<AppSettings> findBySettingKey(String settingKey);
    
    /**
     * Check if setting exists by key
     * @param settingKey The setting key
     * @return true if exists
     */
    boolean existsBySettingKey(String settingKey);
}
