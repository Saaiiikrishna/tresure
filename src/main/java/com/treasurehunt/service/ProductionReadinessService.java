package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Production Readiness Service
 * Comprehensive validation service that checks all aspects of production readiness
 */
@Service
public class ProductionReadinessService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionReadinessService.class);

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    private final Map<String, Boolean> readinessChecks = new HashMap<>();

    /**
     * Perform comprehensive production readiness check
     */
    @EventListener(ApplicationReadyEvent.class)
    public void performProductionReadinessCheck() {
        String activeProfile = environment.getProperty("spring.profiles.active", "default");
        
        if (!"production".equals(activeProfile)) {
            logger.info("Skipping production readiness check - not in production profile");
            return;
        }

        logger.info("=== PRODUCTION READINESS CHECK STARTED ===");
        
        List<String> criticalIssues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Perform all readiness checks
        checkSecurityConfiguration(criticalIssues, warnings);
        checkDatabaseConfiguration(criticalIssues, warnings);
        checkEmailConfiguration(criticalIssues, warnings);
        checkFileStorageConfiguration(criticalIssues, warnings);
        checkLoggingConfiguration(criticalIssues, warnings);
        checkPerformanceConfiguration(criticalIssues, warnings);
        checkMonitoringConfiguration(criticalIssues, warnings);
        checkEnvironmentVariables(criticalIssues, warnings);
        
        // Generate readiness report
        generateReadinessReport(criticalIssues, warnings);
        
        logger.info("=== PRODUCTION READINESS CHECK COMPLETED ===");
    }

    /**
     * Check security configuration
     */
    private void checkSecurityConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking security configuration...");
        
        // Admin credentials
        String adminUsername = environment.getProperty("app.security.admin.username");
        String adminPassword = environment.getProperty("app.security.admin.password");
        
        if ("admin".equals(adminUsername)) {
            criticalIssues.add("Default admin username is being used");
        }
        
        if (adminPassword == null || adminPassword.length() < 12) {
            criticalIssues.add("Admin password is weak or not set");
        }
        
        // SSL/HTTPS
        boolean sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        if (!sslEnabled) {
            warnings.add("SSL/HTTPS is not enabled");
        }
        
        // Error exposure
        String includeStacktrace = environment.getProperty("server.error.include-stacktrace");
        if ("always".equals(includeStacktrace)) {
            criticalIssues.add("Stack traces are exposed to users");
        }
        
        readinessChecks.put("security", criticalIssues.isEmpty());
    }

    /**
     * Check database configuration
     */
    private void checkDatabaseConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking database configuration...");
        
        try {
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    criticalIssues.add("Database connection is not valid");
                }
            }
            
            // Check DDL settings
            String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
            if ("create-drop".equals(ddlAuto) || "create".equals(ddlAuto)) {
                criticalIssues.add("Dangerous DDL auto setting in production: " + ddlAuto);
            }
            
            // Check SQL logging
            boolean showSql = environment.getProperty("spring.jpa.show-sql", Boolean.class, false);
            if (showSql) {
                criticalIssues.add("SQL logging is enabled in production");
            }
            
            // Check H2 console
            boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, false);
            if (h2ConsoleEnabled) {
                criticalIssues.add("H2 console is enabled in production");
            }
            
        } catch (Exception e) {
            criticalIssues.add("Database connectivity check failed: " + e.getMessage());
        }
        
        readinessChecks.put("database", criticalIssues.stream().noneMatch(issue -> issue.contains("Database") || issue.contains("DDL") || issue.contains("SQL") || issue.contains("H2")));
    }

    /**
     * Check email configuration
     */
    private void checkEmailConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking email configuration...");
        
        String mailHost = environment.getProperty("spring.mail.host");
        String mailUsername = environment.getProperty("spring.mail.username");
        String mailPassword = environment.getProperty("spring.mail.password");
        
        if (mailHost == null || mailHost.trim().isEmpty()) {
            warnings.add("Mail host is not configured");
        }
        
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            warnings.add("Mail username is not configured");
        }
        
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            warnings.add("Mail password is not configured");
        }
        
        // Check mock email service
        boolean mockEmailEnabled = environment.getProperty("app.email.mock.enabled", Boolean.class, false);
        if (mockEmailEnabled) {
            criticalIssues.add("Mock email service is enabled in production");
        }
        
        readinessChecks.put("email", !mockEmailEnabled);
    }

    /**
     * Check file storage configuration
     */
    private void checkFileStorageConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking file storage configuration...");
        
        String uploadDir = environment.getProperty("app.file-storage.upload-dir");
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            criticalIssues.add("File upload directory is not configured");
        }
        
        // Check file size limits
        Long maxFileSize = environment.getProperty("spring.servlet.multipart.max-file-size", Long.class);
        if (maxFileSize == null || maxFileSize > 10 * 1024 * 1024) { // 10MB
            warnings.add("File upload size limit is very high or not set");
        }
        
        readinessChecks.put("fileStorage", uploadDir != null && !uploadDir.trim().isEmpty());
    }

    /**
     * Check logging configuration
     */
    private void checkLoggingConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking logging configuration...");
        
        String logFile = environment.getProperty("logging.file.name");
        if (logFile == null || logFile.trim().isEmpty()) {
            warnings.add("Log file is not configured - logs may be lost on restart");
        }
        
        String rootLogLevel = environment.getProperty("logging.level.root");
        if ("DEBUG".equalsIgnoreCase(rootLogLevel) || "TRACE".equalsIgnoreCase(rootLogLevel)) {
            warnings.add("Debug/Trace logging is enabled at root level");
        }
        
        readinessChecks.put("logging", logFile != null && !logFile.trim().isEmpty());
    }

    /**
     * Check performance configuration
     */
    private void checkPerformanceConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking performance configuration...");
        
        // Check Thymeleaf caching
        boolean thymeleafCache = environment.getProperty("spring.thymeleaf.cache", Boolean.class, true);
        if (!thymeleafCache) {
            warnings.add("Thymeleaf caching is disabled - performance impact");
        }
        
        // Check connection pool settings
        Integer maxPoolSize = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        if (maxPoolSize == null || maxPoolSize < 5) {
            warnings.add("Database connection pool size is very small");
        }
        
        readinessChecks.put("performance", thymeleafCache);
    }

    /**
     * Check monitoring configuration
     */
    private void checkMonitoringConfiguration(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking monitoring configuration...");
        
        String actuatorEndpoints = environment.getProperty("management.endpoints.web.exposure.include");
        if (actuatorEndpoints != null && actuatorEndpoints.contains("*")) {
            criticalIssues.add("All actuator endpoints are exposed");
        }
        
        String healthDetails = environment.getProperty("management.endpoint.health.show-details");
        if ("always".equals(healthDetails)) {
            warnings.add("Health endpoint shows details to all users");
        }
        
        readinessChecks.put("monitoring", actuatorEndpoints == null || !actuatorEndpoints.contains("*"));
    }

    /**
     * Check required environment variables
     */
    private void checkEnvironmentVariables(List<String> criticalIssues, List<String> warnings) {
        logger.debug("Checking environment variables...");
        
        List<String> requiredEnvVars = List.of(
            "DB_HOST", "DB_USERNAME", "DB_PASSWORD",
            "ADMIN_USERNAME", "ADMIN_PASSWORD"
        );
        
        for (String envVar : requiredEnvVars) {
            String value = System.getenv(envVar);
            if (value == null || value.trim().isEmpty()) {
                criticalIssues.add("Required environment variable not set: " + envVar);
            }
        }
        
        readinessChecks.put("environment", criticalIssues.stream().noneMatch(issue -> issue.contains("environment variable")));
    }

    /**
     * Generate comprehensive readiness report
     */
    private void generateReadinessReport(List<String> criticalIssues, List<String> warnings) {
        logger.info("=== PRODUCTION READINESS REPORT ===");
        
        // Overall readiness score
        long passedChecks = readinessChecks.values().stream().mapToLong(passed -> passed ? 1 : 0).sum();
        double readinessScore = (double) passedChecks / readinessChecks.size() * 100;
        
        logger.info("Overall Readiness Score: {:.1f}% ({}/{} checks passed)", 
                   readinessScore, passedChecks, readinessChecks.size());
        
        // Individual check results
        logger.info("Individual Check Results:");
        readinessChecks.forEach((check, passed) -> 
            logger.info("  {} {}: {}", passed ? "✅" : "❌", check, passed ? "PASS" : "FAIL"));
        
        // Critical issues
        if (!criticalIssues.isEmpty()) {
            logger.error("❌ CRITICAL ISSUES ({}): Application NOT ready for production", criticalIssues.size());
            criticalIssues.forEach(issue -> logger.error("   - {}", issue));
            
            throw new IllegalStateException(
                "Application is not ready for production deployment. " +
                "Critical issues found: " + criticalIssues.size()
            );
        }
        
        // Warnings
        if (!warnings.isEmpty()) {
            logger.warn("⚠️ WARNINGS ({}): Consider addressing these issues", warnings.size());
            warnings.forEach(warning -> logger.warn("   - {}", warning));
        }
        
        // Success
        if (criticalIssues.isEmpty()) {
            if (warnings.isEmpty()) {
                logger.info("🎉 PRODUCTION READY: All checks passed successfully!");
            } else {
                logger.info("✅ PRODUCTION READY: Critical checks passed (with {} warnings)", warnings.size());
            }
        }
    }

    /**
     * Get current readiness status
     */
    public ProductionReadinessStatus getReadinessStatus() {
        long passedChecks = readinessChecks.values().stream().mapToLong(passed -> passed ? 1 : 0).sum();
        double readinessScore = readinessChecks.isEmpty() ? 0 : (double) passedChecks / readinessChecks.size() * 100;
        
        return new ProductionReadinessStatus(
            readinessScore >= 100,
            readinessScore,
            passedChecks,
            readinessChecks.size(),
            new HashMap<>(readinessChecks)
        );
    }

    /**
     * Production readiness status data class
     */
    public static class ProductionReadinessStatus {
        public final boolean ready;
        public final double score;
        public final long passedChecks;
        public final long totalChecks;
        public final Map<String, Boolean> checkResults;

        public ProductionReadinessStatus(boolean ready, double score, long passedChecks, 
                                       long totalChecks, Map<String, Boolean> checkResults) {
            this.ready = ready;
            this.score = score;
            this.passedChecks = passedChecks;
            this.totalChecks = totalChecks;
            this.checkResults = checkResults;
        }
    }
}
