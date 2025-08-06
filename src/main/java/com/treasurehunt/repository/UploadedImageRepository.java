package com.treasurehunt.repository;

import com.treasurehunt.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository interface for UploadedImage entity
 * Provides data access methods for image management
 */
@Repository
public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    /**
     * Find all active images
     * @return List of active uploaded images
     */
    List<UploadedImage> findByIsActiveTrueOrderByUploadDateDesc();

    /**
     * Find active images by category
     * @param category Image category
     * @return List of active images in the specified category
     */
    List<UploadedImage> findByImageCategoryAndIsActiveTrueOrderByUploadDateDesc(String category);

    /**
     * Find the most recent active image by category
     * @param category Image category
     * @return Optional of the most recent active image in the category
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.imageCategory = :category AND ui.isActive = true ORDER BY ui.uploadDate DESC")
    Optional<UploadedImage> findLatestByCategory(@Param("category") String category);

    /**
     * Find images by uploaded user
     * @param uploadedBy Username of the uploader
     * @return List of images uploaded by the specified user
     */
    List<UploadedImage> findByUploadedByOrderByUploadDateDesc(String uploadedBy);

    /**
     * Find images by content type
     * @param contentType MIME content type
     * @return List of images with the specified content type
     */
    List<UploadedImage> findByContentTypeContainingIgnoreCaseOrderByUploadDateDesc(String contentType);

    /**
     * Find all images (active and inactive) by category
     * @param category Image category
     * @return List of all images in the specified category
     */
    List<UploadedImage> findByImageCategoryOrderByUploadDateDesc(String category);

    /**
     * Count active images by category
     * @param category Image category
     * @return Number of active images in the category
     */
    long countByImageCategoryAndIsActiveTrue(String category);

    /**
     * Find images by file size range
     * @param minSize Minimum file size in bytes
     * @param maxSize Maximum file size in bytes
     * @return List of images within the size range
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.fileSize BETWEEN :minSize AND :maxSize AND ui.isActive = true ORDER BY ui.uploadDate DESC")
    List<UploadedImage> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * Search images by filename or alt text
     * @param searchTerm Search term to match against filename or alt text
     * @return List of matching images
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.isActive = true AND " +
           "(LOWER(ui.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ui.altText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ui.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY ui.uploadDate DESC")
    List<UploadedImage> searchImages(@Param("searchTerm") String searchTerm);

    /**
     * Find duplicate files by stored filename
     * @param storedFilename Stored filename to check
     * @return List of images with the same stored filename
     */
    List<UploadedImage> findByStoredFilename(String storedFilename);

    /**
     * Get total storage used by all images
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(ui.fileSize), 0) FROM UploadedImage ui WHERE ui.isActive = true")
    Long getTotalStorageUsed();

    /**
     * Get storage used by category
     * @param category Image category
     * @return Total file size in bytes for the category
     */
    @Query("SELECT COALESCE(SUM(ui.fileSize), 0) FROM UploadedImage ui WHERE ui.imageCategory = :category AND ui.isActive = true")
    Long getStorageUsedByCategory(@Param("category") String category);

    /**
     * Find images uploaded in the last N days
     * @param days Number of days to look back
     * @return List of recently uploaded images
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.uploadDate >= CURRENT_TIMESTAMP - :days DAY AND ui.isActive = true ORDER BY ui.uploadDate DESC")
    List<UploadedImage> findRecentImages(@Param("days") int days);

    /**
     * Soft delete image by setting isActive to false
     * @param id Image ID
     * @return Number of affected rows
     */
    @Modifying
    @Query("UPDATE UploadedImage ui SET ui.isActive = false WHERE ui.id = :id")
    int softDeleteById(@Param("id") Long id);

    /**
     * Restore soft deleted image
     * @param id Image ID
     * @return Number of affected rows
     */
    @Modifying
    @Query("UPDATE UploadedImage ui SET ui.isActive = true WHERE ui.id = :id")
    int restoreById(@Param("id") Long id);

    /**
     * Atomically deactivate all images in a category
     * @param category Image category
     * @return Number of images deactivated
     */
    @Modifying
    @Query("UPDATE UploadedImage ui SET ui.isActive = false WHERE ui.imageCategory = :category AND ui.isActive = true")
    int deactivateImagesByCategory(@Param("category") String category);

    /**
     * Find inactive images older than specified date
     * @param cutoffDate Date before which images are considered old
     * @return List of inactive images older than cutoff date
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.isActive = false AND ui.uploadDate < :cutoffDate")
    List<UploadedImage> findInactiveImagesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count active images
     * @return Number of active images
     */
    long countByIsActiveTrue();

    /**
     * Stream all file paths for memory-efficient processing
     * @return Stream of file paths
     */
    @Query("SELECT i.filePath FROM UploadedImage i WHERE i.filePath IS NOT NULL")
    Stream<String> streamAllFilePaths();
}
