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
        SpringApplication.run(TreasureHuntApplication.class, args);
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
