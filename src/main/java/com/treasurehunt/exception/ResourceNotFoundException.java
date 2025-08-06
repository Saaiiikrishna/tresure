package com.treasurehunt.exception;

/**
 * Resource not found exceptions
 */
public class ResourceNotFoundException extends DataAccessException {
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, "RESOURCE_NOT_FOUND");
    }
}
