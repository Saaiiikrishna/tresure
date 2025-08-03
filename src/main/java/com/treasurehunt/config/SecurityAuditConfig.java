package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Security audit configuration to validate production readiness
 * ONLY RUNS IN PRODUCTION PROFILE
 */
@Configuration
@Profile("production")
public class SecurityAuditConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditConfig.class);

    private final Environment environment;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public SecurityAuditConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Perform security audit on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void performSecurityAudit() {
        logger.info("=== SECURITY AUDIT STARTING ===");
        
        boolean isProduction = "production".equals(activeProfile);
        boolean hasSecurityIssues = false;

        // Check for hardcoded credentials
        hasSecurityIssues |= checkHardcodedCredentials();
        
        // Check production-specific settings
        if (isProduction) {
            hasSecurityIssues |= checkProductionSettings();
        }
        
        // Check logging configuration
        hasSecurityIssues |= checkLoggingConfiguration();
        
        // Check error handling configuration
        hasSecurityIssues |= checkErrorHandling();

        if (hasSecurityIssues && isProduction) {
            logger.error("❌ CRITICAL: Security issues detected in production environment!");
            throw new IllegalStateException("Security audit failed - application not ready for production");
        } else if (hasSecurityIssues) {
            logger.warn("⚠️  Security issues detected in development environment");
        } else {
            logger.info("✅ Security audit passed");
        }
        
        logger.info("=== SECURITY AUDIT COMPLETED ===");
    }

    /**
     * Check for hardcoded credentials in configuration
     */
    private boolean checkHardcodedCredentials() {
        boolean hasIssues = false;

        // Check database password - verify it's loaded from environment
        String dbPassword = environment.getProperty("spring.datasource.password");
        String dbPasswordEnv = System.getenv("DB_PASSWORD");
        if (dbPassword != null && !dbPassword.trim().isEmpty()) {
            if (dbPasswordEnv == null || dbPasswordEnv.trim().isEmpty()) {
                logger.warn("⚠️  Database password not loaded from environment variable");
                hasIssues = true;
            } else {
                logger.debug("✅ Database password loaded from environment variable DB_PASSWORD");
            }
        }

        // Check mail password - verify it's loaded from environment
        String mailPassword = environment.getProperty("spring.mail.password");
        String mailPasswordEnv = System.getenv("MAIL_PASSWORD");
        if (mailPassword != null && !mailPassword.trim().isEmpty()) {
            if (mailPasswordEnv == null || mailPasswordEnv.trim().isEmpty()) {
                logger.warn("⚠️  Mail password not loaded from environment variable");
                hasIssues = true;
            } else {
                logger.debug("✅ Mail password loaded from environment variable MAIL_PASSWORD");
            }
        }

        // Check admin password - verify it's not default
        String adminPassword = environment.getProperty("app.security.admin.password");
        if ("admin123".equals(adminPassword) || "password".equals(adminPassword) || "admin".equals(adminPassword)) {
            logger.warn("⚠️  Default admin password detected - change immediately!");
            hasIssues = true;
        } else if (adminPassword != null && !adminPassword.trim().isEmpty()) {
            logger.debug("✅ Admin password configured");
        }

        return hasIssues;
    }

    /**
     * Check if a value appears to be hardcoded (not from environment variable)
     */
    private boolean isHardcodedValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Check if it starts with placeholder syntax
        if (value.startsWith("${") && value.endsWith("}")) {
            return false;
        }

        // Check if it's a common placeholder pattern
        if (value.contains("${") || value.contains("ENV:") || value.contains("env.")) {
            return false;
        }

        return true;
    }

    /**
     * Check production-specific security settings
     */
    private boolean checkProductionSettings() {
        boolean hasIssues = false;
        
        // Check H2 console
        Boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);
        if (Boolean.TRUE.equals(h2ConsoleEnabled)) {
            logger.error("❌ H2 console is enabled in production!");
            hasIssues = true;
        }
        
        // Check SQL logging
        Boolean showSql = environment.getProperty("spring.jpa.show-sql", Boolean.class);
        if (Boolean.TRUE.equals(showSql)) {
            logger.error("❌ SQL logging is enabled in production!");
            hasIssues = true;
        }
        
        // Check Thymeleaf caching
        Boolean thymeleafCache = environment.getProperty("spring.thymeleaf.cache", Boolean.class);
        if (Boolean.FALSE.equals(thymeleafCache)) {
            logger.warn("⚠️  Thymeleaf caching is disabled in production");
            hasIssues = true;
        }
        
        return hasIssues;
    }

    /**
     * Check logging configuration for security
     */
    private boolean checkLoggingConfiguration() {
        boolean hasIssues = false;
        
        // Check if debug logging is enabled for security packages
        String securityLogLevel = environment.getProperty("logging.level.org.springframework.security");
        if ("DEBUG".equalsIgnoreCase(securityLogLevel)) {
            logger.warn("⚠️  Debug logging enabled for Spring Security");
            hasIssues = true;
        }
        
        return hasIssues;
    }

    /**
     * Check error handling configuration
     */
    private boolean checkErrorHandling() {
        boolean hasIssues = false;
        
        // Check error message exposure
        String includeMessage = environment.getProperty("server.error.include-message");
        if ("always".equals(includeMessage)) {
            logger.warn("⚠️  Error messages are exposed to users");
            hasIssues = true;
        }
        
        // Check binding error exposure
        String includeBindingErrors = environment.getProperty("server.error.include-binding-errors");
        if ("always".equals(includeBindingErrors)) {
            logger.warn("⚠️  Binding errors are exposed to users");
            hasIssues = true;
        }
        
        // Check stack trace exposure
        String includeStacktrace = environment.getProperty("server.error.include-stacktrace");
        if ("always".equals(includeStacktrace)) {
            logger.error("❌ Stack traces are exposed to users!");
            hasIssues = true;
        }
        
        return hasIssues;
    }
}
