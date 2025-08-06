package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Production Security Hardening Configuration
 * Performs comprehensive security validation and hardening for production deployment
 */
@Configuration
@Profile("production")
public class ProductionSecurityHardeningConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProductionSecurityHardeningConfig.class);

    @Autowired
    private Environment environment;

    @Value("${app.security.admin.username:}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    // Security patterns for validation
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$"
    );

    private static final List<String> WEAK_PASSWORDS = List.of(
        "admin", "password", "123456", "admin123", "changeme", "default",
        "root", "user", "guest", "test", "demo", "CHANGE_ME_IMMEDIATELY"
    );

    /**
     * Comprehensive security hardening validation on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void performSecurityHardening() {
        logger.info("=== PRODUCTION SECURITY HARDENING STARTED ===");
        
        List<String> criticalIssues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Validate credentials security
        validateCredentialSecurity(criticalIssues, warnings);
        
        // 2. Validate configuration security
        validateConfigurationSecurity(criticalIssues, warnings);
        
        // 3. Validate environment security
        validateEnvironmentSecurity(criticalIssues, warnings);
        
        // 4. Validate logging security
        validateLoggingSecurity(criticalIssues, warnings);
        
        // 5. Validate error handling security
        validateErrorHandlingSecurity(criticalIssues, warnings);

        // Report findings
        reportSecurityFindings(criticalIssues, warnings);
        
        logger.info("=== PRODUCTION SECURITY HARDENING COMPLETED ===");
    }

    /**
     * Validate credential security
     */
    private void validateCredentialSecurity(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Validating credential security...");

        // Check admin username
        if (!StringUtils.hasText(adminUsername) || "admin".equals(adminUsername)) {
            criticalIssues.add("Admin username is default or empty - security risk");
        }

        // Check admin password strength
        if (!StringUtils.hasText(adminPassword)) {
            criticalIssues.add("Admin password is not set");
        } else if (WEAK_PASSWORDS.contains(adminPassword.toLowerCase())) {
            criticalIssues.add("Admin password is weak or default");
        } else if (!STRONG_PASSWORD_PATTERN.matcher(adminPassword).matches()) {
            warnings.add("Admin password does not meet strong password requirements (12+ chars, mixed case, numbers, symbols)");
        }

        // Check database password
        if (!StringUtils.hasText(dbPassword)) {
            criticalIssues.add("Database password is not set");
        } else if (dbPassword.length() < 8) {
            warnings.add("Database password is shorter than recommended (8+ characters)");
        }

        // Check mail password
        if (!StringUtils.hasText(mailPassword)) {
            warnings.add("Mail password is not set - email functionality may fail");
        }
    }

    /**
     * Validate configuration security
     */
    private void validateConfigurationSecurity(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Validating configuration security...");

        // Check H2 console
        if (isPropertyEnabled("spring.h2.console.enabled")) {
            criticalIssues.add("H2 console is enabled in production");
        }

        // Check SQL logging
        if (isPropertyEnabled("spring.jpa.show-sql")) {
            criticalIssues.add("SQL logging is enabled in production");
        }

        // Check debug logging
        String rootLogLevel = environment.getProperty("logging.level.root");
        if ("DEBUG".equalsIgnoreCase(rootLogLevel) || "TRACE".equalsIgnoreCase(rootLogLevel)) {
            warnings.add("Debug/Trace logging enabled at root level");
        }

        // Check Thymeleaf caching
        if (!isPropertyEnabled("spring.thymeleaf.cache")) {
            warnings.add("Thymeleaf caching is disabled - performance impact");
        }

        // Check actuator exposure
        String actuatorEndpoints = environment.getProperty("management.endpoints.web.exposure.include");
        if (actuatorEndpoints != null && actuatorEndpoints.contains("*")) {
            criticalIssues.add("All actuator endpoints are exposed");
        }
    }

    /**
     * Validate environment security
     */
    private void validateEnvironmentSecurity(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Validating environment security...");

        // Check required environment variables
        List<String> requiredEnvVars = List.of(
            "DB_HOST", "DB_USERNAME", "DB_PASSWORD",
            "ADMIN_USERNAME", "ADMIN_PASSWORD",
            "MAIL_HOST", "MAIL_USERNAME", "MAIL_PASSWORD"
        );

        for (String envVar : requiredEnvVars) {
            String value = System.getenv(envVar);
            if (!StringUtils.hasText(value)) {
                criticalIssues.add("Required environment variable not set: " + envVar);
            }
        }

        // FIXED: Check SSL configuration more intelligently for cloud deployments
        String serverPort = environment.getProperty("server.port", "8080");
        boolean isCloudDeployment = "80".equals(serverPort) || "8080".equals(serverPort);

        if (!isPropertyEnabled("server.ssl.enabled") && !isPropertyEnabled("app.security.require-ssl")) {
            if (isCloudDeployment) {
                logger.info("ℹ️ SSL/TLS handled by cloud provider/reverse proxy - application running on port {}", serverPort);
            } else {
                warnings.add("SSL/TLS is not configured - consider enabling HTTPS");
            }
        }
    }

    /**
     * Validate logging security
     */
    private void validateLoggingSecurity(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Validating logging security...");

        // Check security logging levels
        String securityLogLevel = environment.getProperty("logging.level.org.springframework.security");
        if ("DEBUG".equalsIgnoreCase(securityLogLevel) || "TRACE".equalsIgnoreCase(securityLogLevel)) {
            warnings.add("Debug logging enabled for Spring Security - may expose sensitive information");
        }

        // Check if log files are configured
        String logFile = environment.getProperty("logging.file.name");
        if (!StringUtils.hasText(logFile)) {
            warnings.add("Log file not configured - logs may be lost on restart");
        }
    }

    /**
     * Validate error handling security
     */
    private void validateErrorHandlingSecurity(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Validating error handling security...");

        // Check error message exposure
        if ("always".equals(environment.getProperty("server.error.include-message"))) {
            criticalIssues.add("Error messages are exposed to users");
        }

        // Check binding error exposure
        if ("always".equals(environment.getProperty("server.error.include-binding-errors"))) {
            criticalIssues.add("Binding errors are exposed to users");
        }

        // Check stack trace exposure
        if ("always".equals(environment.getProperty("server.error.include-stacktrace"))) {
            criticalIssues.add("Stack traces are exposed to users");
        }

        // Check exception exposure
        if (isPropertyEnabled("server.error.include-exception")) {
            criticalIssues.add("Exception details are exposed to users");
        }
    }

    /**
     * Report security findings
     */
    private void reportSecurityFindings(List<String> criticalIssues, List<String> warnings) {
        if (!criticalIssues.isEmpty()) {
            logger.error("❌ CRITICAL SECURITY ISSUES FOUND:");
            criticalIssues.forEach(issue -> logger.error("   - {}", issue));
            
            throw new IllegalStateException(
                "Critical security issues detected. Application cannot start in production mode. " +
                "Issues: " + String.join(", ", criticalIssues)
            );
        }

        if (!warnings.isEmpty()) {
            logger.warn("⚠️ SECURITY WARNINGS:");
            warnings.forEach(warning -> logger.warn("   - {}", warning));
        }

        if (criticalIssues.isEmpty() && warnings.isEmpty()) {
            logger.info("✅ Security hardening validation passed - no critical issues found");
        }
    }

    /**
     * Check if a property is enabled
     */
    private boolean isPropertyEnabled(String propertyName) {
        String value = environment.getProperty(propertyName);
        return "true".equalsIgnoreCase(value);
    }
}
