package com.treasurehunt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Production error handler that sanitizes error responses
 * Prevents sensitive information leakage in production environment
 */
@Component
@Profile("production")
public class ProductionErrorHandler extends DefaultErrorAttributes {

    private static final Logger logger = LoggerFactory.getLogger(ProductionErrorHandler.class);

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        
        // Log the full error details for debugging (server-side only)
        logger.error("Production error occurred: {}", errorAttributes);
        
        // Sanitize error response for production (client-side)
        errorAttributes.remove("exception");
        errorAttributes.remove("trace");
        errorAttributes.remove("message"); // Remove detailed error messages
        
        // Replace with generic user-friendly message
        errorAttributes.put("message", "An error occurred while processing your request. Please try again later.");
        
        // Keep only essential information
        errorAttributes.put("timestamp", errorAttributes.get("timestamp"));
        errorAttributes.put("status", errorAttributes.get("status"));
        errorAttributes.put("error", getGenericErrorMessage((Integer) errorAttributes.get("status")));
        errorAttributes.put("path", errorAttributes.get("path"));
        
        return errorAttributes;
    }
    
    /**
     * Get generic error message based on HTTP status code
     */
    private String getGenericErrorMessage(Integer status) {
        if (status == null) {
            return "Internal Server Error";
        }
        
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Access Denied";
            case 404 -> "Page Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }
}
