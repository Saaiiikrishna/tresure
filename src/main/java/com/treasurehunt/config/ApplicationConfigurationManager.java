package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration management system
 * Provides unified access to all application configuration properties
 */
@Configuration
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfigurationManager.class);

    private final Environment environment;

    // File Storage Configuration
    private FileStorageConfig fileStorage = new FileStorageConfig();

    // Email Configuration
    private EmailConfig email = new EmailConfig();

    // Security Configuration
    private SecurityConfig security = new SecurityConfig();

    // Performance Configuration
    private PerformanceConfig performance = new PerformanceConfig();

    // Business Configuration
    private BusinessConfig business = new BusinessConfig();

    // Monitoring Configuration
    private MonitoringConfig monitoring = new MonitoringConfig();

    public ApplicationConfigurationManager(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateConfiguration() {
        try {
            logger.info("Validating application configuration...");

            validateFileStorageConfig();
            validateEmailConfig();
            validateSecurityConfig();
            validatePerformanceConfig();
            validateBusinessConfig();
            validateMonitoringConfig();

            logger.info("✅ Application configuration validation completed successfully");
            logConfigurationSummary();
        } catch (Exception e) {
            logger.warn("⚠️ Configuration validation failed, using defaults: {}", e.getMessage());
            // Don't fail startup, just log the warning and continue with defaults
        }
    }

    /**
     * File storage configuration
     */
    public static class FileStorageConfig {
        private String uploadDir = "uploads/documents";
        private String imageDir = "uploads/images";
        private String tempDir = "uploads/temp";
        private long maxFileSize = 5 * 1024 * 1024; // 5MB
        private long maxImageSize = 2 * 1024 * 1024; // 2MB
        private String[] allowedDocumentTypes = {"pdf", "jpg", "jpeg", "png"};
        private String[] allowedImageTypes = {"jpg", "jpeg", "png", "webp"};

        // Getters and setters
        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
        public String getImageDir() { return imageDir; }
        public void setImageDir(String imageDir) { this.imageDir = imageDir; }
        public String getTempDir() { return tempDir; }
        public void setTempDir(String tempDir) { this.tempDir = tempDir; }
        public long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        public long getMaxImageSize() { return maxImageSize; }
        public void setMaxImageSize(long maxImageSize) { this.maxImageSize = maxImageSize; }
        public String[] getAllowedDocumentTypes() { return allowedDocumentTypes; }
        public void setAllowedDocumentTypes(String[] allowedDocumentTypes) { this.allowedDocumentTypes = allowedDocumentTypes; }
        public String[] getAllowedImageTypes() { return allowedImageTypes; }
        public void setAllowedImageTypes(String[] allowedImageTypes) { this.allowedImageTypes = allowedImageTypes; }
    }

    /**
     * Email configuration
     */
    public static class EmailConfig {
        private String fromAddress;
        private String supportAddress;
        private String companyName = "Treasure Hunt Adventures";
        private boolean mockEnabled = false;
        private int retryAttempts = 3;
        private long retryDelayMs = 5000;

        // Getters and setters
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        public String getSupportAddress() { return supportAddress; }
        public void setSupportAddress(String supportAddress) { this.supportAddress = supportAddress; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    }

    /**
     * Security configuration
     */
    public static class SecurityConfig {
        private AdminConfig admin = new AdminConfig();
        private SessionConfig session = new SessionConfig();
        private FileSecurityConfig fileSecurity = new FileSecurityConfig();

        public static class AdminConfig {
            private String username;
            private String password;
            private int sessionTimeoutMinutes = 30;

            // Getters and setters
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
            public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
            public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
        }

        public static class SessionConfig {
            private int maxConcurrentSessions = 1;
            private boolean preventLogin = true;
            private int timeoutMinutes = 30;

            // Getters and setters
            public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
            public void setMaxConcurrentSessions(int maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
            public boolean isPreventLogin() { return preventLogin; }
            public void setPreventLogin(boolean preventLogin) { this.preventLogin = preventLogin; }
            public int getTimeoutMinutes() { return timeoutMinutes; }
            public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
        }

        public static class FileSecurityConfig {
            private boolean virusScanEnabled = false;
            private long maxScanSizeBytes = 10 * 1024 * 1024; // 10MB
            private boolean quarantineEnabled = true;
            private String quarantineDir = "quarantine";

            // Getters and setters
            public boolean isVirusScanEnabled() { return virusScanEnabled; }
            public void setVirusScanEnabled(boolean virusScanEnabled) { this.virusScanEnabled = virusScanEnabled; }
            public long getMaxScanSizeBytes() { return maxScanSizeBytes; }
            public void setMaxScanSizeBytes(long maxScanSizeBytes) { this.maxScanSizeBytes = maxScanSizeBytes; }
            public boolean isQuarantineEnabled() { return quarantineEnabled; }
            public void setQuarantineEnabled(boolean quarantineEnabled) { this.quarantineEnabled = quarantineEnabled; }
            public String getQuarantineDir() { return quarantineDir; }
            public void setQuarantineDir(String quarantineDir) { this.quarantineDir = quarantineDir; }
        }

        // Getters and setters
        public AdminConfig getAdmin() { return admin; }
        public void setAdmin(AdminConfig admin) { this.admin = admin; }
        public SessionConfig getSession() { return session; }
        public void setSession(SessionConfig session) { this.session = session; }
        public FileSecurityConfig getFileSecurity() { return fileSecurity; }
        public void setFileSecurity(FileSecurityConfig fileSecurity) { this.fileSecurity = fileSecurity; }
    }

    /**
     * Performance configuration
     */
    public static class PerformanceConfig {
        private CacheConfig cache = new CacheConfig();
        private DatabaseConfig database = new DatabaseConfig();
        private AsyncConfig async = new AsyncConfig();

        public static class CacheConfig {
            private boolean enabled = true;
            private int treasureHuntPlansTtlSeconds = 300;
            private int featuredPlanTtlSeconds = 600;
            private int appSettingsTtlSeconds = 1800;

            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public int getTreasureHuntPlansTtlSeconds() { return treasureHuntPlansTtlSeconds; }
            public void setTreasureHuntPlansTtlSeconds(int treasureHuntPlansTtlSeconds) { this.treasureHuntPlansTtlSeconds = treasureHuntPlansTtlSeconds; }
            public int getFeaturedPlanTtlSeconds() { return featuredPlanTtlSeconds; }
            public void setFeaturedPlanTtlSeconds(int featuredPlanTtlSeconds) { this.featuredPlanTtlSeconds = featuredPlanTtlSeconds; }
            public int getAppSettingsTtlSeconds() { return appSettingsTtlSeconds; }
            public void setAppSettingsTtlSeconds(int appSettingsTtlSeconds) { this.appSettingsTtlSeconds = appSettingsTtlSeconds; }
        }

        public static class DatabaseConfig {
            private int maxPoolSize = 20;
            private int minIdle = 5;
            private long connectionTimeoutMs = 30000;
            private long idleTimeoutMs = 600000;

            // Getters and setters
            public int getMaxPoolSize() { return maxPoolSize; }
            public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
            public int getMinIdle() { return minIdle; }
            public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
            public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
            public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
            public long getIdleTimeoutMs() { return idleTimeoutMs; }
            public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
        }

        public static class AsyncConfig {
            private int emailCorePoolSize = 3;
            private int emailMaxPoolSize = 8;
            private int emailQueueCapacity = 200;
            private int emailKeepAliveSeconds = 60;

            // Getters and setters
            public int getEmailCorePoolSize() { return emailCorePoolSize; }
            public void setEmailCorePoolSize(int emailCorePoolSize) { this.emailCorePoolSize = emailCorePoolSize; }
            public int getEmailMaxPoolSize() { return emailMaxPoolSize; }
            public void setEmailMaxPoolSize(int emailMaxPoolSize) { this.emailMaxPoolSize = emailMaxPoolSize; }
            public int getEmailQueueCapacity() { return emailQueueCapacity; }
            public void setEmailQueueCapacity(int emailQueueCapacity) { this.emailQueueCapacity = emailQueueCapacity; }
            public int getEmailKeepAliveSeconds() { return emailKeepAliveSeconds; }
            public void setEmailKeepAliveSeconds(int emailKeepAliveSeconds) { this.emailKeepAliveSeconds = emailKeepAliveSeconds; }
        }

        // Getters and setters
        public CacheConfig getCache() { return cache; }
        public void setCache(CacheConfig cache) { this.cache = cache; }
        public DatabaseConfig getDatabase() { return database; }
        public void setDatabase(DatabaseConfig database) { this.database = database; }
        public AsyncConfig getAsync() { return async; }
        public void setAsync(AsyncConfig async) { this.async = async; }
    }

    /**
     * Business configuration
     */
    public static class BusinessConfig {
        private int minParticipantAge = 18;
        private int maxTeamSize = 6;
        private int minTeamSize = 2;
        private int registrationTimeoutHours = 24;
        private boolean allowCancellation = true;
        private int cancellationDeadlineHours = 48;

        // Getters and setters
        public int getMinParticipantAge() { return minParticipantAge; }
        public void setMinParticipantAge(int minParticipantAge) { this.minParticipantAge = minParticipantAge; }
        public int getMaxTeamSize() { return maxTeamSize; }
        public void setMaxTeamSize(int maxTeamSize) { this.maxTeamSize = maxTeamSize; }
        public int getMinTeamSize() { return minTeamSize; }
        public void setMinTeamSize(int minTeamSize) { this.minTeamSize = minTeamSize; }
        public int getRegistrationTimeoutHours() { return registrationTimeoutHours; }
        public void setRegistrationTimeoutHours(int registrationTimeoutHours) { this.registrationTimeoutHours = registrationTimeoutHours; }
        public boolean isAllowCancellation() { return allowCancellation; }
        public void setAllowCancellation(boolean allowCancellation) { this.allowCancellation = allowCancellation; }
        public int getCancellationDeadlineHours() { return cancellationDeadlineHours; }
        public void setCancellationDeadlineHours(int cancellationDeadlineHours) { this.cancellationDeadlineHours = cancellationDeadlineHours; }
    }

    /**
     * Monitoring configuration
     */
    public static class MonitoringConfig {
        private boolean enabled = true;
        private int intervalSeconds = 300;
        private int memoryWarningThreshold = 80;
        private int memoryCriticalThreshold = 90;
        private int threadWarningThreshold = 100;
        private int threadCriticalThreshold = 200;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public int getMemoryWarningThreshold() { return memoryWarningThreshold; }
        public void setMemoryWarningThreshold(int memoryWarningThreshold) { this.memoryWarningThreshold = memoryWarningThreshold; }
        public int getMemoryCriticalThreshold() { return memoryCriticalThreshold; }
        public void setMemoryCriticalThreshold(int memoryCriticalThreshold) { this.memoryCriticalThreshold = memoryCriticalThreshold; }
        public int getThreadWarningThreshold() { return threadWarningThreshold; }
        public void setThreadWarningThreshold(int threadWarningThreshold) { this.threadWarningThreshold = threadWarningThreshold; }
        public int getThreadCriticalThreshold() { return threadCriticalThreshold; }
        public void setThreadCriticalThreshold(int threadCriticalThreshold) { this.threadCriticalThreshold = threadCriticalThreshold; }
    }

    // Main getters and setters
    public FileStorageConfig getFileStorage() { return fileStorage; }
    public void setFileStorage(FileStorageConfig fileStorage) { this.fileStorage = fileStorage; }
    public EmailConfig getEmail() { return email; }
    public void setEmail(EmailConfig email) { this.email = email; }
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    public PerformanceConfig getPerformance() { return performance; }
    public void setPerformance(PerformanceConfig performance) { this.performance = performance; }
    public BusinessConfig getBusiness() { return business; }
    public void setBusiness(BusinessConfig business) { this.business = business; }
    public MonitoringConfig getMonitoring() { return monitoring; }
    public void setMonitoring(MonitoringConfig monitoring) { this.monitoring = monitoring; }

    // Validation methods
    private void validateFileStorageConfig() {
        if (fileStorage.getUploadDir() == null || fileStorage.getUploadDir().trim().isEmpty()) {
            throw new IllegalStateException("File storage upload directory must be configured");
        }
        if (fileStorage.getMaxFileSize() <= 0) {
            throw new IllegalStateException("Maximum file size must be positive");
        }
        logger.debug("✅ File storage configuration validated");
    }

    private void validateEmailConfig() {
        if (email.getFromAddress() == null || email.getFromAddress().trim().isEmpty()) {
            throw new IllegalStateException("Email from address must be configured");
        }
        if (email.getSupportAddress() == null || email.getSupportAddress().trim().isEmpty()) {
            throw new IllegalStateException("Email support address must be configured");
        }
        logger.debug("✅ Email configuration validated");
    }

    private void validateSecurityConfig() {
        if (security.getAdmin().getUsername() == null || security.getAdmin().getUsername().trim().isEmpty()) {
            throw new IllegalStateException("Admin username must be configured");
        }
        if (security.getAdmin().getPassword() == null || security.getAdmin().getPassword().trim().isEmpty()) {
            throw new IllegalStateException("Admin password must be configured");
        }
        logger.debug("✅ Security configuration validated");
    }

    private void validatePerformanceConfig() {
        try {
            if (performance.getDatabase().getMaxPoolSize() <= 0) {
                logger.warn("Database max pool size is invalid, using default");
                performance.getDatabase().setMaxPoolSize(10);
            }
            if (performance.getAsync().getEmailCorePoolSize() <= 0) {
                logger.warn("Email core pool size is invalid, using default");
                performance.getAsync().setEmailCorePoolSize(2);
            }
            logger.debug("✅ Performance configuration validated");
        } catch (Exception e) {
            logger.warn("Performance config validation failed, using defaults: {}", e.getMessage());
        }
    }

    private void validateBusinessConfig() {
        if (business.getMinParticipantAge() < 0) {
            throw new IllegalStateException("Minimum participant age cannot be negative");
        }
        if (business.getMaxTeamSize() <= business.getMinTeamSize()) {
            throw new IllegalStateException("Maximum team size must be greater than minimum team size");
        }
        logger.debug("✅ Business configuration validated");
    }

    private void validateMonitoringConfig() {
        if (monitoring.getIntervalSeconds() <= 0) {
            throw new IllegalStateException("Monitoring interval must be positive");
        }
        if (monitoring.getMemoryWarningThreshold() >= monitoring.getMemoryCriticalThreshold()) {
            throw new IllegalStateException("Memory warning threshold must be less than critical threshold");
        }
        logger.debug("✅ Monitoring configuration validated");
    }

    private void logConfigurationSummary() {
        logger.info("📋 Application Configuration Summary:");
        logger.info("  File Storage: uploadDir={}, maxFileSize={}MB", 
                   fileStorage.getUploadDir(), fileStorage.getMaxFileSize() / 1024 / 1024);
        logger.info("  Email: from={}, support={}, mock={}", 
                   fileStorage.getUploadDir(), email.getSupportAddress(), email.isMockEnabled());
        logger.info("  Security: sessionTimeout={}min, maxSessions={}", 
                   security.getSession().getTimeoutMinutes(), security.getSession().getMaxConcurrentSessions());
        logger.info("  Performance: dbPoolSize={}, cacheEnabled={}", 
                   performance.getDatabase().getMaxPoolSize(), performance.getCache().isEnabled());
        logger.info("  Business: minAge={}, maxTeamSize={}, allowCancellation={}", 
                   business.getMinParticipantAge(), business.getMaxTeamSize(), business.isAllowCancellation());
        logger.info("  Monitoring: enabled={}, interval={}s", 
                   monitoring.isEnabled(), monitoring.getIntervalSeconds());
    }

    /**
     * Get configuration as map for external systems
     */
    public Map<String, Object> getConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("fileStorage", fileStorage);
        config.put("email", email);
        config.put("security", security);
        config.put("performance", performance);
        config.put("business", business);
        config.put("monitoring", monitoring);
        return config;
    }

    /**
     * Check if configuration is valid
     */
    public boolean isConfigurationValid() {
        try {
            validateConfiguration();
            return true;
        } catch (Exception e) {
            logger.error("Configuration validation failed: {}", e.getMessage());
            return false;
        }
    }
}
