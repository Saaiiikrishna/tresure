package com.treasurehunt.repository;

import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository interface for UploadedDocument entity
 * Provides CRUD operations and custom queries for uploaded documents
 */
@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {

    /**
     * Find documents by registration
     * @param registration User registration
     * @return List of documents for the registration
     */
    List<UploadedDocument> findByRegistrationOrderByUploadDateDesc(UserRegistration registration);

    /**
     * Find documents by registration ID
     * @param registrationId Registration ID
     * @return List of documents for the registration
     */
    @Query("SELECT d FROM UploadedDocument d WHERE d.registration.id = :registrationId ORDER BY d.uploadDate DESC")
    List<UploadedDocument> findByRegistrationId(@Param("registrationId") Long registrationId);

    /**
     * Find documents by registration and document type
     * @param registration User registration
     * @param documentType Document type
     * @return List of documents matching criteria
     */
    List<UploadedDocument> findByRegistrationAndDocumentTypeOrderByUploadDateDesc(
            UserRegistration registration, UploadedDocument.DocumentType documentType);

    /**
     * Find document by registration and document type (expecting single result)
     * @param registration User registration
     * @param documentType Document type
     * @return Optional document if exists
     */
    Optional<UploadedDocument> findFirstByRegistrationAndDocumentTypeOrderByUploadDateDesc(
            UserRegistration registration, UploadedDocument.DocumentType documentType);

    /**
     * Find documents by stored filename
     * @param storedFilename Stored filename
     * @return Optional document if exists
     */
    Optional<UploadedDocument> findByStoredFilename(String storedFilename);

    /**
     * Find documents by content type
     * @param contentType Content type (MIME type)
     * @return List of documents with specified content type
     */
    List<UploadedDocument> findByContentTypeOrderByUploadDateDesc(String contentType);

    /**
     * Find documents uploaded within date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of documents uploaded within date range
     */
    List<UploadedDocument> findByUploadDateBetweenOrderByUploadDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count documents by registration
     * @param registration User registration
     * @return Number of documents for the registration
     */
    long countByRegistration(UserRegistration registration);

    /**
     * Count documents by document type
     * @param documentType Document type
     * @return Number of documents of specified type
     */
    long countByDocumentType(UploadedDocument.DocumentType documentType);

    /**
     * Calculate total file size for a registration
     * @param registration User registration
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(d.fileSizeBytes), 0) FROM UploadedDocument d WHERE d.registration = :registration")
    Long getTotalFileSizeByRegistration(@Param("registration") UserRegistration registration);

    /**
     * Find documents larger than specified size
     * @param sizeBytes Size threshold in bytes
     * @return List of documents larger than threshold
     */
    List<UploadedDocument> findByFileSizeBytesGreaterThanOrderByFileSizeBytesDesc(Long sizeBytes);

    /**
     * Get document statistics by type
     * @return List of document type statistics
     */
    @Query("SELECT d.documentType, COUNT(d), AVG(d.fileSizeBytes), SUM(d.fileSizeBytes) " +
           "FROM UploadedDocument d " +
           "GROUP BY d.documentType " +
           "ORDER BY d.documentType")
    List<Object[]> getDocumentStatisticsByType();

    /**
     * Find orphaned documents (registrations that were deleted)
     * @return List of orphaned documents
     */
    @Query("SELECT d FROM UploadedDocument d WHERE d.registration IS NULL")
    List<UploadedDocument> findOrphanedDocuments();

    /**
     * Delete documents by registration
     * @param registration User registration
     */
    void deleteByRegistration(UserRegistration registration);

    /**
     * Stream all file paths for memory-efficient processing
     * @return Stream of file paths
     */
    @Query("SELECT d.filePath FROM UploadedDocument d WHERE d.filePath IS NOT NULL")
    Stream<String> streamAllFilePaths();
}
