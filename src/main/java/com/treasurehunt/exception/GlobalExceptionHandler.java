package com.treasurehunt.exception;

import com.treasurehunt.service.PerformanceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler that provides consistent error responses
 * Handles all application exceptions and converts them to appropriate HTTP responses
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final PerformanceMonitoringService performanceMonitoringService;

    @Autowired
    public GlobalExceptionHandler(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Standard error response structure
     */
    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final int status;
        private final LocalDateTime timestamp;
        private final String path;
        private Map<String, Object> details;

        public ErrorResponse(String error, String message, int status, String path) {
            this.error = error;
            this.message = message;
            this.status = status;
            this.path = path;
            this.timestamp = LocalDateTime.now();
            this.details = new HashMap<>();
        }

        // Getters
        public String getError() { return error; }
        public String getMessage() { return message; }
        public int getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getPath() { return path; }
        public Map<String, Object> getDetails() { return details; }

        public void addDetail(String key, Object value) {
            details.put(key, value);
        }
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logger.warn("Validation error: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );

        List<String> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        errorResponse.addDetail("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, WebRequest request) {
        
        logger.warn("Bind error: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "BIND_ERROR",
            "Request binding failed",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );

        List<String> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        errorResponse.addDetail("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        logger.warn("Constraint violation: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Data constraint violation",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );

        List<String> violations = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> 
            violations.add(violation.getPropertyPath() + ": " + violation.getMessage()));
        errorResponse.addDetail("violations", violations);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        logger.warn("Entity not found: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "ENTITY_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, WebRequest request) {
        
        logger.warn("Security violation: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "SECURITY_VIOLATION",
            "Access denied",
            HttpStatus.FORBIDDEN.value(),
            request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        logger.warn("Access denied: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "Insufficient privileges",
            HttpStatus.FORBIDDEN.value(),
            request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        
        logger.error("Data integrity violation", ex);
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "DATA_INTEGRITY_VIOLATION",
            "Data conflict occurred",
            HttpStatus.CONFLICT.value(),
            request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "FILE_SIZE_EXCEEDED",
            "Uploaded file size exceeds maximum allowed size",
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            request.getDescription(false)
        );

        errorResponse.addDetail("maxSize", ex.getMaxUploadSize());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false)
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        logger.error("Illegal state: {}", ex.getMessage());
        performanceMonitoringService.incrementErrorCount();

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        // Log full exception details for debugging (server-side only)
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        performanceMonitoringService.incrementErrorCount();

        // Create sanitized error response (no sensitive information)
        ErrorResponse errorResponse = new ErrorResponse(
            "RUNTIME_ERROR",
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            sanitizeRequestDescription(request.getDescription(false))
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        // Log full exception details for debugging (server-side only)
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        performanceMonitoringService.incrementErrorCount();

        // Create sanitized error response (no sensitive information)
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An internal server error occurred. Please contact support if the problem persists.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            sanitizeRequestDescription(request.getDescription(false))
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Sanitize request description to prevent information disclosure
     * @param description Original request description
     * @return Sanitized description
     */
    private String sanitizeRequestDescription(String description) {
        if (description == null) {
            return "Request information not available";
        }

        // Remove sensitive information from request description
        String sanitized = description.replaceAll("password=[^&\\s]*", "password=***");
        sanitized = sanitized.replaceAll("token=[^&\\s]*", "token=***");
        sanitized = sanitized.replaceAll("key=[^&\\s]*", "key=***");
        sanitized = sanitized.replaceAll("secret=[^&\\s]*", "secret=***");

        return sanitized;
    }
}
