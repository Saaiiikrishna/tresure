package com.treasurehunt.service.cleanup;

import com.treasurehunt.repository.UploadedDocumentRepository;
import com.treasurehunt.repository.UploadedImageRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unified file cleanup service that consolidates all file cleanup operations
 * Eliminates code duplication and provides consistent cleanup behavior
 */
@Service
public class UnifiedFileCleanupService implements CleanupService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedFileCleanupService.class);

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String documentUploadDir;

    @Value("${app.file-storage.image-dir:uploads/images}")
    private String imageUploadDir;

    @Value("${app.file-storage.temp-dir:uploads/temp}")
    private String tempUploadDir;

    @Value("${app.cleanup.file-retention-days:30}")
    private int fileRetentionDays;

    private final UserRegistrationRepository registrationRepository;
    private final UploadedDocumentRepository documentRepository;
    private final UploadedImageRepository imageRepository;

    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
    private final AtomicLong totalFilesDeleted = new AtomicLong(0);
    private final AtomicLong totalSpaceFreed = new AtomicLong(0);
    private final AtomicInteger totalErrors = new AtomicInteger(0);

    @Autowired
    public UnifiedFileCleanupService(UserRegistrationRepository registrationRepository,
                                   UploadedDocumentRepository documentRepository,
                                   UploadedImageRepository imageRepository) {
        this.registrationRepository = registrationRepository;
        this.documentRepository = documentRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    public CleanupResult performCleanup(CleanupType type) {
        if (!cleanupInProgress.compareAndSet(false, true)) {
            return new CleanupResult(0, 0, 1, 0, "Cleanup already in progress");
        }

        long startTime = System.currentTimeMillis();
        
        try {
            switch (type) {
                case ORPHANED_FILES:
                    return cleanupOrphanedFiles();
                case TEMPORARY_FILES:
                    return cleanupTemporaryFiles();
                case OLD_LOGS:
                    return cleanupOldLogFiles();
                default:
                    return new CleanupResult(0, 0, 1, 0, "Unsupported cleanup type: " + type);
            }
        } finally {
            cleanupInProgress.set(false);
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Cleanup operation {} completed in {}ms", type, executionTime);
        }
    }

    @Override
    public CompletableFuture<CleanupResult> performCleanupAsync(CleanupType type) {
        return CompletableFuture.supplyAsync(() -> performCleanup(type));
    }

    /**
     * Clean up orphaned files (files without database records)
     */
    private CleanupResult cleanupOrphanedFiles() {
        logger.info("Starting orphaned file cleanup");
        long startTime = System.currentTimeMillis();
        
        AtomicInteger filesDeleted = new AtomicInteger(0);
        AtomicLong spaceFreed = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);

        try {
            // Get valid file paths using memory-efficient streaming
            Set<String> validDocumentPaths = getValidDocumentPaths();
            Set<String> validImagePaths = getValidImagePaths();
            Set<String> validRegistrationIds = getValidRegistrationIds();

            // Clean up document directory
            CleanupResult docResult = cleanupDirectoryOrphans(
                documentUploadDir, validDocumentPaths, validRegistrationIds);
            filesDeleted.addAndGet(docResult.getFilesDeleted());
            spaceFreed.addAndGet(docResult.getSpaceFreed());
            errors.addAndGet(docResult.getErrorsEncountered());

            // Clean up image directory
            CleanupResult imgResult = cleanupDirectoryOrphans(
                imageUploadDir, validImagePaths, null);
            filesDeleted.addAndGet(imgResult.getFilesDeleted());
            spaceFreed.addAndGet(imgResult.getSpaceFreed());
            errors.addAndGet(imgResult.getErrorsEncountered());

            long executionTime = System.currentTimeMillis() - startTime;
            String summary = String.format("Orphaned file cleanup: %d files deleted, %d bytes freed", 
                                         filesDeleted.get(), spaceFreed.get());

            // Update global counters
            totalFilesDeleted.addAndGet(filesDeleted.get());
            totalSpaceFreed.addAndGet(spaceFreed.get());
            totalErrors.addAndGet(errors.get());

            return new CleanupResult(filesDeleted.get(), spaceFreed.get(), errors.get(), 
                                   executionTime, summary);

        } catch (Exception e) {
            logger.error("Error during orphaned file cleanup", e);
            long executionTime = System.currentTimeMillis() - startTime;
            return new CleanupResult(0, 0, 1, executionTime, "Cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Clean up temporary files older than retention period
     */
    private CleanupResult cleanupTemporaryFiles() {
        logger.info("Starting temporary file cleanup");
        long startTime = System.currentTimeMillis();
        
        AtomicInteger filesDeleted = new AtomicInteger(0);
        AtomicLong spaceFreed = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);

        try {
            Path tempDir = Paths.get(tempUploadDir);
            if (!Files.exists(tempDir)) {
                return new CleanupResult(0, 0, 0, 0, "Temporary directory does not exist");
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1); // 1 day for temp files

            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                        
                        if (fileTime.isBefore(cutoffDate)) {
                            long fileSize = attrs.size();
                            Files.delete(file);
                            filesDeleted.incrementAndGet();
                            spaceFreed.addAndGet(fileSize);
                            logger.debug("Deleted temporary file: {}", file);
                        }
                    } catch (IOException e) {
                        logger.warn("Error deleting temporary file {}: {}", file, e.getMessage());
                        errors.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            long executionTime = System.currentTimeMillis() - startTime;
            String summary = String.format("Temporary file cleanup: %d files deleted, %d bytes freed", 
                                         filesDeleted.get(), spaceFreed.get());

            return new CleanupResult(filesDeleted.get(), spaceFreed.get(), errors.get(), 
                                   executionTime, summary);

        } catch (Exception e) {
            logger.error("Error during temporary file cleanup", e);
            long executionTime = System.currentTimeMillis() - startTime;
            return new CleanupResult(0, 0, 1, executionTime, "Cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Clean up old log files
     */
    private CleanupResult cleanupOldLogFiles() {
        logger.info("Starting old log file cleanup");
        long startTime = System.currentTimeMillis();
        
        // This is a placeholder implementation
        // In a real application, you would clean up actual log files
        
        long executionTime = System.currentTimeMillis() - startTime;
        return new CleanupResult(0, 0, 0, executionTime, "Log file cleanup not implemented");
    }

    /**
     * Clean up orphaned files in a specific directory
     */
    private CleanupResult cleanupDirectoryOrphans(String directoryPath, Set<String> validPaths, 
                                                 Set<String> validRegistrationIds) {
        AtomicInteger filesDeleted = new AtomicInteger(0);
        AtomicLong spaceFreed = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);

        try {
            Path dir = Paths.get(directoryPath);
            if (!Files.exists(dir)) {
                return new CleanupResult(0, 0, 0, 0, "Directory does not exist: " + directoryPath);
            }

            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filePath = file.toString();
                    boolean isOrphaned = !validPaths.contains(filePath);
                    
                    // Additional check for registration-based files
                    if (!isOrphaned && validRegistrationIds != null) {
                        String fileName = file.getFileName().toString();
                        boolean hasValidRegistrationId = validRegistrationIds.stream()
                            .anyMatch(fileName::contains);
                        isOrphaned = !hasValidRegistrationId;
                    }

                    if (isOrphaned) {
                        try {
                            long fileSize = attrs.size();
                            Files.delete(file);
                            filesDeleted.incrementAndGet();
                            spaceFreed.addAndGet(fileSize);
                            logger.debug("Deleted orphaned file: {}", file);
                        } catch (IOException e) {
                            logger.warn("Error deleting orphaned file {}: {}", file, e.getMessage());
                            errors.incrementAndGet();
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (Exception e) {
            logger.error("Error cleaning directory {}: {}", directoryPath, e.getMessage());
            errors.incrementAndGet();
        }

        return new CleanupResult(filesDeleted.get(), spaceFreed.get(), errors.get(), 0, 
                               "Directory cleanup completed");
    }

    /**
     * Get valid document paths using memory-efficient streaming
     */
    private Set<String> getValidDocumentPaths() {
        try (Stream<String> pathStream = documentRepository.streamAllFilePaths()) {
            return pathStream
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
    }

    /**
     * Get valid image paths using memory-efficient streaming
     */
    private Set<String> getValidImagePaths() {
        try (Stream<String> pathStream = imageRepository.streamAllFilePaths()) {
            return pathStream
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
    }

    /**
     * Get valid registration IDs using memory-efficient streaming
     */
    private Set<String> getValidRegistrationIds() {
        try (Stream<Long> idStream = registrationRepository.streamAllRegistrationIds()) {
            return idStream
                .map(String::valueOf)
                .collect(Collectors.toSet());
        }
    }

    @Override
    public String getCleanupStatistics() {
        return String.format("Cleanup Statistics - Files Deleted: %d, Space Freed: %d bytes, Errors: %d",
                           totalFilesDeleted.get(), totalSpaceFreed.get(), totalErrors.get());
    }

    @Override
    public boolean isCleanupInProgress() {
        return cleanupInProgress.get();
    }

    @Override
    public CleanupType[] getSupportedCleanupTypes() {
        return new CleanupType[]{
            CleanupType.ORPHANED_FILES,
            CleanupType.TEMPORARY_FILES,
            CleanupType.OLD_LOGS
        };
    }

    @Override
    public boolean validateConfiguration() {
        boolean valid = true;
        
        if (documentUploadDir == null || documentUploadDir.trim().isEmpty()) {
            logger.error("Document upload directory not configured");
            valid = false;
        }
        
        if (imageUploadDir == null || imageUploadDir.trim().isEmpty()) {
            logger.error("Image upload directory not configured");
            valid = false;
        }
        
        if (fileRetentionDays <= 0) {
            logger.error("File retention days must be positive");
            valid = false;
        }
        
        return valid;
    }

    /**
     * Reset cleanup statistics
     */
    public void resetStatistics() {
        totalFilesDeleted.set(0);
        totalSpaceFreed.set(0);
        totalErrors.set(0);
        logger.info("Cleanup statistics reset");
    }
}
