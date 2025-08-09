package com.treasurehunt.config;

import com.treasurehunt.event.TreasureHuntEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Application startup configuration
 * Handles initialization tasks when the application starts
 */
@Component
@Profile({"!test", "!junit"})
public class ApplicationStartupConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupConfig.class);

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String uploadDir;

    @Value("${app.file-storage.image-dir:uploads/images}")
    private String imageDir;

    @Value("${app.file-storage.temp-dir:uploads/temp}")
    private String tempDir;

    private final ApplicationConfigurationManager configManager;
    private final TreasureHuntEventPublisher eventPublisher;

    @Autowired
    public ApplicationStartupConfig(ApplicationConfigurationManager configManager,
                                   TreasureHuntEventPublisher eventPublisher) {
        this.configManager = configManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 Starting Treasure Hunt Application...");
        logger.info("📋 Version: {}", appVersion);
        logger.info("🏷️ Profile: {}", activeProfile);

        try {
            // Create necessary directories
            createDirectories();

            // Validate configuration
            validateConfiguration();

            // Publish application started event
            eventPublisher.publishApplicationStarted(appVersion, activeProfile);

            logger.info("✅ Treasure Hunt Application started successfully!");
            logger.info("🌐 Application is ready to accept requests");
            
            if ("dev".equals(activeProfile)) {
                logger.info("🔧 Development mode active");
                logger.info("📊 H2 Console available at: http://localhost:8080/h2-console");
                logger.info("📚 API Documentation available at: http://localhost:8080/swagger-ui.html");
                logger.info("🏥 Health Check available at: http://localhost:8080/api/health");
            }

        } catch (Exception e) {
            logger.error("❌ Failed to start Treasure Hunt Application", e);
            throw e;
        }
    }

    /**
     * Create necessary directories for file storage
     */
    private void createDirectories() {
        logger.info("📁 Creating necessary directories...");

        createDirectory(uploadDir, "Upload directory");
        createDirectory(imageDir, "Image directory");
        createDirectory(tempDir, "Temporary directory");

        logger.info("✅ All directories created successfully");
    }

    /**
     * Create a directory if it doesn't exist
     */
    private void createDirectory(String dirPath, String description) {
        try {
            File directory = new File(dirPath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    logger.info("📁 Created {}: {}", description, dirPath);
                } else {
                    logger.warn("⚠️ Failed to create {}: {}", description, dirPath);
                }
            } else {
                logger.debug("📁 {} already exists: {}", description, dirPath);
            }
        } catch (Exception e) {
            logger.error("❌ Error creating {}: {}", description, e.getMessage());
        }
    }

    /**
     * Validate application configuration
     */
    private void validateConfiguration() {
        logger.info("🔍 Validating application configuration...");

        try {
            boolean isValid = configManager.isConfigurationValid();
            if (isValid) {
                logger.info("✅ Configuration validation passed");
            } else {
                logger.error("❌ Configuration validation failed");
                throw new IllegalStateException("Invalid application configuration");
            }
        } catch (Exception e) {
            logger.error("❌ Configuration validation error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Log startup information
     */
    private void logStartupInfo() {
        logger.info("=".repeat(60));
        logger.info("🎯 TREASURE HUNT APPLICATION");
        logger.info("=".repeat(60));
        logger.info("Version: {}", appVersion);
        logger.info("Profile: {}", activeProfile);
        logger.info("Upload Directory: {}", uploadDir);
        logger.info("Image Directory: {}", imageDir);
        logger.info("Temp Directory: {}", tempDir);
        logger.info("=".repeat(60));
    }
}
