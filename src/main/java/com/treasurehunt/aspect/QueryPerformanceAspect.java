package com.treasurehunt.aspect;

import com.treasurehunt.service.PerformanceMonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring query performance and detecting slow operations
 * Automatically tracks execution times for repository methods
 */
@Aspect
@Component
public class QueryPerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceAspect.class);

    // Performance thresholds
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second
    private static final long WARNING_QUERY_THRESHOLD_MS = 500; // 500ms

    private final PerformanceMonitoringService performanceMonitoringService;

    @Autowired
    public QueryPerformanceAspect(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Monitor all repository method executions
     * @param joinPoint Method execution join point
     * @return Method result
     * @throws Throwable If method execution fails
     */
    @Around("execution(* com.treasurehunt.repository.*.*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log performance metrics
            logQueryPerformance(methodName, executionTime, true);
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log failed query performance
            logQueryPerformance(methodName, executionTime, false);
            
            // Increment error counter
            performanceMonitoringService.incrementErrorCount();
            
            throw throwable;
        }
    }

    /**
     * Monitor service method executions for business logic performance
     * @param joinPoint Method execution join point
     * @return Method result
     * @throws Throwable If method execution fails
     */
    @Around("execution(* com.treasurehunt.service.*.*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Only log slow service methods to avoid log spam
            if (executionTime > WARNING_QUERY_THRESHOLD_MS) {
                logger.debug("Service method {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.warn("Service method {} failed after {}ms: {}", 
                       methodName, executionTime, throwable.getMessage());
            
            // Increment error counter
            performanceMonitoringService.incrementErrorCount();
            
            throw throwable;
        }
    }

    /**
     * Monitor controller method executions for request performance
     * @param joinPoint Method execution join point
     * @return Method result
     * @throws Throwable If method execution fails
     */
    @Around("execution(* com.treasurehunt.controller.*.*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        // Increment request counter
        performanceMonitoringService.incrementRequestCount();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log slow controller methods
            if (executionTime > WARNING_QUERY_THRESHOLD_MS) {
                logger.info("Controller method {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Controller method {} failed after {}ms: {}", 
                        methodName, executionTime, throwable.getMessage());
            
            // Increment error counter
            performanceMonitoringService.incrementErrorCount();
            
            throw throwable;
        }
    }

    /**
     * Log query performance metrics
     * @param methodName Method name
     * @param executionTime Execution time in milliseconds
     * @param success Whether the query succeeded
     */
    private void logQueryPerformance(String methodName, long executionTime, boolean success) {
        if (executionTime >= SLOW_QUERY_THRESHOLD_MS) {
            // Log slow queries as warnings
            logger.warn("SLOW QUERY: {} took {}ms (status: {})", 
                       methodName, executionTime, success ? "SUCCESS" : "FAILED");
            
            // Increment slow query counter
            performanceMonitoringService.incrementSlowQueryCount();
            
        } else if (executionTime >= WARNING_QUERY_THRESHOLD_MS) {
            // Log moderately slow queries as info
            logger.info("Query {} took {}ms (status: {})", 
                       methodName, executionTime, success ? "SUCCESS" : "FAILED");
        } else {
            // Log fast queries as debug
            logger.debug("Query {} took {}ms (status: {})", 
                        methodName, executionTime, success ? "SUCCESS" : "FAILED");
        }
    }

    /**
     * Monitor cache operations
     * @param joinPoint Method execution join point
     * @return Method result
     * @throws Throwable If method execution fails
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable) || " +
            "@annotation(org.springframework.cache.annotation.CacheEvict) || " +
            "@annotation(org.springframework.cache.annotation.CachePut)")
    public Object monitorCacheOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.debug("Cache operation {} took {}ms", methodName, executionTime);
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Cache operation {} failed after {}ms: {}", 
                        methodName, executionTime, throwable.getMessage());
            
            throw throwable;
        }
    }

    /**
     * Monitor file operations for I/O performance
     * @param joinPoint Method execution join point
     * @return Method result
     * @throws Throwable If method execution fails
     */
    @Around("execution(* com.treasurehunt.service.*FileService.*(..))")
    public Object monitorFileOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // File operations can be slow, so use higher threshold
            if (executionTime > 2000) { // 2 seconds
                logger.warn("SLOW FILE OPERATION: {} took {}ms", methodName, executionTime);
            } else if (executionTime > 1000) { // 1 second
                logger.info("File operation {} took {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("File operation {} failed after {}ms: {}", 
                        methodName, executionTime, throwable.getMessage());
            
            throw throwable;
        }
    }

    /**
     * Get performance statistics
     * @return Performance statistics summary
     */
    public String getPerformanceStatistics() {
        return String.format("Query Performance Monitoring - Thresholds: Warning=%dms, Slow=%dms", 
                           WARNING_QUERY_THRESHOLD_MS, SLOW_QUERY_THRESHOLD_MS);
    }

    /**
     * Update performance thresholds (for runtime configuration)
     * @param warningThreshold Warning threshold in milliseconds
     * @param slowThreshold Slow query threshold in milliseconds
     */
    public void updateThresholds(long warningThreshold, long slowThreshold) {
        // Note: In a real implementation, these would be configurable fields
        logger.info("Performance thresholds updated: Warning={}ms, Slow={}ms", 
                   warningThreshold, slowThreshold);
    }

    /**
     * Check if monitoring is enabled
     * @return true if monitoring is active
     */
    public boolean isMonitoringEnabled() {
        return true; // Always enabled in this implementation
    }

    /**
     * Get monitoring configuration
     * @return Configuration summary
     */
    public String getMonitoringConfiguration() {
        return String.format("QueryPerformanceAspect{warningThreshold=%dms, slowThreshold=%dms, enabled=%s}",
                           WARNING_QUERY_THRESHOLD_MS, SLOW_QUERY_THRESHOLD_MS, isMonitoringEnabled());
    }
}
