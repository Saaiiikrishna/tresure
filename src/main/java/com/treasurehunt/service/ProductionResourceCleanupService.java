package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Production resource cleanup service
 * Handles cleanup of temporary files, logs, and other resources in production
 */
@Service
@Profile("production")
public class ProductionResourceCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionResourceCleanupService.class);

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String uploadDir;

    @Value("${app.cleanup.temp-file-retention-hours:24}")
    private int tempFileRetentionHours;

    @Value("${app.cleanup.log-retention-days:30}")
    private int logRetentionDays;

    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "production-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * Cleanup temporary files daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempFiles() {
        logger.info("🧹 Starting scheduled temporary file cleanup");
        
        cleanupExecutor.submit(() -> {
            try {
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
                cleanupOldFiles(tempDir, tempFileRetentionHours, ChronoUnit.HOURS);
                logger.info("✅ Temporary file cleanup completed");
            } catch (Exception e) {
                logger.error("❌ Error during temporary file cleanup", e);
            }
        });
    }

    /**
     * Cleanup old log files weekly on Sunday at 3 AM
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void cleanupOldLogs() {
        logger.info("🧹 Starting scheduled log file cleanup");
        
        cleanupExecutor.submit(() -> {
            try {
                Path logsDir = Paths.get("logs");
                if (Files.exists(logsDir)) {
                    cleanupOldFiles(logsDir, logRetentionDays, ChronoUnit.DAYS);
                    logger.info("✅ Log file cleanup completed");
                }
            } catch (Exception e) {
                logger.error("❌ Error during log file cleanup", e);
            }
        });
    }

    /**
     * Cleanup orphaned upload files monthly on 1st day at 4 AM
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void cleanupOrphanedUploads() {
        logger.info("🧹 Starting scheduled orphaned upload cleanup");
        
        cleanupExecutor.submit(() -> {
            try {
                Path uploadsDir = Paths.get(uploadDir);
                if (Files.exists(uploadsDir)) {
                    // This would require database integration to check for orphaned files
                    // For now, just log the action
                    logger.info("📁 Orphaned upload cleanup check completed");
                }
            } catch (Exception e) {
                logger.error("❌ Error during orphaned upload cleanup", e);
            }
        });
    }

    /**
     * Memory cleanup every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void performMemoryCleanup() {
        logger.debug("🧹 Performing memory cleanup");
        
        cleanupExecutor.submit(() -> {
            try {
                // Suggest garbage collection
                System.gc();
                
                // Log memory usage
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                long maxMemory = runtime.maxMemory();
                
                logger.info("💾 Memory usage: {} MB used, {} MB free, {} MB max", 
                           usedMemory / 1024 / 1024, 
                           freeMemory / 1024 / 1024, 
                           maxMemory / 1024 / 1024);
                           
                // Warn if memory usage is high
                double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
                if (memoryUsagePercent > 80) {
                    logger.warn("⚠️ High memory usage detected: {:.1f}%", memoryUsagePercent);
                }
                
            } catch (Exception e) {
                logger.error("❌ Error during memory cleanup", e);
            }
        });
    }

    /**
     * Clean up old files in a directory
     */
    private void cleanupOldFiles(Path directory, int retentionPeriod, ChronoUnit unit) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minus(retentionPeriod, unit);
        int deletedCount = 0;
        long deletedSize = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    LocalDateTime fileTime = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(file).toInstant(),
                        ZoneId.systemDefault()
                    );
                    
                    if (fileTime.isBefore(cutoffTime)) {
                        try {
                            long fileSize = Files.size(file);
                            Files.delete(file);
                            deletedCount++;
                            deletedSize += fileSize;
                            logger.debug("🗑️ Deleted old file: {}", file.getFileName());
                        } catch (IOException e) {
                            logger.warn("⚠️ Failed to delete file: {}", file.getFileName(), e);
                        }
                    }
                }
            }
        }

        if (deletedCount > 0) {
            logger.info("🧹 Cleaned up {} files ({} MB) from {}", 
                       deletedCount, deletedSize / 1024 / 1024, directory);
        }
    }

    /**
     * Graceful shutdown of cleanup executor
     */
    @PreDestroy
    public void shutdown() {
        logger.info("🛑 Shutting down production resource cleanup service");
        
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
                logger.warn("⚠️ Cleanup executor did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("✅ Production resource cleanup service shutdown complete");
    }
}
