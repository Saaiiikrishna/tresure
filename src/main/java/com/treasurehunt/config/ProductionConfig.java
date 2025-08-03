package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executor;

/**
 * Production configuration for security hardening and resource management
 */
@Configuration
@Profile("production")
public class ProductionConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProductionConfig.class);

    @Value("${spring.application.name}")
    private String applicationName;

    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * Configure async task executor with proper resource management
     * This is the PRIMARY task executor for production
     */
    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TreasureHunt-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        this.taskExecutor = executor;
        logger.info("Configured async task executor for production");
        return executor;
    }

    /**
     * Application ready event listener for production validation
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("=== PRODUCTION STARTUP VALIDATION ===");
        
        // Validate critical environment variables
        validateEnvironmentVariables();
        
        // Log production configuration status
        logProductionStatus();
        
        logger.info("=== APPLICATION READY FOR PRODUCTION ===");
    }

    /**
     * Validate that all critical environment variables are set
     */
    private void validateEnvironmentVariables() {
        String[] requiredVars = {
            "DB_USERNAME", "DB_PASSWORD", "MAIL_USERNAME", "MAIL_PASSWORD",
            "ADMIN_USERNAME", "ADMIN_PASSWORD", "MAIL_FROM_ADDRESS", "MAIL_SUPPORT_ADDRESS"
        };
        
        boolean allSet = true;
        for (String var : requiredVars) {
            String value = System.getenv(var);
            if (value == null || value.trim().isEmpty()) {
                logger.error("❌ CRITICAL: Environment variable {} is not set!", var);
                allSet = false;
            } else {
                logger.info("✅ Environment variable {} is configured", var);
            }
        }
        
        if (!allSet) {
            logger.error("❌ CRITICAL: Missing required environment variables for production!");
            throw new IllegalStateException("Missing required environment variables for production deployment");
        }
    }

    /**
     * Log production configuration status
     */
    private void logProductionStatus() {
        logger.info("Application: {}", applicationName);
        logger.info("Profile: production");
        logger.info("Database: PostgreSQL (configured via environment)");
        logger.info("Email: SMTP (configured via environment)");
        logger.info("File uploads: Configured with size limits");
        logger.info("Security: Hardened configuration active");
        logger.info("Logging: Production levels (INFO/WARN)");
        logger.info("Error handling: Secure (no sensitive data exposure)");
        logger.info("Graceful shutdown: Enabled (30s timeout)");
    }

    /**
     * Cleanup resources on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Starting application shutdown cleanup...");
        
        if (taskExecutor != null) {
            logger.info("Shutting down task executor...");
            taskExecutor.shutdown();
        }
        
        logger.info("Application cleanup completed");
    }
}
