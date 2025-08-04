package com.treasurehunt.service;

import com.treasurehunt.entity.AppSettings;
import com.treasurehunt.repository.AppSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Get setting value by key
     * @param key Setting key
     * @return Setting value or null if not found
     */
    public String getSettingValue(String key) {
        Optional<AppSettings> setting = appSettingsRepository.findBySettingKey(key);
        return setting.map(AppSettings::getSettingValue).orElse(null);
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
     * Update or create setting
     * @param key Setting key
     * @param value Setting value
     * @param description Setting description
     * @return Updated AppSettings
     */
    public AppSettings updateSetting(String key, String value, String description) {
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
        logger.info("Updated setting: {} = {}", key, value);
        return savedSetting;
    }

    /**
     * Get hero video URL
     * @return Hero video URL or default
     */
    public String getHeroVideoUrl() {
        // Use the correct key that matches ImageManagementService
        String value = getSettingValue("hero_background_video_url", "https://www.youtube.com/embed/dQw4w9WgXcQ");
        logger.debug("getHeroVideoUrl() returning: {}", value);
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
     * Update hero video URL
     * @param videoUrl New video URL
     * @return Updated setting
     */
    public AppSettings updateHeroVideoUrl(String videoUrl) {
        logger.info("=== UPDATING HERO VIDEO URL ===");
        logger.info("New video URL: {}", videoUrl);

        // Use the correct key that matches ImageManagementService
        AppSettings result = updateSetting("hero_background_video_url", videoUrl, "Main hero section video URL");

        // Verify the update was successful
        String retrievedValue = getHeroVideoUrl();
        logger.info("Updated hero video URL. Retrieved value: {}", retrievedValue);
        logger.info("=== HERO VIDEO URL UPDATE COMPLETE ===");

        return result;
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
}
