package com.treasurehunt.controller;

import com.treasurehunt.config.ApplicationConfigurationManager;
import com.treasurehunt.event.TreasureHuntEventListener;
import com.treasurehunt.event.TreasureHuntEventPublisher;
import com.treasurehunt.service.PerformanceMonitoringService;
import com.treasurehunt.service.ThreadSafeEmailProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check and monitoring endpoints
 * Provides comprehensive system health and status information
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Monitoring API", description = "System monitoring and health check endpoints")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final PerformanceMonitoringService performanceMonitoringService;
    private final ThreadSafeEmailProcessor emailProcessor;
    private final TreasureHuntEventPublisher eventPublisher;
    private final TreasureHuntEventListener eventListener;
    private final ApplicationConfigurationManager configManager;

    @Autowired
    public HealthController(DataSource dataSource,
                           PerformanceMonitoringService performanceMonitoringService,
                           ThreadSafeEmailProcessor emailProcessor,
                           TreasureHuntEventPublisher eventPublisher,
                           TreasureHuntEventListener eventListener,
                           ApplicationConfigurationManager configManager) {
        this.dataSource = dataSource;
        this.performanceMonitoringService = performanceMonitoringService;
        this.emailProcessor = emailProcessor;
        this.eventPublisher = eventPublisher;
        this.eventListener = eventListener;
        this.configManager = configManager;
    }

    /**
     * Basic health check endpoint
     */
    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns basic application health status")
    @ApiResponse(responseCode = "200", description = "Application is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Treasure Hunt Application");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with all components
     */
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Returns comprehensive health status of all components")
    @ApiResponse(responseCode = "200", description = "Detailed health information")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Basic info
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Treasure Hunt Application");
        health.put("version", "1.0.0");
        
        // Component health checks
        Map<String, Object> components = new HashMap<>();
        components.put("database", checkDatabaseHealth());
        components.put("email", checkEmailHealth());
        components.put("configuration", checkConfigurationHealth());
        components.put("events", checkEventSystemHealth());
        components.put("performance", checkPerformanceHealth());
        
        health.put("components", components);
        
        // Overall status
        boolean allHealthy = components.values().stream()
            .allMatch(component -> "UP".equals(((Map<?, ?>) component).get("status")));
        health.put("status", allHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Performance metrics endpoint
     */
    @GetMapping("/metrics")
    @Operation(summary = "Performance metrics", description = "Returns current performance metrics")
    @ApiResponse(responseCode = "200", description = "Performance metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get performance metrics
        PerformanceMonitoringService.PerformanceMetrics performanceMetrics = 
            performanceMonitoringService.getCurrentMetrics();
        
        metrics.put("timestamp", LocalDateTime.now());
        metrics.put("memory", Map.of(
            "heapUsed", performanceMetrics.heapUsed,
            "heapMax", performanceMetrics.heapMax,
            "heapUtilization", performanceMetrics.heapUtilization,
            "nonHeapUsed", performanceMetrics.nonHeapUsed
        ));
        
        metrics.put("threads", Map.of(
            "threadCount", performanceMetrics.threadCount,
            "peakThreadCount", performanceMetrics.peakThreadCount,
            "daemonThreadCount", performanceMetrics.daemonThreadCount
        ));
        
        metrics.put("application", Map.of(
            "totalRequests", performanceMetrics.totalRequests,
            "totalErrors", performanceMetrics.totalErrors,
            "slowQueries", performanceMetrics.slowQueries
        ));
        
        metrics.put("system", Map.of(
            "availableProcessors", performanceMetrics.availableProcessors,
            "freeMemory", performanceMetrics.freeMemory,
            "totalMemory", performanceMetrics.totalMemory,
            "maxMemory", performanceMetrics.maxMemory
        ));
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Email system status
     */
    @GetMapping("/email")
    @Operation(summary = "Email system status", description = "Returns email processing status and statistics")
    @ApiResponse(responseCode = "200", description = "Email system status")
    public ResponseEntity<Map<String, Object>> emailStatus() {
        Map<String, Object> status = new HashMap<>();
        
        ThreadSafeEmailProcessor.ProcessingStats emailStats = emailProcessor.getProcessingStats();
        
        status.put("timestamp", LocalDateTime.now());
        status.put("processing", emailStats.isProcessing());
        status.put("totalProcessed", emailStats.getTotalProcessed());
        status.put("totalErrors", emailStats.getTotalErrors());
        status.put("pendingEmails", emailStats.getPendingEmails());
        status.put("failedEmails", emailStats.getFailedEmails());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Event system status
     */
    @GetMapping("/events")
    @Operation(summary = "Event system status", description = "Returns event publishing and handling statistics")
    @ApiResponse(responseCode = "200", description = "Event system status")
    public ResponseEntity<Map<String, Object>> eventStatus() {
        Map<String, Object> status = new HashMap<>();
        
        TreasureHuntEventPublisher.EventPublishingStats publishingStats = eventPublisher.getStats();
        TreasureHuntEventListener.EventHandlingStats handlingStats = eventListener.getStats();
        
        status.put("timestamp", LocalDateTime.now());
        status.put("publishing", Map.of(
            "totalEventsPublished", publishingStats.getTotalEventsPublished()
        ));
        status.put("handling", Map.of(
            "registrationEvents", handlingStats.getRegistrationEvents(),
            "planEvents", handlingStats.getPlanEvents(),
            "fileEvents", handlingStats.getFileEvents(),
            "emailEvents", handlingStats.getEmailEvents(),
            "systemEvents", handlingStats.getSystemEvents(),
            "totalEvents", handlingStats.getTotalEvents()
        ));
        
        return ResponseEntity.ok(status);
    }

    /**
     * Configuration status
     */
    @GetMapping("/config")
    @Operation(summary = "Configuration status", description = "Returns application configuration validation status")
    @ApiResponse(responseCode = "200", description = "Configuration status")
    public ResponseEntity<Map<String, Object>> configStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("timestamp", LocalDateTime.now());
        status.put("valid", configManager.isConfigurationValid());
        status.put("summary", configManager.getConfigurationMap());
        
        return ResponseEntity.ok(status);
    }

    // Private helper methods for health checks

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("connectionValid", isValid);
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private Map<String, Object> checkEmailHealth() {
        Map<String, Object> emailHealth = new HashMap<>();
        try {
            ThreadSafeEmailProcessor.ProcessingStats stats = emailProcessor.getProcessingStats();
            emailHealth.put("status", "UP");
            emailHealth.put("processing", stats.isProcessing());
            emailHealth.put("pendingEmails", stats.getPendingEmails());
            emailHealth.put("failedEmails", stats.getFailedEmails());
        } catch (Exception e) {
            logger.error("Email health check failed", e);
            emailHealth.put("status", "DOWN");
            emailHealth.put("error", e.getMessage());
        }
        return emailHealth;
    }

    private Map<String, Object> checkConfigurationHealth() {
        Map<String, Object> configHealth = new HashMap<>();
        try {
            boolean isValid = configManager.isConfigurationValid();
            configHealth.put("status", isValid ? "UP" : "DOWN");
            configHealth.put("valid", isValid);
        } catch (Exception e) {
            logger.error("Configuration health check failed", e);
            configHealth.put("status", "DOWN");
            configHealth.put("error", e.getMessage());
        }
        return configHealth;
    }

    private Map<String, Object> checkEventSystemHealth() {
        Map<String, Object> eventHealth = new HashMap<>();
        try {
            TreasureHuntEventPublisher.EventPublishingStats publishingStats = eventPublisher.getStats();
            TreasureHuntEventListener.EventHandlingStats handlingStats = eventListener.getStats();
            
            eventHealth.put("status", "UP");
            eventHealth.put("eventsPublished", publishingStats.getTotalEventsPublished());
            eventHealth.put("eventsHandled", handlingStats.getTotalEvents());
        } catch (Exception e) {
            logger.error("Event system health check failed", e);
            eventHealth.put("status", "DOWN");
            eventHealth.put("error", e.getMessage());
        }
        return eventHealth;
    }

    private Map<String, Object> checkPerformanceHealth() {
        Map<String, Object> perfHealth = new HashMap<>();
        try {
            PerformanceMonitoringService.PerformanceMetrics metrics = 
                performanceMonitoringService.getCurrentMetrics();
            
            boolean memoryHealthy = metrics.heapUtilization < 90.0;
            boolean threadsHealthy = metrics.threadCount < 200;
            boolean dbHealthy = metrics.databaseConnectionHealth;
            
            boolean overallHealthy = memoryHealthy && threadsHealthy && dbHealthy;
            
            perfHealth.put("status", overallHealthy ? "UP" : "DOWN");
            perfHealth.put("memoryHealthy", memoryHealthy);
            perfHealth.put("threadsHealthy", threadsHealthy);
            perfHealth.put("databaseHealthy", dbHealthy);
            perfHealth.put("heapUtilization", metrics.heapUtilization);
            perfHealth.put("threadCount", metrics.threadCount);
        } catch (Exception e) {
            logger.error("Performance health check failed", e);
            perfHealth.put("status", "DOWN");
            perfHealth.put("error", e.getMessage());
        }
        return perfHealth;
    }
}
