package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resource Management Configuration
 * Monitors and manages system resources to prevent leaks and optimize performance
 */
@Configuration
@EnableScheduling
public class ResourceManagementConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManagementConfig.class);

    @Value("${app.resource-management.monitoring-enabled:true}")
    private boolean monitoringEnabled;

    @Value("${app.resource-management.cleanup-interval-minutes:30}")
    private int cleanupIntervalMinutes;

    @Value("${app.resource-management.memory-threshold-percent:85}")
    private int memoryThresholdPercent;

    @Value("${app.resource-management.thread-threshold:200}")
    private int threadThreshold;

    @Bean
    public ResourceMonitor resourceMonitor() {
        return new ResourceMonitor();
    }

    @Bean
    public ResourceCleanupService resourceCleanupService() {
        return new ResourceCleanupService();
    }

    /**
     * Resource monitoring service
     */
    @Component
    public static class ResourceMonitor {
        
        private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);
        
        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        private final AtomicLong memoryWarningCount = new AtomicLong(0);
        private final AtomicLong threadWarningCount = new AtomicLong(0);

        @Value("${app.resource-management.memory-threshold-percent:85}")
        private int memoryThresholdPercent;

        @Value("${app.resource-management.thread-threshold:200}")
        private int threadThreshold;

        /**
         * Monitor system resources every 5 minutes
         */
        @Scheduled(fixedRate = 300000) // 5 minutes
        public void monitorResources() {
            try {
                monitorMemoryUsage();
                monitorThreadUsage();
                monitorFileHandles();
            } catch (Exception e) {
                logger.error("Error during resource monitoring", e);
            }
        }

        /**
         * Monitor memory usage
         */
        private void monitorMemoryUsage() {
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            if (heapMax > 0) {
                double usagePercent = (double) heapUsed / heapMax * 100;
                
                if (usagePercent > memoryThresholdPercent) {
                    long warningCount = memoryWarningCount.incrementAndGet();
                    logger.warn("⚠️ High memory usage detected: {:.2f}% (Warning #{}) - Consider garbage collection", 
                               usagePercent, warningCount);
                    
                    if (warningCount % 3 == 0) { // Every 3rd warning, suggest GC
                        logger.warn("Suggesting garbage collection due to high memory usage");
                        System.gc(); // Suggest GC, but don't force it
                    }
                } else {
                    logger.debug("Memory usage: {:.2f}% ({} MB / {} MB)", 
                                usagePercent, heapUsed / 1024 / 1024, heapMax / 1024 / 1024);
                }
            }
        }

        /**
         * Monitor thread usage
         */
        private void monitorThreadUsage() {
            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            
            if (threadCount > threadThreshold) {
                long warningCount = threadWarningCount.incrementAndGet();
                logger.warn("⚠️ High thread count detected: {} threads ({} daemon) - Warning #{}", 
                           threadCount, daemonThreadCount, warningCount);
                
                // Log thread details for debugging
                if (warningCount % 5 == 0) { // Every 5th warning
                    logThreadDetails();
                }
            } else {
                logger.debug("Thread count: {} ({} daemon)", threadCount, daemonThreadCount);
            }
        }

        /**
         * Monitor file handle usage (approximate)
         */
        private void monitorFileHandles() {
            try {
                // This is a rough estimate - actual implementation would depend on OS
                long openFileDescriptors = threadBean.getThreadCount() * 2; // Rough estimate
                logger.debug("Estimated open file descriptors: {}", openFileDescriptors);
                
                if (openFileDescriptors > 1000) {
                    logger.warn("⚠️ High number of estimated file descriptors: {}", openFileDescriptors);
                }
            } catch (Exception e) {
                logger.debug("Could not monitor file handles: {}", e.getMessage());
            }
        }

        /**
         * Log detailed thread information for debugging
         */
        private void logThreadDetails() {
            try {
                long[] threadIds = threadBean.getAllThreadIds();
                logger.debug("Thread details - Total threads: {}", threadIds.length);
                
                // Log thread states
                int runnableCount = 0, blockedCount = 0, waitingCount = 0;
                
                for (long threadId : threadIds) {
                    Thread.State state = threadBean.getThreadInfo(threadId).getThreadState();
                    switch (state) {
                        case RUNNABLE -> runnableCount++;
                        case BLOCKED -> blockedCount++;
                        case WAITING, TIMED_WAITING -> waitingCount++;
                    }
                }
                
                logger.debug("Thread states - Runnable: {}, Blocked: {}, Waiting: {}", 
                            runnableCount, blockedCount, waitingCount);
                
            } catch (Exception e) {
                logger.debug("Could not log thread details: {}", e.getMessage());
            }
        }

        /**
         * Get current resource metrics
         */
        public ResourceMetrics getCurrentMetrics() {
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUtilization = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
            
            return new ResourceMetrics(
                heapUtilization,
                threadBean.getThreadCount(),
                memoryWarningCount.get(),
                threadWarningCount.get()
            );
        }
    }

    /**
     * Resource cleanup service
     */
    @Component
    public static class ResourceCleanupService {
        
        private static final Logger logger = LoggerFactory.getLogger(ResourceCleanupService.class);
        private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "resource-cleanup");
                t.setDaemon(true);
                return t;
            }
        );

        @Value("${app.file-storage.temp-dir:uploads/temp}")
        private String tempDir;

        @Value("${app.resource-management.temp-file-max-age-hours:24}")
        private int tempFileMaxAgeHours;

        /**
         * Initialize cleanup scheduler
         */
        @EventListener(ApplicationReadyEvent.class)
        public void initializeCleanup() {
            if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
                logger.info("Initializing resource cleanup scheduler");
                
                // Schedule cleanup every hour
                cleanupExecutor.scheduleAtFixedRate(
                    this::performCleanup,
                    1, // Initial delay
                    60, // Period
                    TimeUnit.MINUTES
                );
            }
        }

        /**
         * Perform resource cleanup
         */
        @Scheduled(fixedRate = 3600000) // Every hour
        public void performCleanup() {
            try {
                logger.debug("Starting resource cleanup cycle");
                
                cleanupTempFiles();
                cleanupMemory();
                
                logger.debug("Resource cleanup cycle completed");
            } catch (Exception e) {
                logger.error("Error during resource cleanup", e);
            }
        }

        /**
         * Clean up temporary files
         */
        private void cleanupTempFiles() {
            try {
                Path tempPath = Paths.get(tempDir);
                if (!Files.exists(tempPath)) {
                    return;
                }

                LocalDateTime cutoffTime = LocalDateTime.now().minusHours(tempFileMaxAgeHours);
                
                Files.walk(tempPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            LocalDateTime fileTime = LocalDateTime.parse(
                                Files.getLastModifiedTime(path).toString().substring(0, 19),
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            );
                            return fileTime.isBefore(cutoffTime);
                        } catch (Exception e) {
                            return false; // Keep file if we can't determine age
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.debug("Deleted old temp file: {}", path);
                        } catch (IOException e) {
                            logger.warn("Could not delete temp file {}: {}", path, e.getMessage());
                        }
                    });
                    
            } catch (Exception e) {
                logger.warn("Error cleaning up temp files: {}", e.getMessage());
            }
        }

        /**
         * Suggest memory cleanup
         */
        private void cleanupMemory() {
            // Suggest garbage collection if memory usage is high
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            if (heapMax > 0) {
                double usagePercent = (double) heapUsed / heapMax * 100;
                if (usagePercent > 80) {
                    logger.debug("Suggesting garbage collection due to high memory usage: {:.2f}%", usagePercent);
                    System.gc();
                }
            }
        }

        /**
         * Shutdown cleanup resources
         */
        @PreDestroy
        public void shutdown() {
            logger.info("Shutting down resource cleanup service");
            if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
                cleanupExecutor.shutdown();
                try {
                    if (!cleanupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        cleanupExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Resource metrics data class
     */
    public static class ResourceMetrics {
        public final double heapUtilization;
        public final int threadCount;
        public final long memoryWarnings;
        public final long threadWarnings;

        public ResourceMetrics(double heapUtilization, int threadCount, long memoryWarnings, long threadWarnings) {
            this.heapUtilization = heapUtilization;
            this.threadCount = threadCount;
            this.memoryWarnings = memoryWarnings;
            this.threadWarnings = threadWarnings;
        }
    }
}
