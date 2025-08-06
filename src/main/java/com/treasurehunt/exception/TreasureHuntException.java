package com.treasurehunt.exception;

/**
 * Base exception class for all treasure hunt application exceptions
 * Provides common functionality and error categorization
 */
public abstract class TreasureHuntException extends RuntimeException {

    private final String errorCode;
    private final ErrorCategory category;

    /**
     * Error categories for better exception handling
     */
    public enum ErrorCategory {
        VALIDATION("Validation Error"),
        BUSINESS_LOGIC("Business Logic Error"),
        DATA_ACCESS("Data Access Error"),
        SECURITY("Security Error"),
        FILE_OPERATION("File Operation Error"),
        EXTERNAL_SERVICE("External Service Error"),
        CONFIGURATION("Configuration Error");

        private final String description;

        ErrorCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    protected TreasureHuntException(String message, String errorCode, ErrorCategory category) {
        super(message);
        this.errorCode = errorCode;
        this.category = category;
    }

    protected TreasureHuntException(String message, Throwable cause, String errorCode, ErrorCategory category) {
        super(message, cause);
        this.errorCode = errorCode;
        this.category = category;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]: %s", getClass().getSimpleName(), errorCode, getMessage());
    }
}

/**
 * Business logic related exceptions
 */
class BusinessLogicException extends TreasureHuntException {
    public BusinessLogicException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.BUSINESS_LOGIC);
    }

    public BusinessLogicException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.BUSINESS_LOGIC);
    }
}

/**
 * Security related exceptions
 */
class SecurityViolationException extends TreasureHuntException {
    public SecurityViolationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.SECURITY);
    }

    public SecurityViolationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.SECURITY);
    }
}

/**
 * File operation related exceptions
 */
class FileOperationException extends TreasureHuntException {
    public FileOperationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.FILE_OPERATION);
    }

    public FileOperationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.FILE_OPERATION);
    }
}

/**
 * External service related exceptions
 */
class ExternalServiceException extends TreasureHuntException {
    public ExternalServiceException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.EXTERNAL_SERVICE);
    }

    public ExternalServiceException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.EXTERNAL_SERVICE);
    }
}

/**
 * Configuration related exceptions
 */
class ConfigurationException extends TreasureHuntException {
    public ConfigurationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.CONFIGURATION);
    }

    public ConfigurationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.CONFIGURATION);
    }
}

/**
 * Specific business exceptions
 */

/**
 * Registration related exceptions
 */
class RegistrationException extends BusinessLogicException {
    public RegistrationException(String message) {
        super(message, "REG_ERROR");
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause, "REG_ERROR");
    }
}

/**
 * Plan related exceptions
 */
class PlanException extends BusinessLogicException {
    public PlanException(String message) {
        super(message, "PLAN_ERROR");
    }

    public PlanException(String message, Throwable cause) {
        super(message, cause, "PLAN_ERROR");
    }
}

/**
 * Email related exceptions
 */
class EmailException extends ExternalServiceException {
    public EmailException(String message) {
        super(message, "EMAIL_ERROR");
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause, "EMAIL_ERROR");
    }
}

/**
 * File upload related exceptions
 */
class FileUploadException extends FileOperationException {
    public FileUploadException(String message) {
        super(message, "UPLOAD_ERROR");
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause, "UPLOAD_ERROR");
    }
}

/**
 * Payment related exceptions
 */
class PaymentException extends ExternalServiceException {
    public PaymentException(String message) {
        super(message, "PAYMENT_ERROR");
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause, "PAYMENT_ERROR");
    }
}

/**
 * Authentication related exceptions
 */
class AuthenticationException extends SecurityViolationException {
    public AuthenticationException(String message) {
        super(message, "AUTH_ERROR");
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause, "AUTH_ERROR");
    }
}

/**
 * Authorization related exceptions
 */
class AuthorizationException extends SecurityViolationException {
    public AuthorizationException(String message) {
        super(message, "AUTHZ_ERROR");
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause, "AUTHZ_ERROR");
    }
}

/**
 * Resource conflict exceptions
 */
class ResourceConflictException extends DataAccessException {
    public ResourceConflictException(String message) {
        super(message, "RESOURCE_CONFLICT");
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause, "RESOURCE_CONFLICT");
    }
}
