package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Caching configuration for improved application performance
 * Uses in-memory caching for frequently accessed data
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Autowired(required = false)
    private CacheManager cacheManager;

    // Cache names - FIXED: Added missing cache definitions
    public static final String TREASURE_HUNT_PLANS_CACHE = "treasureHuntPlans";
    public static final String FEATURED_PLAN_CACHE = "featuredPlan";
    public static final String APP_SETTINGS_CACHE = "appSettings";
    public static final String PLAN_STATISTICS_CACHE = "planStatistics";
    public static final String USER_REGISTRATION_CACHE = "userRegistrations";
    public static final String EMAIL_TEMPLATES_CACHE = "emailTemplates";
    public static final String REGISTRATION_STATISTICS_CACHE = "registrationStatistics";
    public static final String EMAIL_QUEUE_CACHE = "emailQueue";
    public static final String UPLOADED_IMAGES_CACHE = "uploadedImages";

    /**
     * Production cache manager using Caffeine for high performance
     * @return Configured Caffeine cache manager
     */
    @Bean
    @Primary
    @Profile("production")
    public CacheManager productionCacheManager() {
        logger.info("Configuring production Caffeine cache manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configure Caffeine with production-optimized settings
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(60, java.util.concurrent.TimeUnit.MINUTES)
            .expireAfterAccess(30, java.util.concurrent.TimeUnit.MINUTES)
            .recordStats());

        // FIXED: Pre-configure ALL cache names including missing ones
        cacheManager.setCacheNames(Arrays.asList(
            TREASURE_HUNT_PLANS_CACHE,
            FEATURED_PLAN_CACHE,
            APP_SETTINGS_CACHE,
            PLAN_STATISTICS_CACHE,
            USER_REGISTRATION_CACHE,
            EMAIL_TEMPLATES_CACHE,
            REGISTRATION_STATISTICS_CACHE,
            EMAIL_QUEUE_CACHE,
            UPLOADED_IMAGES_CACHE
        ));

        logger.info("Production Caffeine cache manager configured with {} pre-defined caches",
                   cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * Development cache manager using ConcurrentMapCacheManager
     * @return Configured simple cache manager
     */
    @Bean
    @Profile("!production")
    public CacheManager developmentCacheManager() {
        logger.info("Configuring development cache manager");

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // FIXED: Pre-configure ALL cache names including missing ones
        cacheManager.setCacheNames(Arrays.asList(
            TREASURE_HUNT_PLANS_CACHE,
            FEATURED_PLAN_CACHE,
            APP_SETTINGS_CACHE,
            PLAN_STATISTICS_CACHE,
            USER_REGISTRATION_CACHE,
            EMAIL_TEMPLATES_CACHE,
            REGISTRATION_STATISTICS_CACHE,
            EMAIL_QUEUE_CACHE,
            UPLOADED_IMAGES_CACHE
        ));

        // Allow dynamic cache creation
        cacheManager.setAllowNullValues(false);

        logger.info("Development cache manager configured with {} pre-defined caches",
                   cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * PRODUCTION FIX: Initialize and validate cache configuration on startup
     */
    @PostConstruct
    public void initializeAndValidateCache() {
        logger.info("Initializing cache configuration...");

        if (cacheManager != null) {
            logger.info("Cache manager type: {}", cacheManager.getClass().getSimpleName());

            // Validate all required caches exist
            String[] requiredCaches = {
                TREASURE_HUNT_PLANS_CACHE, FEATURED_PLAN_CACHE, APP_SETTINGS_CACHE,
                PLAN_STATISTICS_CACHE, USER_REGISTRATION_CACHE, EMAIL_TEMPLATES_CACHE,
                REGISTRATION_STATISTICS_CACHE, EMAIL_QUEUE_CACHE, UPLOADED_IMAGES_CACHE
            };

            for (String cacheName : requiredCaches) {
                if (cacheManager.getCache(cacheName) != null) {
                    logger.debug("✅ Cache '{}' is properly configured", cacheName);
                } else {
                    logger.error("❌ Cache '{}' is NOT configured - this will cause errors!", cacheName);
                }
            }

            logger.info("Cache initialization completed successfully");
        } else {
            logger.error("❌ CRITICAL: No cache manager found! Caching will not work.");
        }
    }

    /**
     * Cache configuration properties
     */
    public static class CacheProperties {
        
        // FIXED: Cache TTL settings (in seconds) - Added missing cache TTLs
        public static final long TREASURE_HUNT_PLANS_TTL = 300; // 5 minutes
        public static final long FEATURED_PLAN_TTL = 600; // 10 minutes
        public static final long APP_SETTINGS_TTL = 1800; // 30 minutes
        public static final long PLAN_STATISTICS_TTL = 180; // 3 minutes
        public static final long USER_REGISTRATION_TTL = 60; // 1 minute
        public static final long EMAIL_TEMPLATES_TTL = 3600; // 1 hour
        public static final long REGISTRATION_STATISTICS_TTL = 300; // 5 minutes
        public static final long EMAIL_QUEUE_TTL = 120; // 2 minutes
        public static final long UPLOADED_IMAGES_TTL = 1800; // 30 minutes

        // Cache size limits
        public static final int MAX_TREASURE_HUNT_PLANS = 100;
        public static final int MAX_APP_SETTINGS = 50;
        public static final int MAX_PLAN_STATISTICS = 200;
        public static final int MAX_USER_REGISTRATIONS = 1000;
        public static final int MAX_EMAIL_TEMPLATES = 20;
    }

    /**
     * Cache statistics and monitoring
     */
    public static class CacheStats {
        private final String cacheName;
        private final long hitCount;
        private final long missCount;
        private final long size;
        private final double hitRatio;

        public CacheStats(String cacheName, long hitCount, long missCount, long size) {
            this.cacheName = cacheName;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.size = size;
            this.hitRatio = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
        }

        // Getters
        public String getCacheName() { return cacheName; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getSize() { return size; }
        public double getHitRatio() { return hitRatio; }

        @Override
        public String toString() {
            return String.format("CacheStats{name='%s', hits=%d, misses=%d, size=%d, hitRatio=%.2f%%}",
                               cacheName, hitCount, missCount, size, hitRatio * 100);
        }
    }

    /**
     * Cache warming strategies
     */
    public interface CacheWarmer {
        
        /**
         * Warm up treasure hunt plans cache
         */
        void warmTreasureHuntPlansCache();
        
        /**
         * Warm up featured plan cache
         */
        void warmFeaturedPlanCache();
        
        /**
         * Warm up app settings cache
         */
        void warmAppSettingsCache();
        
        /**
         * Warm up all caches
         */
        default void warmAllCaches() {
            warmTreasureHuntPlansCache();
            warmFeaturedPlanCache();
            warmAppSettingsCache();
        }
    }

    /**
     * Cache eviction strategies
     */
    public interface CacheEvictionStrategy {
        
        /**
         * Evict treasure hunt plans cache
         */
        void evictTreasureHuntPlansCache();
        
        /**
         * Evict featured plan cache
         */
        void evictFeaturedPlanCache();
        
        /**
         * Evict app settings cache
         */
        void evictAppSettingsCache();
        
        /**
         * Evict plan statistics cache
         */
        void evictPlanStatisticsCache();
        
        /**
         * Evict all caches
         */
        void evictAllCaches();
    }

    /**
     * Cache key generation utilities
     */
    public static class CacheKeyGenerator {
        
        /**
         * Generate cache key for treasure hunt plan
         * @param planId Plan ID
         * @return Cache key
         */
        public static String planKey(Long planId) {
            return "plan:" + planId;
        }
        
        /**
         * Generate cache key for plan statistics
         * @param planId Plan ID
         * @return Cache key
         */
        public static String planStatsKey(Long planId) {
            return "stats:" + planId;
        }
        
        /**
         * Generate cache key for user registration
         * @param registrationId Registration ID
         * @return Cache key
         */
        public static String registrationKey(Long registrationId) {
            return "registration:" + registrationId;
        }
        
        /**
         * Generate cache key for app setting
         * @param settingKey Setting key
         * @return Cache key
         */
        public static String appSettingKey(String settingKey) {
            return "setting:" + settingKey;
        }
        
        /**
         * Generate cache key for email template
         * @param templateName Template name
         * @return Cache key
         */
        public static String emailTemplateKey(String templateName) {
            return "template:" + templateName;
        }
    }

    /**
     * Cache configuration validation
     */
    public void validateCacheConfiguration() {
        logger.info("Validating cache configuration...");

        // FIXED: Get cache manager from Spring context instead of calling method directly
        CacheManager manager = null;
        try {
            // This will be injected by Spring
            manager = productionCacheManager();
        } catch (Exception e) {
            logger.warn("Could not get production cache manager, trying development: {}", e.getMessage());
            try {
                manager = developmentCacheManager();
            } catch (Exception ex) {
                logger.error("Could not get any cache manager for validation", ex);
                return;
            }
        }
        
        // FIXED: Verify ALL expected caches are configured including missing ones
        String[] expectedCaches = {
            TREASURE_HUNT_PLANS_CACHE,
            FEATURED_PLAN_CACHE,
            APP_SETTINGS_CACHE,
            PLAN_STATISTICS_CACHE,
            USER_REGISTRATION_CACHE,
            EMAIL_TEMPLATES_CACHE,
            REGISTRATION_STATISTICS_CACHE,
            EMAIL_QUEUE_CACHE,
            UPLOADED_IMAGES_CACHE
        };
        
        for (String cacheName : expectedCaches) {
            if (manager.getCache(cacheName) == null) {
                logger.warn("Cache '{}' is not configured", cacheName);
            } else {
                logger.debug("Cache '{}' is properly configured", cacheName);
            }
        }
        
        logger.info("Cache configuration validation completed");
    }

    /**
     * Log cache configuration
     */
    public void logCacheConfiguration() {
        logger.info("Application Cache Configuration:");
        // FIXED: Don't call cache manager methods directly - this will be handled by Spring
        logger.info("  Cache Manager: Caffeine (Production) / ConcurrentMap (Development)");
        logger.info("  Configured Caches:");

        // Log all configured cache names
        String[] allCaches = {
            TREASURE_HUNT_PLANS_CACHE, FEATURED_PLAN_CACHE, APP_SETTINGS_CACHE,
            PLAN_STATISTICS_CACHE, USER_REGISTRATION_CACHE, EMAIL_TEMPLATES_CACHE,
            REGISTRATION_STATISTICS_CACHE, EMAIL_QUEUE_CACHE, UPLOADED_IMAGES_CACHE
        };

        for (String cacheName : allCaches) {
            logger.info("    - {}", cacheName);
        }
        
        logger.info("  Cache Properties:");
        logger.info("    - Treasure Hunt Plans TTL: {}s", CacheProperties.TREASURE_HUNT_PLANS_TTL);
        logger.info("    - Featured Plan TTL: {}s", CacheProperties.FEATURED_PLAN_TTL);
        logger.info("    - App Settings TTL: {}s", CacheProperties.APP_SETTINGS_TTL);
        logger.info("    - Plan Statistics TTL: {}s", CacheProperties.PLAN_STATISTICS_TTL);
        logger.info("    - User Registration TTL: {}s", CacheProperties.USER_REGISTRATION_TTL);
        logger.info("    - Email Templates TTL: {}s", CacheProperties.EMAIL_TEMPLATES_TTL);
    }

    /**
     * Get cache usage recommendations
     */
    public void logCacheUsageRecommendations() {
        logger.info("Cache Usage Recommendations:");
        logger.info("  1. Use @Cacheable on frequently accessed read operations");
        logger.info("  2. Use @CacheEvict on update/delete operations");
        logger.info("  3. Use @CachePut for cache-through operations");
        logger.info("  4. Monitor cache hit ratios and adjust TTL accordingly");
        logger.info("  5. Consider cache warming for critical data");
        logger.info("  6. Implement cache eviction strategies for data consistency");
    }
}
