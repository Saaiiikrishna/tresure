package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Critical configuration to validate Tomcat embedded dependencies at startup
 * Prevents NoClassDefFoundError for org.apache.catalina.connector.Response
 */
@Configuration
public class TomcatValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(TomcatValidationConfig.class);

    /**
     * Validate Tomcat embedded classes are available at startup
     * This prevents runtime NoClassDefFoundError issues
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateTomcatDependencies() {
        logger.info("=== TOMCAT DEPENDENCY VALIDATION STARTED ===");
        
        List<String> missingClasses = new ArrayList<>();
        List<String> criticalClasses = List.of(
            "org.apache.catalina.connector.Response",
            "org.apache.catalina.connector.Request", 
            "org.apache.catalina.connector.Connector",
            "org.apache.catalina.core.StandardContext",
            "org.apache.tomcat.embed.tomcat.TomcatEmbeddedContext",
            "org.apache.coyote.http11.Http11Processor",
            "org.apache.coyote.AbstractProcessorLight"
        );

        // Check each critical Tomcat class
        for (String className : criticalClasses) {
            try {
                Class.forName(className);
                logger.debug("✅ Found Tomcat class: {}", className);
            } catch (ClassNotFoundException e) {
                missingClasses.add(className);
                logger.error("❌ MISSING CRITICAL TOMCAT CLASS: {}", className);
            }
        }

        // Report results
        if (missingClasses.isEmpty()) {
            logger.info("✅ ALL TOMCAT DEPENDENCIES VALIDATED SUCCESSFULLY");
        } else {
            logger.error("❌ CRITICAL TOMCAT DEPENDENCY VALIDATION FAILED");
            logger.error("Missing {} critical Tomcat classes:", missingClasses.size());
            missingClasses.forEach(className -> logger.error("  - {}", className));
            
            // Provide fix instructions
            logger.error("");
            logger.error("🔧 TO FIX THIS ISSUE:");
            logger.error("1. Add explicit Tomcat dependencies to pom.xml:");
            logger.error("   <dependency>");
            logger.error("     <groupId>org.apache.tomcat.embed</groupId>");
            logger.error("     <artifactId>tomcat-embed-core</artifactId>");
            logger.error("     <version>10.1.16</version>");
            logger.error("   </dependency>");
            logger.error("2. Run: mvn clean compile -U");
            logger.error("3. Restart the application");
            
            // Don't fail startup, but log critical warning
            logger.warn("⚠️  APPLICATION MAY EXPERIENCE RUNTIME ERRORS DUE TO MISSING TOMCAT CLASSES");
        }
        
        logger.info("=== TOMCAT DEPENDENCY VALIDATION COMPLETED ===");
    }

    /**
     * Validate Tomcat version consistency
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateTomcatVersion() {
        try {
            // Get Tomcat version from embedded server
            String tomcatVersion = org.apache.catalina.util.ServerInfo.getServerInfo();
            logger.info("Detected Tomcat version: {}", tomcatVersion);
            
            // Check if version contains expected version
            if (tomcatVersion.contains("10.1.16")) {
                logger.info("✅ Tomcat version matches expected version (10.1.16)");
            } else {
                logger.warn("⚠️  Tomcat version mismatch - Expected: 10.1.16, Found: {}", tomcatVersion);
            }
            
        } catch (Exception e) {
            logger.error("❌ Could not determine Tomcat version: {}", e.getMessage());
        }
    }
}
