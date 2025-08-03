package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Production validation configuration
 * Validates that all required environment variables are set for production deployment
 */
@Configuration
@Profile("production")
public class ProductionValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProductionValidationConfig.class);

    @Value("${app.security.admin.username:}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Value("${MAIL_FROM_ADDRESS:}")
    private String mailFromAddress;

    @Value("${MAIL_SUPPORT_ADDRESS:}")
    private String mailSupportAddress;

    @Value("${MAIL_USERNAME:}")
    private String mailUsername;

    @Value("${MAIL_PASSWORD:}")
    private String mailPassword;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${DB_NAME:}")
    private String dbName;

    @Value("${COMPANY_NAME:}")
    private String companyName;

    /**
     * Validate production configuration after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfig() {
        logger.info("🔍 Validating production configuration...");
        
        List<String> missingVariables = new ArrayList<>();
        List<String> insecureVariables = new ArrayList<>();

        // Check required environment variables
        if (!StringUtils.hasText(adminUsername)) {
            missingVariables.add("app.security.admin.username");
        }
        if (!StringUtils.hasText(adminPassword)) {
            missingVariables.add("app.security.admin.password");
        } else if ("admin123".equals(adminPassword) || "admin".equals(adminPassword)) {
            insecureVariables.add("app.security.admin.password (using default/weak password)");
        }

        if (!StringUtils.hasText(mailFromAddress)) {
            missingVariables.add("MAIL_FROM_ADDRESS");
        }
        if (!StringUtils.hasText(mailSupportAddress)) {
            missingVariables.add("MAIL_SUPPORT_ADDRESS");
        }
        if (!StringUtils.hasText(mailUsername)) {
            missingVariables.add("MAIL_USERNAME");
        }
        if (!StringUtils.hasText(mailPassword)) {
            missingVariables.add("MAIL_PASSWORD");
        }
        if (!StringUtils.hasText(dbUsername)) {
            missingVariables.add("DB_USERNAME");
        }
        if (!StringUtils.hasText(dbPassword)) {
            missingVariables.add("DB_PASSWORD");
        }
        if (!StringUtils.hasText(dbName)) {
            missingVariables.add("DB_NAME");
        }
        if (!StringUtils.hasText(companyName)) {
            missingVariables.add("COMPANY_NAME");
        }

        // Report validation results
        if (!missingVariables.isEmpty()) {
            logger.error("❌ PRODUCTION VALIDATION FAILED - Missing required environment variables:");
            missingVariables.forEach(var -> logger.error("   - {}", var));
            throw new IllegalStateException("Production deployment requires all environment variables to be set. " +
                    "Missing: " + String.join(", ", missingVariables));
        }

        if (!insecureVariables.isEmpty()) {
            logger.warn("⚠️ SECURITY WARNING - Insecure configuration detected:");
            insecureVariables.forEach(var -> logger.warn("   - {}", var));
        }

        logger.info("✅ Production configuration validation passed");
        logger.info("🚀 Application is ready for production deployment");
    }
}
