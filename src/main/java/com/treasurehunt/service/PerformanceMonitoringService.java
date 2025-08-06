package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring service that tracks application metrics
 * Provides insights into system performance, memory usage, and database connections
 */
@Service
public class PerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);

    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final ThreadSafeEmailProcessor emailProcessor;

    // Performance counters
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong slowQueryCount = new AtomicLong(0);

    // Performance thresholds
    private static final long MEMORY_WARNING_THRESHOLD = 80; // 80% memory usage
    private static final long MEMORY_CRITICAL_THRESHOLD = 90; // 90% memory usage
    private static final int THREAD_WARNING_THRESHOLD = 100; // 100 threads
    private static final int THREAD_CRITICAL_THRESHOLD = 200; // 200 threads

    @Autowired
    public PerformanceMonitoringService(DataSource dataSource,
                                       CacheManager cacheManager,
                                       @Lazy ThreadSafeEmailProcessor emailProcessor) {
        this.dataSource = dataSource;
        this.cacheManager = cacheManager;
        this.emailProcessor = emailProcessor;
    }

    /**
     * Scheduled performance monitoring - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorPerformance() {
        try {
            PerformanceMetrics metrics = collectPerformanceMetrics();
            logPerformanceMetrics(metrics);
            checkPerformanceThresholds(metrics);
        } catch (Exception e) {
            logger.error("Error during performance monitoring", e);
        }
    }

    /**
     * Collect comprehensive performance metrics
     * @return Performance metrics
     */
    public PerformanceMetrics collectPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        metrics.heapUsed = heapUsage.getUsed();
        metrics.heapMax = heapUsage.getMax();
        metrics.heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        metrics.nonHeapUsed = nonHeapUsage.getUsed();
        metrics.nonHeapMax = nonHeapUsage.getMax();

        // Thread metrics
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.threadCount = threadBean.getThreadCount();
        metrics.peakThreadCount = threadBean.getPeakThreadCount();
        metrics.daemonThreadCount = threadBean.getDaemonThreadCount();

        // Database connection metrics
        metrics.databaseConnectionHealth = checkDatabaseConnection();

        // Cache metrics
        metrics.cacheStats = collectCacheStats();

        // Email processing metrics
        if (emailProcessor != null) {
            metrics.emailProcessingStats = emailProcessor.getProcessingStats();
        }

        // Application counters
        metrics.totalRequests = requestCount.get();
        metrics.totalErrors = errorCount.get();
        metrics.slowQueries = slowQueryCount.get();

        // System metrics
        Runtime runtime = Runtime.getRuntime();
        metrics.availableProcessors = runtime.availableProcessors();
        metrics.freeMemory = runtime.freeMemory();
        metrics.totalMemory = runtime.totalMemory();
        metrics.maxMemory = runtime.maxMemory();

        return metrics;
    }

    /**
     * Check database connection health
     * @return true if database is healthy
     */
    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Database connection health check failed", e);
            return false;
        }
    }

    /**
     * Collect cache statistics
     * @return Map of cache statistics
     */
    private Map<String, CacheStats> collectCacheStats() {
        Map<String, CacheStats> cacheStats = new HashMap<>();
        
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    // Note: This is a simplified implementation
                    // In a real application, you'd use cache-specific metrics
                    cacheStats.put(cacheName, new CacheStats(cacheName, 0, 0, 0));
                } catch (Exception e) {
                    logger.warn("Error collecting stats for cache: {}", cacheName, e);
                }
            });
        }
        
        return cacheStats;
    }

    /**
     * Log performance metrics
     * @param metrics Performance metrics to log
     */
    private void logPerformanceMetrics(PerformanceMetrics metrics) {
        logger.info("=== PERFORMANCE METRICS ===");
        logger.info("Memory - Heap: {:.1f}% ({} MB / {} MB)", 
                   metrics.heapUtilization, 
                   metrics.heapUsed / 1024 / 1024, 
                   metrics.heapMax / 1024 / 1024);
        logger.info("Memory - Non-Heap: {} MB / {} MB", 
                   metrics.nonHeapUsed / 1024 / 1024, 
                   metrics.nonHeapMax / 1024 / 1024);
        logger.info("Threads - Active: {} (Peak: {}, Daemon: {})", 
                   metrics.threadCount, metrics.peakThreadCount, metrics.daemonThreadCount);
        logger.info("Database - Connection Health: {}", 
                   metrics.databaseConnectionHealth ? "HEALTHY" : "UNHEALTHY");
        logger.info("Application - Requests: {}, Errors: {}, Slow Queries: {}", 
                   metrics.totalRequests, metrics.totalErrors, metrics.slowQueries);
        
        if (metrics.emailProcessingStats != null) {
            logger.info("Email Processing - {}", metrics.emailProcessingStats);
        }
        
        logger.info("System - CPUs: {}, Free Memory: {} MB", 
                   metrics.availableProcessors, metrics.freeMemory / 1024 / 1024);
        logger.info("=== END PERFORMANCE METRICS ===");
    }

    /**
     * Check performance thresholds and log warnings
     * @param metrics Performance metrics to check
     */
    private void checkPerformanceThresholds(PerformanceMetrics metrics) {
        // Memory threshold checks
        if (metrics.heapUtilization >= MEMORY_CRITICAL_THRESHOLD) {
            logger.error("CRITICAL: Heap memory usage is at {:.1f}% - immediate attention required!", 
                        metrics.heapUtilization);
        } else if (metrics.heapUtilization >= MEMORY_WARNING_THRESHOLD) {
            logger.warn("WARNING: Heap memory usage is at {:.1f}% - consider optimization", 
                       metrics.heapUtilization);
        }

        // Thread threshold checks
        if (metrics.threadCount >= THREAD_CRITICAL_THRESHOLD) {
            logger.error("CRITICAL: Thread count is {} - possible thread leak!", metrics.threadCount);
        } else if (metrics.threadCount >= THREAD_WARNING_THRESHOLD) {
            logger.warn("WARNING: Thread count is {} - monitor for thread leaks", metrics.threadCount);
        }

        // Database health check
        if (!metrics.databaseConnectionHealth) {
            logger.error("CRITICAL: Database connection is unhealthy!");
        }

        // Error rate check
        if (metrics.totalRequests > 0) {
            double errorRate = (double) metrics.totalErrors / metrics.totalRequests * 100;
            if (errorRate > 5.0) {
                logger.warn("WARNING: Error rate is {:.2f}% - investigate errors", errorRate);
            }
        }
    }

    /**
     * Increment request counter
     */
    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    /**
     * Increment error counter
     */
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }

    /**
     * Increment slow query counter
     */
    public void incrementSlowQueryCount() {
        slowQueryCount.incrementAndGet();
    }

    /**
     * Reset performance counters
     */
    public void resetCounters() {
        requestCount.set(0);
        errorCount.set(0);
        slowQueryCount.set(0);
        logger.info("Performance counters reset");
    }

    /**
     * Get current performance metrics
     * @return Current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        return collectPerformanceMetrics();
    }

    /**
     * Performance metrics data class
     */
    public static class PerformanceMetrics {
        // Memory metrics
        public long heapUsed;
        public long heapMax;
        public double heapUtilization;
        public long nonHeapUsed;
        public long nonHeapMax;

        // Thread metrics
        public int threadCount;
        public int peakThreadCount;
        public int daemonThreadCount;

        // Database metrics
        public boolean databaseConnectionHealth;

        // Cache metrics
        public Map<String, CacheStats> cacheStats;

        // Email processing metrics
        public ThreadSafeEmailProcessor.ProcessingStats emailProcessingStats;

        // Application metrics
        public long totalRequests;
        public long totalErrors;
        public long slowQueries;

        // System metrics
        public int availableProcessors;
        public long freeMemory;
        public long totalMemory;
        public long maxMemory;

        @Override
        public String toString() {
            return String.format("PerformanceMetrics{heap=%.1f%%, threads=%d, dbHealth=%s, requests=%d, errors=%d}",
                               heapUtilization, threadCount, databaseConnectionHealth, totalRequests, totalErrors);
        }
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStats {
        private final String name;
        private final long hitCount;
        private final long missCount;
        private final long size;

        public CacheStats(String name, long hitCount, long missCount, long size) {
            this.name = name;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.size = size;
        }

        // Getters
        public String getName() { return name; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getSize() { return size; }

        public double getHitRatio() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
    }
}
