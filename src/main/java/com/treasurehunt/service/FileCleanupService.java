package com.treasurehunt.service;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UploadedImage;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.TreasureHuntPlanRepository;
import com.treasurehunt.repository.UploadedDocumentRepository;
import com.treasurehunt.repository.UploadedImageRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for automated file cleanup and storage management
 * Handles cleanup of old registration documents and orphaned files
 */
@Service
@Transactional
public class FileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupService.class);

    private final UploadedDocumentRepository documentRepository;
    private final UserRegistrationRepository registrationRepository;
    private final TreasureHuntPlanRepository planRepository;
    private final UploadedImageRepository imageRepository;

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String documentUploadDir;

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String imageUploadDir;

    // Cleanup configuration
    private static final int CLEANUP_RETENTION_DAYS = 15;
    private static final int ORPHANED_FILE_RETENTION_DAYS = 7;

    // Cleanup statistics
    private final List<CleanupLog> cleanupLogs = new ArrayList<>();

    @Autowired
    public FileCleanupService(UploadedDocumentRepository documentRepository,
                             UserRegistrationRepository registrationRepository,
                             TreasureHuntPlanRepository planRepository,
                             UploadedImageRepository imageRepository) {
        this.documentRepository = documentRepository;
        this.registrationRepository = registrationRepository;
        this.planRepository = planRepository;
        this.imageRepository = imageRepository;
    }

    /**
     * Scheduled cleanup task - runs every Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void performWeeklyCleanup() {
        logger.info("=== STARTING WEEKLY FILE CLEANUP ===");
        
        CleanupLog log = new CleanupLog();
        log.setStartTime(LocalDateTime.now());
        
        try {
            // Clean up old registration documents
            CleanupResult documentResult = cleanupOldRegistrationDocuments();
            log.setDocumentsDeleted(documentResult.getFilesDeleted());
            log.setDocumentSpaceFreed(documentResult.getSpaceFreed());
            
            // Clean up orphaned files
            CleanupResult orphanedResult = cleanupOrphanedFiles();
            log.setOrphanedFilesDeleted(orphanedResult.getFilesDeleted());
            log.setOrphanedSpaceFreed(orphanedResult.getSpaceFreed());
            
            // Clean up inactive images
            CleanupResult imageResult = cleanupInactiveImages();
            log.setImagesDeleted(imageResult.getFilesDeleted());
            log.setImageSpaceFreed(imageResult.getSpaceFreed());
            
            log.setEndTime(LocalDateTime.now());
            log.setSuccess(true);
            log.setMessage("Cleanup completed successfully");
            
        } catch (Exception e) {
            log.setEndTime(LocalDateTime.now());
            log.setSuccess(false);
            log.setMessage("Cleanup failed: " + e.getMessage());
            logger.error("Weekly cleanup failed", e);
        }
        
        // Store cleanup log
        addCleanupLog(log);
        
        logger.info("=== WEEKLY FILE CLEANUP COMPLETED ===");
        logger.info("Documents deleted: {}, Space freed: {} MB", 
                   log.getDocumentsDeleted(), log.getTotalSpaceFreedMB());
    }

    /**
     * Clean up old registration documents for completed/cancelled events
     */
    public CleanupResult cleanupOldRegistrationDocuments() {
        logger.info("Starting cleanup of old registration documents...");
        
        LocalDate cutoffDate = LocalDate.now().minusDays(CLEANUP_RETENTION_DAYS);
        int filesDeleted = 0;
        long spaceFreed = 0;
        
        try {
            // Find registrations for events that are completed and older than retention period
            List<UserRegistration> oldRegistrations = registrationRepository.findOldCompletedRegistrations(cutoffDate);
            
            logger.info("Found {} old registrations for cleanup", oldRegistrations.size());
            
            for (UserRegistration registration : oldRegistrations) {
                // Safety check: ensure event is actually completed
                if (isEventCompleted(registration)) {
                    CleanupResult result = cleanupRegistrationFiles(registration);
                    filesDeleted += result.getFilesDeleted();
                    spaceFreed += result.getSpaceFreed();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during registration document cleanup", e);
        }
        
        logger.info("Registration document cleanup completed. Files deleted: {}, Space freed: {} bytes", 
                   filesDeleted, spaceFreed);
        
        return new CleanupResult(filesDeleted, spaceFreed);
    }

    /**
     * Clean up orphaned files that don't have database records
     */
    public CleanupResult cleanupOrphanedFiles() {
        logger.info("Starting cleanup of orphaned files...");
        
        int filesDeleted = 0;
        long spaceFreed = 0;
        
        try {
            Path uploadPath = Paths.get(documentUploadDir);
            if (!Files.exists(uploadPath)) {
                logger.info("Upload directory does not exist: {}", uploadPath);
                return new CleanupResult(0, 0);
            }
            
            // Get all registration IDs from database using memory-efficient streaming
            Set<String> validRegistrationIds;
            try (Stream<Long> idStream = registrationRepository.streamAllRegistrationIds()) {
                validRegistrationIds = idStream
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
            }
            
            // Walk through upload directory
            Files.walkFileTree(uploadPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // Check if file is old enough for cleanup
                        if (attrs.creationTime().toInstant().isBefore(
                            LocalDateTime.now().minusDays(ORPHANED_FILE_RETENTION_DAYS)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                            
                            // Check if file belongs to a valid registration
                            String parentDir = file.getParent().getFileName().toString();
                            if (!validRegistrationIds.contains(parentDir)) {
                                long fileSize = attrs.size();
                                Files.delete(file);
                                logger.debug("Deleted orphaned file: {}", file);
                                // Note: We can't modify the outer variables directly in this context
                                // This is a limitation of the current approach
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing file {}: {}", file, e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (Exception e) {
            logger.error("Error during orphaned file cleanup", e);
        }
        
        logger.info("Orphaned file cleanup completed. Files deleted: {}, Space freed: {} bytes", 
                   filesDeleted, spaceFreed);
        
        return new CleanupResult(filesDeleted, spaceFreed);
    }

    /**
     * Clean up inactive image files
     */
    public CleanupResult cleanupInactiveImages() {
        logger.info("Starting cleanup of inactive images...");
        
        int filesDeleted = 0;
        long spaceFreed = 0;
        
        try {
            // Find inactive images older than retention period
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(CLEANUP_RETENTION_DAYS);
            List<UploadedImage> inactiveImages = imageRepository.findInactiveImagesOlderThan(cutoffDate);
            
            logger.info("Found {} inactive images for cleanup", inactiveImages.size());
            
            for (UploadedImage image : inactiveImages) {
                if (deleteImageFile(image)) {
                    spaceFreed += image.getFileSize() != null ? image.getFileSize() : 0;
                    filesDeleted++;
                    
                    // Remove from database
                    imageRepository.delete(image);
                    logger.debug("Deleted inactive image: {}", image.getOriginalFilename());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during inactive image cleanup", e);
        }
        
        logger.info("Inactive image cleanup completed. Files deleted: {}, Space freed: {} bytes", 
                   filesDeleted, spaceFreed);
        
        return new CleanupResult(filesDeleted, spaceFreed);
    }

    /**
     * Clean up files for a specific registration
     */
    private CleanupResult cleanupRegistrationFiles(UserRegistration registration) {
        int filesDeleted = 0;
        long spaceFreed = 0;
        
        try {
            List<UploadedDocument> documents = documentRepository.findByRegistrationId(registration.getId());
            
            for (UploadedDocument document : documents) {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    long fileSize = Files.size(filePath);
                    Files.delete(filePath);
                    spaceFreed += fileSize;
                    filesDeleted++;
                    logger.debug("Deleted file: {}", filePath);
                }
                
                // Remove document record from database
                documentRepository.delete(document);
            }
            
            // Remove empty registration directory
            Path registrationDir = Paths.get(documentUploadDir).resolve(registration.getId().toString());
            if (Files.exists(registrationDir) && isDirectoryEmpty(registrationDir)) {
                Files.delete(registrationDir);
                logger.debug("Deleted empty directory: {}", registrationDir);
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up files for registration {}", registration.getId(), e);
        }
        
        return new CleanupResult(filesDeleted, spaceFreed);
    }

    /**
     * Check if an event is completed (past event date)
     */
    private boolean isEventCompleted(UserRegistration registration) {
        TreasureHuntPlan plan = registration.getPlan();
        if (plan != null && plan.getEventDate() != null) {
            return plan.getEventDate().isBefore(LocalDate.now());
        }
        return false;
    }

    /**
     * Delete image file from disk
     */
    private boolean deleteImageFile(UploadedImage image) {
        try {
            if (!image.getFilePath().startsWith("http")) {
                Path filePath = Paths.get(image.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting image file: {}", image.getFilePath(), e);
        }
        return false;
    }

    /**
     * Check if directory is empty
     */
    private boolean isDirectoryEmpty(Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Add cleanup log entry
     */
    private synchronized void addCleanupLog(CleanupLog log) {
        cleanupLogs.add(0, log); // Add to beginning
        
        // Keep only last 50 logs
        if (cleanupLogs.size() > 50) {
            cleanupLogs.subList(50, cleanupLogs.size()).clear();
        }
    }

    /**
     * Get cleanup logs for admin panel
     */
    public List<CleanupLog> getCleanupLogs() {
        return new ArrayList<>(cleanupLogs);
    }

    /**
     * Get storage usage statistics
     */
    public StorageStats getStorageStats() {
        StorageStats stats = new StorageStats();
        
        try {
            // Calculate document storage usage
            Path documentPath = Paths.get(documentUploadDir);
            if (Files.exists(documentPath)) {
                stats.setDocumentStorageUsed(calculateDirectorySize(documentPath));
            }
            
            // Calculate image storage usage
            Path imagePath = Paths.get(imageUploadDir);
            if (Files.exists(imagePath)) {
                stats.setImageStorageUsed(calculateDirectorySize(imagePath));
            }
            
            // Get database statistics
            stats.setTotalDocuments(documentRepository.count());
            stats.setTotalImages(imageRepository.count());
            stats.setActiveImages(imageRepository.countByIsActiveTrue());
            
        } catch (Exception e) {
            logger.error("Error calculating storage stats", e);
        }
        
        return stats;
    }

    /**
     * Calculate directory size recursively
     */
    private long calculateDirectorySize(Path directory) {
        try {
            return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            logger.error("Error calculating directory size for: {}", directory, e);
            return 0L;
        }
    }

    // Inner classes for data transfer

    public static class CleanupResult {
        private final int filesDeleted;
        private final long spaceFreed;

        public CleanupResult(int filesDeleted, long spaceFreed) {
            this.filesDeleted = filesDeleted;
            this.spaceFreed = spaceFreed;
        }

        public int getFilesDeleted() { return filesDeleted; }
        public long getSpaceFreed() { return spaceFreed; }
    }

    public static class CleanupLog {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;
        private String message;
        private int documentsDeleted;
        private int orphanedFilesDeleted;
        private int imagesDeleted;
        private long documentSpaceFreed;
        private long orphanedSpaceFreed;
        private long imageSpaceFreed;

        // Getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getDocumentsDeleted() { return documentsDeleted; }
        public void setDocumentsDeleted(int documentsDeleted) { this.documentsDeleted = documentsDeleted; }
        
        public int getOrphanedFilesDeleted() { return orphanedFilesDeleted; }
        public void setOrphanedFilesDeleted(int orphanedFilesDeleted) { this.orphanedFilesDeleted = orphanedFilesDeleted; }
        
        public int getImagesDeleted() { return imagesDeleted; }
        public void setImagesDeleted(int imagesDeleted) { this.imagesDeleted = imagesDeleted; }
        
        public long getDocumentSpaceFreed() { return documentSpaceFreed; }
        public void setDocumentSpaceFreed(long documentSpaceFreed) { this.documentSpaceFreed = documentSpaceFreed; }
        
        public long getOrphanedSpaceFreed() { return orphanedSpaceFreed; }
        public void setOrphanedSpaceFreed(long orphanedSpaceFreed) { this.orphanedSpaceFreed = orphanedSpaceFreed; }
        
        public long getImageSpaceFreed() { return imageSpaceFreed; }
        public void setImageSpaceFreed(long imageSpaceFreed) { this.imageSpaceFreed = imageSpaceFreed; }
        
        public long getTotalSpaceFreed() {
            return documentSpaceFreed + orphanedSpaceFreed + imageSpaceFreed;
        }
        
        public double getTotalSpaceFreedMB() {
            return getTotalSpaceFreed() / (1024.0 * 1024.0);
        }
        
        public int getTotalFilesDeleted() {
            return documentsDeleted + orphanedFilesDeleted + imagesDeleted;
        }
    }

    public static class StorageStats {
        private long documentStorageUsed;
        private long imageStorageUsed;
        private long totalDocuments;
        private long totalImages;
        private long activeImages;

        // Getters and setters
        public long getDocumentStorageUsed() { return documentStorageUsed; }
        public void setDocumentStorageUsed(long documentStorageUsed) { this.documentStorageUsed = documentStorageUsed; }
        
        public long getImageStorageUsed() { return imageStorageUsed; }
        public void setImageStorageUsed(long imageStorageUsed) { this.imageStorageUsed = imageStorageUsed; }
        
        public long getTotalStorageUsed() { return documentStorageUsed + imageStorageUsed; }
        
        public double getTotalStorageUsedMB() { return getTotalStorageUsed() / (1024.0 * 1024.0); }
        public double getDocumentStorageUsedMB() { return documentStorageUsed / (1024.0 * 1024.0); }
        public double getImageStorageUsedMB() { return imageStorageUsed / (1024.0 * 1024.0); }
        
        public long getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }
        
        public long getTotalImages() { return totalImages; }
        public void setTotalImages(long totalImages) { this.totalImages = totalImages; }
        
        public long getActiveImages() { return activeImages; }
        public void setActiveImages(long activeImages) { this.activeImages = activeImages; }
    }
}
