package com.treasurehunt.exception;

/**
 * Validation related exceptions
 */
public class ValidationException extends TreasureHuntException {
    public ValidationException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.VALIDATION);
    }

    public ValidationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.VALIDATION);
    }
}
