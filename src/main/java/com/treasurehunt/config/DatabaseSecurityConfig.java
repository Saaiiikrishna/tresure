package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Security Configuration
 * Validates database security settings and connection parameters
 */
@Configuration
@EnableTransactionManagement
public class DatabaseSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSecurityConfig.class);

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    /**
     * Validate database security configuration on startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseSecurity() {
        logger.info("=== DATABASE SECURITY VALIDATION STARTED ===");
        
        List<String> securityIssues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Validate connection security
            validateConnectionSecurity(securityIssues, warnings);
            
            // Validate database configuration
            validateDatabaseConfiguration(securityIssues, warnings);
            
            // Validate connection pool settings
            validateConnectionPoolSettings(securityIssues, warnings);
            
            // Report findings
            reportDatabaseSecurityFindings(securityIssues, warnings);
            
        } catch (Exception e) {
            logger.error("Error during database security validation", e);
            securityIssues.add("Database security validation failed: " + e.getMessage());
        }
        
        logger.info("=== DATABASE SECURITY VALIDATION COMPLETED ===");
    }

    /**
     * Validate database connection security
     */
    private void validateConnectionSecurity(List<String> securityIssues, List<String> warnings) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            logger.debug("Database: {} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            logger.debug("Driver: {} {}", metaData.getDriverName(), metaData.getDriverVersion());
            
            // Check SSL/TLS usage
            String jdbcUrl = environment.getProperty("spring.datasource.url", "");
            if (!jdbcUrl.toLowerCase().contains("ssl") && !jdbcUrl.toLowerCase().contains("sslmode")) {
                if (jdbcUrl.contains("postgresql") || jdbcUrl.contains("mysql")) {
                    warnings.add("Database connection may not be using SSL/TLS encryption");
                }
            }
            
            // Check for default ports
            if (jdbcUrl.contains(":5432") && jdbcUrl.contains("postgresql")) {
                warnings.add("Using default PostgreSQL port (5432) - consider using non-standard port");
            }
            if (jdbcUrl.contains(":3306") && jdbcUrl.contains("mysql")) {
                warnings.add("Using default MySQL port (3306) - consider using non-standard port");
            }
            
            // Check connection properties
            validateConnectionProperties(connection, securityIssues, warnings);
            
        } catch (SQLException e) {
            logger.error("Error validating database connection security", e);
            securityIssues.add("Could not validate database connection security: " + e.getMessage());
        }
    }

    /**
     * Validate connection properties
     */
    private void validateConnectionProperties(Connection connection, List<String> securityIssues, List<String> warnings) {
        try {
            // Check auto-commit setting
            if (connection.getAutoCommit()) {
                warnings.add("Auto-commit is enabled - consider disabling for better transaction control");
            }
            
            // Check transaction isolation level
            int isolationLevel = connection.getTransactionIsolation();
            if (isolationLevel < Connection.TRANSACTION_READ_COMMITTED) {
                warnings.add("Transaction isolation level is below READ_COMMITTED");
            }
            
            // Check if connection is read-only (should not be for main datasource)
            if (connection.isReadOnly()) {
                securityIssues.add("Main database connection is read-only");
            }
            
        } catch (SQLException e) {
            logger.debug("Could not validate connection properties: {}", e.getMessage());
        }
    }

    /**
     * Validate database configuration
     */
    private void validateDatabaseConfiguration(List<String> securityIssues, List<String> warnings) {
        // Check JPA settings - Log warning but allow startup
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "");
        if ("create-drop".equals(ddlAuto) || "create".equals(ddlAuto)) {
            logger.warn("⚠️  DATABASE SECURITY WARNING: Dangerous DDL auto setting detected: {}", ddlAuto);
            logger.warn("⚠️  This setting will DELETE ALL DATA when the application stops!");
            logger.warn("⚠️  Change to 'update' for production to avoid data loss");
            warnings.add("Dangerous DDL auto setting detected: " + ddlAuto + " - WILL DELETE DATA ON RESTART!");
        }
        
        boolean showSql = environment.getProperty("spring.jpa.show-sql", Boolean.class, false);
        if (showSql) {
            String activeProfile = environment.getProperty("spring.profiles.active", "");
            if ("production".equals(activeProfile)) {
                securityIssues.add("SQL logging is enabled in production");
            } else {
                warnings.add("SQL logging is enabled - disable in production");
            }
        }
        
        // Check database credentials
        validateDatabaseCredentials(securityIssues, warnings);
    }

    /**
     * Validate database credentials
     */
    private void validateDatabaseCredentials(List<String> securityIssues, List<String> warnings) {
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");
        
        // Check for default/weak usernames
        List<String> weakUsernames = List.of("root", "admin", "sa", "postgres", "user", "test");
        if (weakUsernames.contains(username.toLowerCase())) {
            warnings.add("Database username appears to be default/common: " + username);
        }
        
        // Check password strength (basic check)
        if (password.length() < 8) {
            securityIssues.add("Database password is too short (less than 8 characters)");
        }
        
        // Check if credentials are externalized
        String dbPasswordEnv = System.getenv("DB_PASSWORD");
        if (dbPasswordEnv == null || dbPasswordEnv.trim().isEmpty()) {
            warnings.add("Database password may not be externalized to environment variables");
        }
    }

    /**
     * Validate connection pool settings
     */
    private void validateConnectionPoolSettings(List<String> securityIssues, List<String> warnings) {
        // Check HikariCP settings
        Integer maxPoolSize = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        if (maxPoolSize != null && maxPoolSize > 50) {
            warnings.add("Connection pool size is very large: " + maxPoolSize + " - may cause resource exhaustion");
        }
        
        Long connectionTimeout = environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class);
        if (connectionTimeout != null && connectionTimeout > 60000) {
            warnings.add("Connection timeout is very high: " + connectionTimeout + "ms - may cause hanging connections");
        }
        
        Long leakDetectionThreshold = environment.getProperty("spring.datasource.hikari.leak-detection-threshold", Long.class);
        if (leakDetectionThreshold == null || leakDetectionThreshold <= 0) {
            warnings.add("Connection leak detection is disabled - enable for production monitoring");
        }
        
        // Check for connection validation
        String testQuery = environment.getProperty("spring.datasource.hikari.connection-test-query", "");
        if (!testQuery.trim().isEmpty() && !testQuery.toLowerCase().startsWith("select")) {
            warnings.add("Connection test query may not be safe: " + testQuery);
        }
    }

    /**
     * Report database security findings
     */
    private void reportDatabaseSecurityFindings(List<String> securityIssues, List<String> warnings) {
        if (!securityIssues.isEmpty()) {
            logger.error("❌ DATABASE SECURITY ISSUES FOUND:");
            securityIssues.forEach(issue -> logger.error("   - {}", issue));
            
            String activeProfile = environment.getProperty("spring.profiles.active", "");
            if ("production".equals(activeProfile)) {
                throw new IllegalStateException(
                    "Critical database security issues detected in production. " +
                    "Issues: " + String.join(", ", securityIssues)
                );
            }
        }

        if (!warnings.isEmpty()) {
            logger.warn("⚠️ DATABASE SECURITY WARNINGS:");
            warnings.forEach(warning -> logger.warn("   - {}", warning));
        }

        if (securityIssues.isEmpty() && warnings.isEmpty()) {
            logger.info("✅ Database security validation passed - no critical issues found");
        }
    }

    /**
     * Test database connectivity and basic operations
     */
    public boolean testDatabaseConnectivity() {
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            if (!connection.isValid(5)) {
                logger.error("Database connection is not valid");
                return false;
            }
            
            // Test basic query execution
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                
                if (resultSet.next()) {
                    logger.debug("Database connectivity test passed");
                    return true;
                } else {
                    logger.error("Database query test failed");
                    return false;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Database connectivity test failed", e);
            return false;
        }
    }

    /**
     * Get database connection statistics
     */
    public DatabaseStats getDatabaseStats() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            return new DatabaseStats(
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                metaData.getDriverName(),
                metaData.getDriverVersion(),
                connection.getTransactionIsolation(),
                !connection.getAutoCommit()
            );
            
        } catch (SQLException e) {
            logger.error("Error getting database stats", e);
            return new DatabaseStats("Unknown", "Unknown", "Unknown", "Unknown", -1, false);
        }
    }

    /**
     * Database statistics data class
     */
    public static class DatabaseStats {
        public final String databaseProduct;
        public final String databaseVersion;
        public final String driverName;
        public final String driverVersion;
        public final int transactionIsolation;
        public final boolean transactionManagement;

        public DatabaseStats(String databaseProduct, String databaseVersion, String driverName, 
                           String driverVersion, int transactionIsolation, boolean transactionManagement) {
            this.databaseProduct = databaseProduct;
            this.databaseVersion = databaseVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.transactionIsolation = transactionIsolation;
            this.transactionManagement = transactionManagement;
        }
    }
}
