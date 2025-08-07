package com.treasurehunt.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to load environment variables from .env file
 */
@Configuration
public class EnvironmentConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);
    
    @PostConstruct
    public void loadEnvironmentVariables() {
        try {
            // Load .env file from the root directory
            Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
            
            // Set system properties from .env file
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Only set if not already set as system property
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    // Never log environment variable values for security
                    logger.debug("Loaded environment variable: {} = ***SECURED***", key);
                }
            });
            
            logger.info("Environment variables loaded successfully from .env file");
            
            // Log Gmail configuration (without password)
            String gmailUsername = System.getProperty("GMAIL_USERNAME");
            if (gmailUsername != null) {
                logger.info("Gmail SMTP configured for: {}", gmailUsername);
            }
            
        } catch (Exception e) {
            logger.warn("Could not load .env file: {}", e.getMessage());
            logger.info("Using default configuration values");
        }
    }
}
