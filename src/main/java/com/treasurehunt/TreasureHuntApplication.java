package com.treasurehunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Main Spring Boot application class for Treasure Hunt Registration System
 * 
 * Features:
 * - Web-based registration system for treasure hunt events
 * - Multi-step registration form with file uploads
 * - Email notifications and confirmations
 * - Admin panel for managing plans and registrations
 * - PostgreSQL database integration
 * - Spring Security for admin authentication
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
public class TreasureHuntApplication {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntApplication.class);

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        // Load .env file BEFORE Spring Boot starts
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

            // Set environment variables from .env file
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                // Only log the key name for security - never log values
                System.out.println("🔧 Loaded env var: " + entry.getKey() + " = [SECURED]");
            });

            System.out.println("✅ Environment variables loaded from .env file");
        } catch (Exception e) {
            System.err.println("⚠️ Could not load .env file: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("🚀 Starting Spring Boot application...");
            SpringApplication.run(TreasureHuntApplication.class, args);
            System.out.println("✅ Spring Boot application started successfully");
        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Spring Boot application failed to start");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            logger.info("🚀 Active Profile(s): {}", String.join(", ", activeProfiles));
        } else {
            logger.info("🚀 No active profiles - using default configuration");
        }
    }
}
