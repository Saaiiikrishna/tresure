package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 * Input Sanitization Service
 * Provides comprehensive input validation and sanitization to prevent security vulnerabilities
 */
@Service
public class InputSanitizationService {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationService.class);

    // Security patterns for detection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<iframe|</iframe)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;|\\||&|`|\\$\\(|\\$\\{|<\\(|>\\(|\\|\\||&&)",
        Pattern.CASE_INSENSITIVE
    );

    // HTML entities for encoding
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();
    static {
        HTML_ENTITIES.put("&", "&amp;");
        HTML_ENTITIES.put("<", "&lt;");
        HTML_ENTITIES.put(">", "&gt;");
        HTML_ENTITIES.put("\"", "&quot;");
        HTML_ENTITIES.put("'", "&#x27;");
        HTML_ENTITIES.put("/", "&#x2F;");
    }

    // Allowed characters for different input types
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{10,15}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-\\.]{1,100}$");

    /**
     * Sanitize general text input
     * @param input Raw input string
     * @return Sanitized string
     */
    public String sanitizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }

        String sanitized = input.trim();
        
        // Remove null bytes
        sanitized = sanitized.replace("\0", "");
        
        // Detect and log potential security threats
        detectSecurityThreats(sanitized);
        
        // HTML encode dangerous characters
        sanitized = htmlEncode(sanitized);
        
        // Limit length to prevent DoS
        if (sanitized.length() > 1000) {
            logger.warn("Input truncated due to excessive length: {} characters", sanitized.length());
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized;
    }

    /**
     * Sanitize HTML content (more restrictive)
     * @param input Raw HTML input
     * @return Sanitized HTML
     */
    public String sanitizeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }

        String sanitized = input.trim();
        
        // Remove script tags and their content
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        // Remove dangerous attributes
        sanitized = sanitized.replaceAll("(?i)\\s*(on\\w+|javascript:|vbscript:)[^\\s>]*", "");
        
        // Remove dangerous tags
        List<String> dangerousTags = Arrays.asList("script", "iframe", "object", "embed", "form", "input");
        for (String tag : dangerousTags) {
            sanitized = sanitized.replaceAll("(?i)</?\\s*" + tag + "[^>]*>", "");
        }
        
        return sanitized;
    }

    /**
     * Sanitize filename for safe storage
     * @param filename Original filename
     * @return Safe filename
     */
    public String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "unnamed_file";
        }

        String sanitized = filename.trim();
        
        // Remove path traversal attempts
        sanitized = sanitized.replaceAll("[\\\\/]", "_");
        
        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Prevent hidden files
        if (sanitized.startsWith(".")) {
            sanitized = "file_" + sanitized;
        }
        
        // Limit length
        if (sanitized.length() > 100) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, Math.min(95, lastDot)) + extension;
            } else {
                sanitized = sanitized.substring(0, 100);
            }
        }
        
        return sanitized;
    }

    /**
     * Validate and sanitize email address
     * @param email Email address
     * @return Sanitized email or null if invalid
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        String sanitized = email.trim().toLowerCase();
        
        // Basic format validation
        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            logger.warn("Invalid email format detected: {}", maskEmail(email));
            return null;
        }
        
        // Check for suspicious patterns
        if (containsSuspiciousPatterns(sanitized)) {
            logger.warn("Suspicious email detected: {}", maskEmail(email));
            return null;
        }
        
        return sanitized;
    }

    /**
     * Validate and sanitize phone number
     * @param phone Phone number
     * @return Sanitized phone number or null if invalid
     */
    public String sanitizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        String sanitized = phone.trim();
        
        // Remove common formatting characters
        sanitized = sanitized.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Validate pattern
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            logger.warn("Invalid phone format detected");
            return null;
        }
        
        return sanitized;
    }

    /**
     * Validate and sanitize name fields
     * @param name Name input
     * @return Sanitized name or null if invalid
     */
    public String sanitizeName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        String sanitized = name.trim();
        
        // Check for valid name pattern
        if (!NAME_PATTERN.matcher(sanitized).matches()) {
            logger.warn("Invalid name format detected");
            return null;
        }
        
        // Check for suspicious patterns
        if (containsSuspiciousPatterns(sanitized)) {
            logger.warn("Suspicious name detected");
            return null;
        }
        
        return sanitized;
    }

    /**
     * Detect potential security threats in input
     * @param input Input string to analyze
     */
    private void detectSecurityThreats(String input) {
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("Potential SQL injection attempt detected in input");
        }
        
        if (XSS_PATTERN.matcher(input).find()) {
            logger.warn("Potential XSS attempt detected in input");
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            logger.warn("Potential path traversal attempt detected in input");
        }
        
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("Potential command injection attempt detected in input");
        }
    }

    /**
     * Check if input contains suspicious patterns
     * @param input Input to check
     * @return true if suspicious patterns found
     */
    private boolean containsSuspiciousPatterns(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).find() ||
               XSS_PATTERN.matcher(input).find() ||
               PATH_TRAVERSAL_PATTERN.matcher(input).find() ||
               COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * HTML encode dangerous characters
     * @param input Input string
     * @return HTML encoded string
     */
    private String htmlEncode(String input) {
        String encoded = input;
        for (Map.Entry<String, String> entity : HTML_ENTITIES.entrySet()) {
            encoded = encoded.replace(entity.getKey(), entity.getValue());
        }
        return encoded;
    }

    /**
     * Mask email for logging (security)
     * @param email Email to mask
     * @return Masked email
     */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        String maskedLocal = localPart.length() > 2 
            ? localPart.substring(0, 2) + "***" 
            : "***";
            
        return maskedLocal + "@" + domain;
    }

    /**
     * Validate input length
     * @param input Input string
     * @param maxLength Maximum allowed length
     * @param fieldName Field name for logging
     * @return true if valid length
     */
    public boolean validateLength(String input, int maxLength, String fieldName) {
        if (input != null && input.length() > maxLength) {
            logger.warn("Input too long for field {}: {} characters (max: {})", 
                       fieldName, input.length(), maxLength);
            return false;
        }
        return true;
    }

    /**
     * Sanitize search query to prevent injection attacks
     * @param query Search query
     * @return Sanitized query
     */
    public String sanitizeSearchQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }

        String sanitized = query.trim();
        
        // Remove SQL injection patterns
        sanitized = sanitized.replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter)", "");
        
        // Remove special characters that could be used for injection
        sanitized = sanitized.replaceAll("[';\"\\\\]", "");
        
        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized;
    }
}
