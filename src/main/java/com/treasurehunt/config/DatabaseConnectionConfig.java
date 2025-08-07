package com.treasurehunt.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database connection pool configuration for optimized performance
 * Provides production-ready connection pooling with HikariCP
 */
@Configuration
@Profile("production")
public class DatabaseConnectionConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionConfig.class);

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    // Connection pool configuration
    @Value("${app.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${app.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${app.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${app.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${app.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${app.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * Primary DataSource with optimized HikariCP configuration
     * @return Configured HikariDataSource
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.datasource.hikari.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource hikariDataSource() {
        logger.info("Configuring HikariCP connection pool for production");

        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Pool sizing
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);

        // Connection timeouts
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // Performance optimizations
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Pool name for monitoring
        config.setPoolName("TreasureHuntCP");

        // Transaction management - CRITICAL: Disable autoCommit for Spring transaction management
        config.setAutoCommit(false);

        // Additional performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        HikariDataSource dataSource = new HikariDataSource(config);

        logger.info("HikariCP connection pool configured successfully:");
        logger.info("  - Maximum Pool Size: {}", maximumPoolSize);
        logger.info("  - Minimum Idle: {}", minimumIdle);
        logger.info("  - Connection Timeout: {}ms", connectionTimeout);
        logger.info("  - Idle Timeout: {}ms", idleTimeout);
        logger.info("  - Max Lifetime: {}ms", maxLifetime);
        logger.info("  - Leak Detection Threshold: {}ms", leakDetectionThreshold);

        return dataSource;
    }

    /**
     * Connection pool health check
     * @return Connection pool statistics
     */
    public ConnectionPoolStats getConnectionPoolStats() {
        if (hikariDataSource() instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) hikariDataSource();
            return new ConnectionPoolStats(
                hikariDS.getHikariPoolMXBean().getActiveConnections(),
                hikariDS.getHikariPoolMXBean().getIdleConnections(),
                hikariDS.getHikariPoolMXBean().getTotalConnections(),
                hikariDS.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return new ConnectionPoolStats(0, 0, 0, 0);
    }

    /**
     * Connection pool statistics data class
     */
    public static class ConnectionPoolStats {
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int threadsAwaitingConnection;

        public ConnectionPoolStats(int activeConnections, int idleConnections, 
                                 int totalConnections, int threadsAwaitingConnection) {
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
        }

        // Getters
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }

        @Override
        public String toString() {
            return String.format("ConnectionPoolStats{active=%d, idle=%d, total=%d, awaiting=%d}",
                               activeConnections, idleConnections, totalConnections, threadsAwaitingConnection);
        }

        /**
         * Check if connection pool is healthy
         * @return true if pool is healthy
         */
        public boolean isHealthy() {
            // Pool is considered unhealthy if:
            // 1. Too many threads are waiting for connections
            // 2. All connections are active (no idle connections available)
            return threadsAwaitingConnection < 5 && (idleConnections > 0 || activeConnections < totalConnections);
        }

        /**
         * Get pool utilization percentage
         * @return Utilization percentage (0-100)
         */
        public double getUtilizationPercentage() {
            if (totalConnections == 0) return 0.0;
            return (double) activeConnections / totalConnections * 100.0;
        }
    }

    /**
     * Configuration validation
     */
    public void validateConfiguration() {
        logger.info("Validating database connection pool configuration...");

        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("Maximum pool size must be positive");
        }

        if (minimumIdle < 0) {
            throw new IllegalArgumentException("Minimum idle connections cannot be negative");
        }

        if (minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("Minimum idle cannot be greater than maximum pool size");
        }

        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }

        if (idleTimeout <= 0) {
            throw new IllegalArgumentException("Idle timeout must be positive");
        }

        if (maxLifetime <= 0) {
            throw new IllegalArgumentException("Max lifetime must be positive");
        }

        // Warn about potentially problematic configurations
        if (maximumPoolSize > 50) {
            logger.warn("Maximum pool size ({}) is quite large. Consider if this is necessary.", maximumPoolSize);
        }

        if (connectionTimeout > 60000) {
            logger.warn("Connection timeout ({}) is quite long. This may cause slow response times.", connectionTimeout);
        }

        if (leakDetectionThreshold > 0 && leakDetectionThreshold < 10000) {
            logger.warn("Leak detection threshold ({}) is very low. This may cause false positives.", leakDetectionThreshold);
        }

        logger.info("Database connection pool configuration validation completed successfully");
    }

    /**
     * Get recommended pool size based on system resources
     * @return Recommended pool size
     */
    public static int getRecommendedPoolSize() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // Rule of thumb: pool size = number of CPU cores * 2
        int recommendedSize = availableProcessors * 2;
        
        // Ensure minimum of 5 and maximum of 50
        return Math.max(5, Math.min(50, recommendedSize));
    }

    /**
     * Log current configuration
     */
    public void logCurrentConfiguration() {
        logger.info("Current Database Connection Pool Configuration:");
        logger.info("  JDBC URL: {}", maskSensitiveUrl(jdbcUrl));
        logger.info("  Username: {}", username);
        logger.info("  Driver: {}", driverClassName);
        logger.info("  Maximum Pool Size: {}", maximumPoolSize);
        logger.info("  Minimum Idle: {}", minimumIdle);
        logger.info("  Connection Timeout: {}ms", connectionTimeout);
        logger.info("  Idle Timeout: {}ms", idleTimeout);
        logger.info("  Max Lifetime: {}ms", maxLifetime);
        logger.info("  Leak Detection Threshold: {}ms", leakDetectionThreshold);
        logger.info("  Recommended Pool Size: {}", getRecommendedPoolSize());
    }

    /**
     * Mask sensitive information in JDBC URL
     */
    private String maskSensitiveUrl(String url) {
        if (url == null) return "null";
        // Remove password from URL if present
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
