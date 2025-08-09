package com.treasurehunt.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Logging configuration for standardized application logging
 * Provides consistent logging patterns and levels across the application
 */
@Configuration
public class LoggingConfig {

    @Value("${logging.level.com.treasurehunt:INFO}")
    private String applicationLogLevel;

    @Value("${logging.level.org.springframework:WARN}")
    private String springLogLevel;

    @Value("${logging.level.org.hibernate:WARN}")
    private String hibernateLogLevel;

    @Value("${logging.file.path:logs}")
    private String logFilePath;

    @Value("${logging.file.max-history:30}")
    private int maxHistory;

    @Autowired
    private Environment environment;

    /**
     * Configure logging after bean initialization
     */
    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Configure application logging levels
        configureLoggerLevel(context, "com.treasurehunt", applicationLogLevel);
        configureLoggerLevel(context, "org.springframework", springLogLevel);
        configureLoggerLevel(context, "org.hibernate", hibernateLogLevel);
        
        // Configure specific loggers for better control
        configureSpecificLoggers(context);
        
        org.slf4j.Logger logger = LoggerFactory.getLogger(LoggingConfig.class);
        logger.info("Logging configuration applied successfully");
        logger.info("Application log level: {}", applicationLogLevel);
        logger.info("Spring log level: {}", springLogLevel);
        logger.info("Hibernate log level: {}", hibernateLogLevel);
    }

    /**
     * Configure production logging with file appenders
     */
    @PostConstruct
    @Profile("production")
    public void configureProductionLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Create file appender for application logs
        RollingFileAppender<ILoggingEvent> fileAppender = createFileAppender(context, 
            "APPLICATION", logFilePath + "/application.log");
        
        // Create file appender for error logs
        RollingFileAppender<ILoggingEvent> errorAppender = createErrorFileAppender(context,
            "ERROR", logFilePath + "/error.log");
        
        // Add appenders to root logger
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);
        rootLogger.addAppender(errorAppender);
        
        org.slf4j.Logger logger = LoggerFactory.getLogger(LoggingConfig.class);
        logger.info("Production logging configuration applied");
    }

    /**
     * Configure specific loggers for better control
     */
    private void configureSpecificLoggers(LoggerContext context) {
        boolean isProduction = false;
        try {
            for (String profile : environment.getActiveProfiles()) {
                if ("production".equalsIgnoreCase(profile)) {
                    isProduction = true;
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Security logging
        configureLoggerLevel(context, "org.springframework.security", isProduction ? "INFO" : "DEBUG");

        // Database logging
        configureLoggerLevel(context, "org.hibernate.SQL", isProduction ? "WARN" : "DEBUG");
        configureLoggerLevel(context, "org.hibernate.type.descriptor.sql.BasicBinder", isProduction ? "WARN" : "TRACE");

        // Connection pool logging
        configureLoggerLevel(context, "com.zaxxer.hikari", "INFO");

        // Cache logging
        configureLoggerLevel(context, "org.springframework.cache", isProduction ? "INFO" : "DEBUG");

        // Email logging
        configureLoggerLevel(context, "org.springframework.mail", isProduction ? "INFO" : "DEBUG");

        // File upload logging
        configureLoggerLevel(context, "org.springframework.web.multipart", isProduction ? "INFO" : "DEBUG");

        // Performance monitoring
        configureLoggerLevel(context, "com.treasurehunt.aspect", "INFO");
        configureLoggerLevel(context, "com.treasurehunt.service.PerformanceMonitoringService", "INFO");

        // Cleanup services
        configureLoggerLevel(context, "com.treasurehunt.service.cleanup", "INFO");
    }

    /**
     * Configure logger level
     */
    private void configureLoggerLevel(LoggerContext context, String loggerName, String level) {
        Logger logger = context.getLogger(loggerName);
        logger.setLevel(Level.toLevel(level));
    }

    /**
     * Create rolling file appender
     */
    private RollingFileAppender<ILoggingEvent> createFileAppender(LoggerContext context, 
                                                                 String name, String fileName) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName(name);
        appender.setFile(fileName);

        // Configure rolling policy
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(fileName + ".%d{yyyy-MM-dd}.gz");
        rollingPolicy.setMaxHistory(maxHistory);
        rollingPolicy.start();

        // Configure encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        appender.setRollingPolicy(rollingPolicy);
        appender.setEncoder(encoder);
        appender.start();

        return appender;
    }

    /**
     * Create error-only file appender
     */
    private RollingFileAppender<ILoggingEvent> createErrorFileAppender(LoggerContext context,
                                                                      String name, String fileName) {
        RollingFileAppender<ILoggingEvent> appender = createFileAppender(context, name, fileName);
        
        // Add filter for ERROR level only
        ch.qos.logback.classic.filter.LevelFilter levelFilter = new ch.qos.logback.classic.filter.LevelFilter();
        levelFilter.setLevel(Level.ERROR);
        levelFilter.setOnMatch(ch.qos.logback.core.spi.FilterReply.ACCEPT);
        levelFilter.setOnMismatch(ch.qos.logback.core.spi.FilterReply.DENY);
        levelFilter.start();
        
        appender.addFilter(levelFilter);
        
        return appender;
    }

    /**
     * Logging standards and patterns
     */
    public static class LoggingStandards {
        
        // Standard log patterns
        public static final String CONSOLE_PATTERN = 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{36}) - %msg%n";
        
        public static final String FILE_PATTERN = 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
        
        public static final String JSON_PATTERN = 
            "{\"timestamp\":\"%d{yyyy-MM-dd HH:mm:ss.SSS}\",\"thread\":\"%thread\",\"level\":\"%level\",\"logger\":\"%logger\",\"message\":\"%msg\"}%n";
        
        // Standard log levels by component
        public static final String APPLICATION_LEVEL = "INFO";
        public static final String SECURITY_LEVEL = "INFO";
        public static final String DATABASE_LEVEL = "WARN";
        public static final String PERFORMANCE_LEVEL = "INFO";
        public static final String CLEANUP_LEVEL = "INFO";
        public static final String EMAIL_LEVEL = "DEBUG";
        
        // Log message templates
        public static String formatPerformanceLog(String operation, long duration) {
            return String.format("PERFORMANCE: %s completed in %dms", operation, duration);
        }
        
        public static String formatSecurityLog(String event, String user, String resource) {
            return String.format("SECURITY: %s - User: %s, Resource: %s", event, user, resource);
        }
        
        public static String formatBusinessLog(String operation, String entity, String result) {
            return String.format("BUSINESS: %s %s - %s", operation, entity, result);
        }
        
        public static String formatErrorLog(String operation, String error) {
            return String.format("ERROR: %s failed - %s", operation, error);
        }
        
        public static String formatAuditLog(String action, String user, String details) {
            return String.format("AUDIT: %s by %s - %s", action, user, details);
        }
    }

    /**
     * Logging utilities
     */
    public static class LoggingUtils {
        
        /**
         * Sanitize log message to prevent log injection
         */
        public static String sanitizeLogMessage(String message) {
            if (message == null) return "null";
            
            return message
                .replaceAll("[\r\n\t]", "_")  // Replace line breaks and tabs
                .replaceAll("[\\p{Cntrl}]", "") // Remove control characters
                .trim();
        }
        
        /**
         * Mask sensitive information in logs
         */
        public static String maskSensitiveData(String data) {
            if (data == null || data.length() <= 4) return "***";
            
            return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
        }
        
        /**
         * Format duration for logging
         */
        public static String formatDuration(long milliseconds) {
            if (milliseconds < 1000) {
                return milliseconds + "ms";
            } else if (milliseconds < 60000) {
                return String.format("%.2fs", milliseconds / 1000.0);
            } else {
                long minutes = milliseconds / 60000;
                long seconds = (milliseconds % 60000) / 1000;
                return String.format("%dm %ds", minutes, seconds);
            }
        }
        
        /**
         * Create structured log entry
         */
        public static String createStructuredLog(String component, String operation, 
                                               String status, Object... details) {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(component).append("] ");
            sb.append(operation).append(" - ");
            sb.append("Status: ").append(status);
            
            if (details.length > 0) {
                sb.append(" | ");
                for (int i = 0; i < details.length; i += 2) {
                    if (i + 1 < details.length) {
                        sb.append(details[i]).append(": ").append(details[i + 1]);
                        if (i + 2 < details.length) sb.append(", ");
                    }
                }
            }
            
            return sb.toString();
        }
    }

    /**
     * Get current logging configuration summary
     */
    public String getLoggingConfigurationSummary() {
        return String.format("LoggingConfig{app=%s, spring=%s, hibernate=%s, logPath=%s, maxHistory=%d}",
                           applicationLogLevel, springLogLevel, hibernateLogLevel, logFilePath, maxHistory);
    }
}
