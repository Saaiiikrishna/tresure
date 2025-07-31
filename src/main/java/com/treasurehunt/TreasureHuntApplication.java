package com.treasurehunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

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

    public static void main(String[] args) {
        SpringApplication.run(TreasureHuntApplication.class, args);
    }
}
