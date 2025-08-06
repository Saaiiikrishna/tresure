package com.treasurehunt.service;

import com.treasurehunt.entity.AppSettings;
import com.treasurehunt.repository.AppSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing application settings
 */
@Service
@Transactional
public class AppSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(AppSettingsService.class);

    @Autowired
    private AppSettingsRepository appSettingsRepository;

    // In-memory cache for settings to reduce database calls
    private final ConcurrentHashMap<String, String> settingsCache = new ConcurrentHashMap<>();
    private volatile long lastCacheRefresh = 0;
    private static final long CACHE_REFRESH_INTERVAL = 300000; // 5 minutes

    /**
     * Load all settings into cache on startup and periodically refresh
     */
    @PostConstruct
    public void loadAllSettingsIntoCache() {
        try {
            logger.info("Loading all settings into cache...");
            List<AppSettings> allSettings = appSettingsRepository.findAll();

            settingsCache.clear();
            for (AppSettings setting : allSettings) {
                settingsCache.put(setting.getSettingKey(), setting.getSettingValue());
            }

            lastCacheRefresh = System.currentTimeMillis();
            logger.info("Loaded {} settings into cache", settingsCache.size());

        } catch (Exception e) {
            logger.error("Error loading settings into cache", e);
        }
    }

    /**
     * Initialize default settings on startup
     */
    public void initializeDefaultSettings() {
        try {
            logger.info("Initializing default application settings...");

            // Check if settings table is accessible
            long settingsCount = appSettingsRepository.count();
            logger.debug("Found {} existing settings", settingsCount);

            // Initialize default settings if none exist
            if (settingsCount == 0) {
                initializeDefaults();
            }

            logger.info("Application settings initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error initializing application settings: {}", e.getMessage());
            logger.warn("Application will continue startup without default settings initialization");
            logger.debug("Full error details:", e);
            // Don't throw exception to prevent application startup failure
            // Settings can be configured later through admin interface
        }
    }

    /**
     * Initialize default settings from environment variables or fallback to defaults
     */
    private void initializeDefaults() {
        try {
            // Hero preview video URL (YouTube embed only)
            String heroPreviewVideoUrl = getEnvOrDefault("HERO_PREVIEW_VIDEO_URL", "https://www.youtube.com/embed/dQw4w9WgXcQ");
            updateSetting("hero_preview_video_url", heroPreviewVideoUrl, "Hero section preview video URL (YouTube only)");

            // Hero background video URL (YouTube or uploaded video)
            String heroBackgroundVideoUrl = getEnvOrDefault("HERO_BACKGROUND_VIDEO_URL", "");
            updateSetting("hero_background_video_url", heroBackgroundVideoUrl, "Hero section background video URL");

            // Company info from environment variables or defaults
            Map<String, String> companyInfo = new HashMap<>();
            companyInfo.put("name", getEnvOrDefault("COMPANY_NAME", "Treasure Hunt Adventures"));
            companyInfo.put("address", getEnvOrDefault("COMPANY_ADDRESS", "123 Adventure Street, City, State 12345"));
            companyInfo.put("phone", getEnvOrDefault("COMPANY_PHONE", "+1-234-567-8900"));
            companyInfo.put("email", getEnvOrDefault("COMPANY_EMAIL", "info@treasurehuntadventures.com"));
            updateCompanyInfo(companyInfo);

            // Contact info from environment variables or defaults
            Map<String, String> contactInfo = new HashMap<>();
            contactInfo.put("phone", getEnvOrDefault("CONTACT_PHONE", "+1-234-567-8900"));
            contactInfo.put("email", getEnvOrDefault("CONTACT_EMAIL", "contact@treasurehuntadventures.com"));
            contactInfo.put("address", getEnvOrDefault("CONTACT_ADDRESS", "123 Adventure Street, City, State 12345"));
            contactInfo.put("hours", getEnvOrDefault("CONTACT_HOURS", "Mon-Fri: 9AM-6PM, Sat-Sun: 10AM-4PM"));
            contactInfo.put("emergency", getEnvOrDefault("CONTACT_EMERGENCY", "+1-234-567-8911"));
            updateContactInfo(contactInfo);

            // Social media links from environment variables or defaults
            Map<String, String> socialLinks = new HashMap<>();
            socialLinks.put("facebook", getEnvOrDefault("SOCIAL_FACEBOOK", "https://facebook.com/treasurehuntadventures"));
            socialLinks.put("twitter", getEnvOrDefault("SOCIAL_TWITTER", "https://twitter.com/treasurehuntadv"));
            socialLinks.put("instagram", getEnvOrDefault("SOCIAL_INSTAGRAM", "https://instagram.com/treasurehuntadventures"));
            socialLinks.put("linkedin", getEnvOrDefault("SOCIAL_LINKEDIN", "https://linkedin.com/company/treasurehuntadventures"));
            socialLinks.put("youtube", getEnvOrDefault("SOCIAL_YOUTUBE", "https://youtube.com/treasurehuntadventures"));
            updateSocialMediaLinks(socialLinks);

            // Image URLs from environment variables or defaults
            updateSetting("hero_fallback_image_url",
                getEnvOrDefault("HERO_FALLBACK_IMAGE_URL", "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"),
                "Hero section fallback image URL");

            updateSetting("about_section_image_url",
                getEnvOrDefault("ABOUT_SECTION_IMAGE_URL", "https://images.unsplash.com/photo-1559827260-dc66d52bef19?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"),
                "About section image URL");

            updateSetting("contact_background_image_url",
                getEnvOrDefault("CONTACT_BACKGROUND_IMAGE_URL", "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"),
                "Contact section background image URL");

            // Hero section blur intensity setting (0-10 scale)
            String heroBlurIntensity = getEnvOrDefault("HERO_BLUR_INTENSITY", "3");
            updateSetting("hero_blur_intensity", heroBlurIntensity, "Hero section background blur intensity (0=no blur, 10=maximum blur)");

            logger.info("Default settings initialized successfully from environment variables");

            // Refresh cache after all settings are initialized
            loadAllSettingsIntoCache();

        } catch (Exception e) {
            logger.error("Error initializing default settings", e);
        }
    }

    /**
     * Get environment variable value or return default
     */
    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Get setting value by key with in-memory caching (no database calls for cached values)
     * @param key Setting key
     * @return Setting value or null if not found
     */
    public String getSettingValue(String key) {
        try {
            if (key == null || key.trim().isEmpty()) {
                logger.warn("Attempted to get setting with null or empty key");
                return null;
            }

            // Check if cache needs refresh
            refreshCacheIfNeeded();

            // Get from cache first
            String cachedValue = settingsCache.get(key);
            if (cachedValue != null) {
                return cachedValue;
            }

            // If not in cache, fetch from database and cache it
            Optional<AppSettings> setting = appSettingsRepository.findBySettingKey(key);
            if (setting.isPresent()) {
                String value = setting.get().getSettingValue();
                settingsCache.put(key, value);
                return value;
            }

            return null;
        } catch (Exception e) {
            logger.error("Error getting setting value for key: {}", key, e);
            return null;
        }
    }

    /**
     * Refresh cache if it's older than the refresh interval
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheRefresh > CACHE_REFRESH_INTERVAL) {
            logger.debug("Cache refresh interval exceeded, refreshing settings cache");
            loadAllSettingsIntoCache();
        }
    }

    /**
     * Get setting value by key with default value
     * @param key Setting key
     * @param defaultValue Default value if setting not found
     * @return Setting value or default value
     */
    public String getSettingValue(String key, String defaultValue) {
        String value = getSettingValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Update or create setting with cache update
     * @param key Setting key
     * @param value Setting value
     * @param description Setting description
     * @return Updated AppSettings
     */
    public AppSettings updateSetting(String key, String value, String description) {
        try {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Setting key cannot be null or empty");
            }

            Optional<AppSettings> existingSetting = appSettingsRepository.findBySettingKey(key);

            AppSettings setting;
            if (existingSetting.isPresent()) {
                setting = existingSetting.get();
                setting.setSettingValue(value);
                if (description != null) {
                    setting.setDescription(description);
                }
            } else {
                setting = new AppSettings(key, value, description);
            }

            AppSettings savedSetting = appSettingsRepository.save(setting);

            // Update cache immediately
            settingsCache.put(key, value);

            logger.info("Updated setting: {} = {}", key, value);
            return savedSetting;
        } catch (Exception e) {
            logger.error("Error updating setting: {} = {}", key, value, e);
            throw new RuntimeException("Failed to update setting: " + key, e);
        }
    }

    /**
     * Get hero background video URL (for background video)
     * @return Hero background video URL or default
     */
    public String getHeroBackgroundVideoUrl() {
        String value = getSettingValue("hero_background_video_url", "");
        return value;
    }

    /**
     * @deprecated Use getHeroBackgroundVideoUrl() instead
     */
    @Deprecated
    public String getHeroVideoUrl() {
        return getHeroBackgroundVideoUrl();
    }

    /**
     * Get hero preview video URL (for preview video in hero section)
     * @return Hero preview video URL or default
     */
    public String getHeroPreviewVideoUrl() {
        String value = getSettingValue("hero_preview_video_url", "https://www.youtube.com/embed/dQw4w9WgXcQ");
        return value;
    }

    /**
     * Get hero fallback image URL
     * @return Hero fallback image URL or default
     */
    public String getHeroFallbackImageUrl() {
        String value = getSettingValue("hero_background_fallback_image", "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
        logger.debug("getHeroFallbackImageUrl() returning: {}", value);
        return value;
    }

    /**
     * Get about section image URL
     * @return About section image URL or default
     */
    public String getAboutSectionImageUrl() {
        String value = getSettingValue("about_section_image", "https://images.unsplash.com/photo-1559827260-dc66d52bef19?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
        logger.debug("getAboutSectionImageUrl() returning: {}", value);
        return value;
    }

    /**
     * Get contact background image URL
     * @return Contact background image URL or default
     */
    public String getContactBackgroundImageUrl() {
        String value = getSettingValue("contact_background_image", "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
        logger.debug("getContactBackgroundImageUrl() returning: {}", value);
        return value;
    }

    /**
     * Update hero preview video URL (YouTube embed only)
     * @param videoUrl New YouTube embed URL
     * @return Updated setting
     */
    public AppSettings updateHeroPreviewVideoUrl(String videoUrl) {
        // Validate YouTube URL
        if (videoUrl != null && !videoUrl.trim().isEmpty()) {
            String trimmed = videoUrl.trim();
            if (!trimmed.contains("youtube.com") && !trimmed.contains("youtu.be")) {
                throw new IllegalArgumentException("Hero preview video must be a YouTube URL");
            }
        }

        return updateSetting("hero_preview_video_url", videoUrl, "Hero section preview video URL (YouTube only)");
    }

    /**
     * Update hero background video URL (YouTube or uploaded video)
     * @param videoUrl New video URL
     * @return Updated setting
     */
    public AppSettings updateHeroBackgroundVideoUrl(String videoUrl) {
        // Validate URL - allow YouTube or uploaded videos only
        if (videoUrl != null && !videoUrl.trim().isEmpty()) {
            String trimmed = videoUrl.trim();
            boolean isYouTube = trimmed.contains("youtube.com") || trimmed.contains("youtu.be");
            boolean isUploadedVideo = trimmed.startsWith("/uploads/") || trimmed.startsWith("uploads/");

            if (!isYouTube && !isUploadedVideo) {
                throw new IllegalArgumentException("Background video must be YouTube URL or uploaded video file");
            }
        }

        return updateSetting("hero_background_video_url", videoUrl, "Hero section background video URL");
    }

    /**
     * Get company information for footer
     * @return Map of company settings
     */
    public Map<String, String> getCompanyInfo() {
        Map<String, String> companyInfo = new HashMap<>();
        companyInfo.put("name", getSettingValue("company_name", "Treasure Hunt Adventures"));
        companyInfo.put("address", getSettingValue("company_address", "123 Adventure Street, Explorer City, EC 12345"));
        companyInfo.put("phone", getSettingValue("company_phone", "+1 (555) 123-4567"));
        companyInfo.put("email", getSettingValue("company_email", "info@treasurehuntadventures.com"));
        return companyInfo;
    }

    /**
     * Get social media links for footer
     * @return Map of social media URLs
     */
    public Map<String, String> getSocialMediaLinks() {
        Map<String, String> socialLinks = new HashMap<>();
        socialLinks.put("facebook", getSettingValue("facebook_url", ""));
        socialLinks.put("twitter", getSettingValue("twitter_url", ""));
        socialLinks.put("instagram", getSettingValue("instagram_url", ""));
        socialLinks.put("linkedin", getSettingValue("linkedin_url", ""));
        socialLinks.put("youtube", getSettingValue("youtube_url", ""));
        return socialLinks;
    }

    /**
     * Get footer links
     * @return Map of footer page URLs
     */
    public Map<String, String> getFooterLinks() {
        Map<String, String> footerLinks = new HashMap<>();
        footerLinks.put("about", getSettingValue("about_us_url", "/about"));
        footerLinks.put("contact", getSettingValue("contact_url", "/contact"));
        footerLinks.put("privacy", getSettingValue("privacy_policy_url", "/privacy"));
        footerLinks.put("terms", getSettingValue("terms_of_service_url", "/terms"));
        return footerLinks;
    }

    /**
     * Update company information
     * @param companyInfo Map of company settings
     */
    public void updateCompanyInfo(Map<String, String> companyInfo) {
        if (companyInfo.containsKey("name")) {
            updateSetting("company_name", companyInfo.get("name"), "Company name displayed in footer");
        }
        if (companyInfo.containsKey("address")) {
            updateSetting("company_address", companyInfo.get("address"), "Company address");
        }
        if (companyInfo.containsKey("phone")) {
            updateSetting("company_phone", companyInfo.get("phone"), "Company phone number");
        }
        if (companyInfo.containsKey("email")) {
            updateSetting("company_email", companyInfo.get("email"), "Company email address");
        }
    }

    /**
     * Update social media links
     * @param socialLinks Map of social media URLs
     */
    public void updateSocialMediaLinks(Map<String, String> socialLinks) {
        if (socialLinks.containsKey("facebook")) {
            updateSetting("facebook_url", socialLinks.get("facebook"), "Facebook page URL");
        }
        if (socialLinks.containsKey("twitter")) {
            updateSetting("twitter_url", socialLinks.get("twitter"), "Twitter profile URL");
        }
        if (socialLinks.containsKey("instagram")) {
            updateSetting("instagram_url", socialLinks.get("instagram"), "Instagram profile URL");
        }
        if (socialLinks.containsKey("linkedin")) {
            updateSetting("linkedin_url", socialLinks.get("linkedin"), "LinkedIn company page URL");
        }
        if (socialLinks.containsKey("youtube")) {
            updateSetting("youtube_url", socialLinks.get("youtube"), "YouTube channel URL");
        }
    }

    /**
     * Get contact information for "Get In Touch" section
     * @return Map of contact settings
     */
    public Map<String, String> getContactInfo() {
        Map<String, String> contactInfo = new HashMap<>();
        contactInfo.put("phone", getSettingValue("contact.phone", "+1 (555) 123-4567"));
        contactInfo.put("email", getSettingValue("contact.email", "info@treasurehuntadventures.com"));
        contactInfo.put("address", getSettingValue("contact.address", "123 Adventure Street, Explorer City, EC 12345"));
        contactInfo.put("hours", getSettingValue("contact.hours", "Monday - Friday: 9:00 AM - 6:00 PM"));
        contactInfo.put("emergency", getSettingValue("contact.emergency", "+1 (555) 911-HELP"));
        return contactInfo;
    }

    /**
     * Update contact information
     * @param contactInfo Map of contact settings
     */
    public void updateContactInfo(Map<String, String> contactInfo) {
        if (contactInfo.containsKey("phone")) {
            updateSetting("contact.phone", contactInfo.get("phone"), "Primary contact phone number");
        }
        if (contactInfo.containsKey("email")) {
            updateSetting("contact.email", contactInfo.get("email"), "Primary contact email address");
        }
        if (contactInfo.containsKey("address")) {
            updateSetting("contact.address", contactInfo.get("address"), "Physical address for contact");
        }
        if (contactInfo.containsKey("hours")) {
            updateSetting("contact.hours", contactInfo.get("hours"), "Business hours");
        }
        if (contactInfo.containsKey("emergency")) {
            updateSetting("contact.emergency", contactInfo.get("emergency"), "Emergency contact number");
        }
    }

    /**
     * Get background media enabled setting
     * @return true if background media is enabled, false otherwise
     */
    public boolean getBackgroundMediaEnabled() {
        String value = getSettingValue("background_media_enabled", "true");
        return Boolean.parseBoolean(value);
    }

    /**
     * Set background media enabled setting
     * @param enabled true to enable background media, false to disable
     */
    public void setBackgroundMediaEnabled(boolean enabled) {
        logger.info("Setting background media enabled to: {}", enabled);
        updateSetting("background_media_enabled", String.valueOf(enabled), "Enable/disable background media on hero section");
        logger.info("Background media enabled setting updated successfully");
    }

    /**
     * Get hero section blur intensity (0-10 scale)
     * @return blur intensity value (0=no blur, 10=maximum blur)
     */
    public int getHeroBlurIntensity() {
        String value = getSettingValue("hero_blur_intensity", "3");
        try {
            int intensity = Integer.parseInt(value);
            // Ensure value is within valid range
            return Math.max(0, Math.min(10, intensity));
        } catch (NumberFormatException e) {
            logger.warn("Invalid blur intensity value: {}, using default: 3", value);
            return 3;
        }
    }

    /**
     * Set hero section blur intensity
     * @param intensity blur intensity (0-10 scale, 0=no blur, 10=maximum blur)
     */
    public void setHeroBlurIntensity(int intensity) {
        // Ensure value is within valid range
        intensity = Math.max(0, Math.min(10, intensity));
        logger.info("Setting hero blur intensity to: {}", intensity);
        updateSetting("hero_blur_intensity", String.valueOf(intensity), "Hero section background blur intensity (0=no blur, 10=maximum blur)");
        logger.info("Hero blur intensity setting updated successfully");
    }

    /**
     * Manually refresh the settings cache (useful for admin operations)
     */
    public void refreshCache() {
        logger.info("Manually refreshing settings cache");
        loadAllSettingsIntoCache();
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", settingsCache.size());
        stats.put("lastRefresh", new java.util.Date(lastCacheRefresh));
        stats.put("refreshInterval", CACHE_REFRESH_INTERVAL);
        return stats;
    }
}
