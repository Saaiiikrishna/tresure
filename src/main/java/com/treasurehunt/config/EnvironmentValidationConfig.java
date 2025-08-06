package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required environment variables are properly configured
 * Prevents application startup with missing critical configuration
 */
@Component
public class EnvironmentValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentValidationConfig.class);

    private final Environment environment;

    // Required environment variables for security
    @Value("${ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${MAIL_USERNAME:}")
    private String mailUsername;

    @Value("${MAIL_PASSWORD:}")
    private String mailPassword;

    @Value("${MAIL_FROM_ADDRESS:}")
    private String mailFromAddress;

    @Value("${MAIL_SUPPORT_ADDRESS:}")
    private String mailSupportAddress;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    public EnvironmentValidationConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validate all required environment variables on application startup
     * Fails fast if critical configuration is missing
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateRequiredEnvironmentVariables() {
        logger.info("🔍 Validating required environment variables...");

        List<String> missingVariables = new ArrayList<>();
        List<String> insecureVariables = new ArrayList<>();

        // Validate admin credentials
        if (!StringUtils.hasText(adminUsername)) {
            missingVariables.add("ADMIN_USERNAME");
        }
        if (!StringUtils.hasText(adminPassword)) {
            missingVariables.add("ADMIN_PASSWORD");
        } else if (isWeakPassword(adminPassword)) {
            insecureVariables.add("ADMIN_PASSWORD (weak password detected)");
        }

        // Validate email configuration
        if (!StringUtils.hasText(mailUsername)) {
            missingVariables.add("MAIL_USERNAME");
        }
        if (!StringUtils.hasText(mailPassword)) {
            missingVariables.add("MAIL_PASSWORD");
        }
        if (!StringUtils.hasText(mailFromAddress)) {
            missingVariables.add("MAIL_FROM_ADDRESS");
        }
        if (!StringUtils.hasText(mailSupportAddress)) {
            missingVariables.add("MAIL_SUPPORT_ADDRESS");
        }

        // Validate database configuration (only in production)
        String activeProfile = environment.getProperty("spring.profiles.active", "default");
        if ("production".equals(activeProfile)) {
            if (!StringUtils.hasText(dbPassword)) {
                missingVariables.add("DB_PASSWORD");
            }
        }

        // Check for missing variables
        if (!missingVariables.isEmpty()) {
            logger.error("❌ CRITICAL: Missing required environment variables:");
            missingVariables.forEach(var -> logger.error("   - {}", var));
            logger.error("❌ Application cannot start without these environment variables");
            throw new IllegalStateException(
                "Missing required environment variables: " + String.join(", ", missingVariables) +
                ". Please set these environment variables and restart the application."
            );
        }

        // Check for insecure variables
        if (!insecureVariables.isEmpty()) {
            logger.warn("⚠️ SECURITY WARNING: Insecure configuration detected:");
            insecureVariables.forEach(var -> logger.warn("   - {}", var));
            logger.warn("⚠️ Please update these configurations for better security");
        }

        // Validate email format
        if (!isValidEmail(mailFromAddress)) {
            logger.error("❌ MAIL_FROM_ADDRESS is not a valid email format: {}", mailFromAddress);
            throw new IllegalStateException("MAIL_FROM_ADDRESS must be a valid email address");
        }

        if (!isValidEmail(mailSupportAddress)) {
            logger.error("❌ MAIL_SUPPORT_ADDRESS is not a valid email format: {}", mailSupportAddress);
            throw new IllegalStateException("MAIL_SUPPORT_ADDRESS must be a valid email address");
        }

        logger.info("✅ All required environment variables are properly configured");
        logger.info("🚀 Application security validation passed");
    }

    /**
     * Check if password is weak/default
     */
    private boolean isWeakPassword(String password) {
        if (password.length() < 8) {
            return true;
        }

        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        return lowerPassword.equals("admin") ||
               lowerPassword.equals("password") ||
               lowerPassword.equals("admin123") ||
               lowerPassword.equals("change_me") ||
               lowerPassword.equals("changeme") ||
               lowerPassword.equals("123456") ||
               lowerPassword.equals("12345678");
    }

    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Get configuration summary for logging (without sensitive values)
     */
    public void logConfigurationSummary() {
        logger.info("📋 Configuration Summary:");
        logger.info("   - Admin Username: {}", StringUtils.hasText(adminUsername) ? "✅ Configured" : "❌ Missing");
        logger.info("   - Admin Password: {}", StringUtils.hasText(adminPassword) ? "✅ Configured" : "❌ Missing");
        logger.info("   - Mail Username: {}", StringUtils.hasText(mailUsername) ? "✅ Configured" : "❌ Missing");
        logger.info("   - Mail Password: {}", StringUtils.hasText(mailPassword) ? "✅ Configured" : "❌ Missing");
        logger.info("   - Mail From Address: {}", StringUtils.hasText(mailFromAddress) ? mailFromAddress : "❌ Missing");
        logger.info("   - Mail Support Address: {}", StringUtils.hasText(mailSupportAddress) ? mailSupportAddress : "❌ Missing");
        
        String activeProfile = environment.getProperty("spring.profiles.active", "default");
        logger.info("   - Active Profile: {}", activeProfile);
        
        if ("production".equals(activeProfile)) {
            logger.info("   - Database Password: {}", StringUtils.hasText(dbPassword) ? "✅ Configured" : "❌ Missing");
        }
    }
}
