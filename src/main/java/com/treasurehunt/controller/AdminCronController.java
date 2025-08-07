package com.treasurehunt.controller;

import com.treasurehunt.service.FileCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for admin cron job management functionality
 * Handles scheduled task monitoring and manual execution
 */
@Controller
@RequestMapping("/admin/cron")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCronController {

    private static final Logger logger = LoggerFactory.getLogger(AdminCronController.class);

    private final FileCleanupService fileCleanupService;

    @Autowired
    public AdminCronController(FileCleanupService fileCleanupService) {
        this.fileCleanupService = fileCleanupService;
    }

    /**
     * Display cron jobs management page
     */
    @GetMapping
    public String cronJobsManagement(Model model) {
        logger.info("Displaying cron jobs management page");

        try {
            // Get cleanup logs
            List<FileCleanupService.CleanupLog> cleanupLogs = fileCleanupService.getCleanupLogs();

            // Get storage statistics
            FileCleanupService.StorageStats storageStats = fileCleanupService.getStorageStats();

            // Calculate statistics
            long totalCleanups = cleanupLogs.size();
            long successfulCleanups = cleanupLogs.stream()
                .mapToLong(log -> log.isSuccess() ? 1 : 0)
                .sum();
            
            double totalSpaceFreed = cleanupLogs.stream()
                .mapToDouble(FileCleanupService.CleanupLog::getTotalSpaceFreedMB)
                .sum();

            int totalFilesDeleted = cleanupLogs.stream()
                .mapToInt(FileCleanupService.CleanupLog::getTotalFilesDeleted)
                .sum();

            model.addAttribute("cleanupLogs", cleanupLogs);
            model.addAttribute("storageStats", storageStats);
            model.addAttribute("totalCleanups", totalCleanups);
            model.addAttribute("successfulCleanups", successfulCleanups);
            model.addAttribute("totalSpaceFreed", totalSpaceFreed);
            model.addAttribute("totalFilesDeleted", totalFilesDeleted);

        } catch (Exception e) {
            logger.error("Error loading cron jobs management page", e);
            model.addAttribute("error", "Error loading cron job data: " + e.getMessage());
        }

        return "admin/cron-jobs";
    }

    /**
     * Trigger manual file cleanup
     */
    @PostMapping("/cleanup/manual")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerManualCleanup() {
        logger.info("Triggering manual file cleanup");

        Map<String, Object> response = new HashMap<>();

        try {
            // Run cleanup operations
            FileCleanupService.CleanupResult documentResult = fileCleanupService.cleanupOldRegistrationDocuments();
            FileCleanupService.CleanupResult orphanedResult = fileCleanupService.cleanupOrphanedFiles();
            FileCleanupService.CleanupResult imageResult = fileCleanupService.cleanupInactiveImages();

            int totalFilesDeleted = documentResult.getFilesDeleted() + 
                                  orphanedResult.getFilesDeleted() + 
                                  imageResult.getFilesDeleted();
            
            long totalSpaceFreed = documentResult.getSpaceFreed() + 
                                 orphanedResult.getSpaceFreed() + 
                                 imageResult.getSpaceFreed();

            response.put("success", true);
            response.put("message", "Manual cleanup completed successfully");
            response.put("filesDeleted", totalFilesDeleted);
            response.put("spaceFreedMB", totalSpaceFreed / (1024.0 * 1024.0));
            response.put("documentFilesDeleted", documentResult.getFilesDeleted());
            response.put("orphanedFilesDeleted", orphanedResult.getFilesDeleted());
            response.put("imageFilesDeleted", imageResult.getFilesDeleted());

            logger.info("Manual cleanup completed. Files deleted: {}, Space freed: {} MB", 
                       totalFilesDeleted, totalSpaceFreed / (1024.0 * 1024.0));

        } catch (Exception e) {
            logger.error("Error during manual cleanup", e);
            response.put("success", false);
            response.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get cleanup logs
     */
    @GetMapping("/cleanup/logs")
    @ResponseBody
    public ResponseEntity<List<FileCleanupService.CleanupLog>> getCleanupLogs() {
        logger.info("Getting cleanup logs");

        try {
            List<FileCleanupService.CleanupLog> logs = fileCleanupService.getCleanupLogs();
            return ResponseEntity.ok(logs);

        } catch (Exception e) {
            logger.error("Error getting cleanup logs", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get storage statistics
     */
    @GetMapping("/storage/stats")
    @ResponseBody
    public ResponseEntity<FileCleanupService.StorageStats> getStorageStats() {
        logger.info("Getting storage statistics");

        try {
            FileCleanupService.StorageStats stats = fileCleanupService.getStorageStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting storage statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get cron job status information
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCronJobStatus() {
        logger.info("Getting cron job status");

        Map<String, Object> status = new HashMap<>();

        try {
            // Get cleanup logs to determine last run
            List<FileCleanupService.CleanupLog> logs = fileCleanupService.getCleanupLogs();
            
            status.put("fileCleanupEnabled", true);
            status.put("fileCleanupSchedule", "Weekly (Sundays at 2:00 AM)");
            status.put("fileCleanupRetentionDays", 15);
            
            if (!logs.isEmpty()) {
                FileCleanupService.CleanupLog lastLog = logs.get(0);
                status.put("lastCleanupRun", lastLog.getStartTime());
                status.put("lastCleanupSuccess", lastLog.isSuccess());
                status.put("lastCleanupMessage", lastLog.getMessage());
            } else {
                status.put("lastCleanupRun", null);
                status.put("lastCleanupSuccess", null);
                status.put("lastCleanupMessage", "No cleanup runs recorded yet");
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting cron job status", e);
            status.put("error", "Error retrieving cron job status: " + e.getMessage());
            return ResponseEntity.badRequest().body(status);
        }
    }
}
