package com.treasurehunt.exception;

/**
 * Data access related exceptions
 */
public class DataAccessException extends TreasureHuntException {
    public DataAccessException(String message, String errorCode) {
        super(message, errorCode, ErrorCategory.DATA_ACCESS);
    }

    public DataAccessException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode, ErrorCategory.DATA_ACCESS);
    }
}
