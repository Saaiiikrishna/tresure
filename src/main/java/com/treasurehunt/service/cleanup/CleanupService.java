package com.treasurehunt.service.cleanup;

import java.util.concurrent.CompletableFuture;

/**
 * Common interface for all cleanup operations
 * Provides standardized cleanup methods and result tracking
 */
public interface CleanupService {

    /**
     * Result of a cleanup operation
     */
    class CleanupResult {
        private final int filesDeleted;
        private final long spaceFreed;
        private final int errorsEncountered;
        private final long executionTimeMs;
        private final String summary;

        public CleanupResult(int filesDeleted, long spaceFreed, int errorsEncountered, 
                           long executionTimeMs, String summary) {
            this.filesDeleted = filesDeleted;
            this.spaceFreed = spaceFreed;
            this.errorsEncountered = errorsEncountered;
            this.executionTimeMs = executionTimeMs;
            this.summary = summary;
        }

        // Getters
        public int getFilesDeleted() { return filesDeleted; }
        public long getSpaceFreed() { return spaceFreed; }
        public int getErrorsEncountered() { return errorsEncountered; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getSummary() { return summary; }

        public boolean isSuccessful() {
            return errorsEncountered == 0;
        }

        @Override
        public String toString() {
            return String.format("CleanupResult{files=%d, space=%d bytes, errors=%d, time=%dms, summary='%s'}",
                               filesDeleted, spaceFreed, errorsEncountered, executionTimeMs, summary);
        }
    }

    /**
     * Cleanup type enumeration
     */
    enum CleanupType {
        ORPHANED_FILES("Orphaned Files"),
        OLD_LOGS("Old Log Files"),
        TEMPORARY_FILES("Temporary Files"),
        EXPIRED_SESSIONS("Expired Sessions"),
        FAILED_UPLOADS("Failed Uploads");

        private final String description;

        CleanupType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Perform cleanup operation synchronously
     * @param type Type of cleanup to perform
     * @return Cleanup result
     */
    CleanupResult performCleanup(CleanupType type);

    /**
     * Perform cleanup operation asynchronously
     * @param type Type of cleanup to perform
     * @return CompletableFuture with cleanup result
     */
    CompletableFuture<CleanupResult> performCleanupAsync(CleanupType type);

    /**
     * Get cleanup statistics
     * @return Statistics summary
     */
    String getCleanupStatistics();

    /**
     * Check if cleanup is currently running
     * @return true if cleanup is in progress
     */
    boolean isCleanupInProgress();

    /**
     * Get supported cleanup types
     * @return Array of supported cleanup types
     */
    CleanupType[] getSupportedCleanupTypes();

    /**
     * Validate cleanup configuration
     * @return true if configuration is valid
     */
    boolean validateConfiguration();
}
