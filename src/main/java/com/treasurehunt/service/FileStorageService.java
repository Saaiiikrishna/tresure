package com.treasurehunt.service;

import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.UploadedDocumentRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Service class for handling file storage operations
 * Manages file uploads, validation, and storage for user registrations
 */
@Service
@Transactional
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final UploadedDocumentRepository documentRepository;
    private final Path fileStorageLocation;

    // File size limits
    @Value("${app.file-storage.max-photo-size:2097152}")
    private long maxPhotoSize;

    @Value("${app.file-storage.max-document-size:5242880}")
    private long maxDocumentSize;

    // Allowed content types
    @Value("${app.file-storage.allowed-photo-types:image/jpeg,image/jpg,image/png}")
    private String allowedPhotoTypes;

    @Value("${app.file-storage.allowed-document-types:application/pdf,image/jpeg,image/jpg}")
    private String allowedDocumentTypes;

    public FileStorageService(UploadedDocumentRepository documentRepository,
                             @Value("${app.file-storage.upload-dir:uploads/documents}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage location initialized: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store uploaded file for a registration
     * @param file Uploaded file
     * @param registration User registration
     * @param documentType Document type
     * @return Saved document entity
     * @throws IOException if file storage fails
     */
    public UploadedDocument storeFile(MultipartFile file, UserRegistration registration, 
                                    UploadedDocument.DocumentType documentType) throws IOException {
        
        logger.info("Storing file {} for registration ID: {}, type: {}", 
                   file.getOriginalFilename(), registration.getId(), documentType);

        // Validate file
        validateFile(file, documentType);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = FilenameUtils.getExtension(originalFilename);
        String storedFilename = generateStoredFilename(registration.getId(), documentType, fileExtension);

        // Create registration-specific directory
        Path registrationDir = this.fileStorageLocation.resolve(registration.getId().toString());
        Files.createDirectories(registrationDir);

        // Store file
        Path targetLocation = registrationDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create document entity
        UploadedDocument document = new UploadedDocument(
                registration,
                documentType,
                originalFilename,
                storedFilename,
                targetLocation.toString(),
                file.getSize(),
                file.getContentType()
        );

        UploadedDocument savedDocument = documentRepository.save(document);
        logger.info("Successfully stored file with ID: {}", savedDocument.getId());

        return savedDocument;
    }

    /**
     * Get file path for a document
     * @param documentId Document ID
     * @return File path
     * @throws IllegalArgumentException if document not found
     */
    @Transactional(readOnly = true)
    public Path getFilePath(Long documentId) {
        UploadedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        return Paths.get(document.getFilePath());
    }

    /**
     * Delete file and document record
     * @param documentId Document ID
     * @throws IOException if file deletion fails
     */
    public void deleteFile(Long documentId) throws IOException {
        logger.info("Deleting file with document ID: {}", documentId);
        
        UploadedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // Delete physical file
        Path filePath = Paths.get(document.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            logger.debug("Deleted physical file: {}", filePath);
        }

        // Delete document record
        documentRepository.delete(document);
        logger.info("Successfully deleted document with ID: {}", documentId);
    }

    /**
     * Delete all files for a registration
     * @param registration User registration
     * @throws IOException if file deletion fails
     */
    public void deleteAllFilesForRegistration(UserRegistration registration) throws IOException {
        logger.info("Deleting all files for registration ID: {}", registration.getId());
        
        List<UploadedDocument> documents = documentRepository.findByRegistrationOrderByUploadDateDesc(registration);
        
        for (UploadedDocument document : documents) {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.debug("Deleted physical file: {}", filePath);
            }
        }

        // Delete document records
        documentRepository.deleteByRegistration(registration);
        
        // Try to delete registration directory if empty
        Path registrationDir = this.fileStorageLocation.resolve(registration.getId().toString());
        if (Files.exists(registrationDir) && Files.list(registrationDir).findAny().isEmpty()) {
            Files.delete(registrationDir);
            logger.debug("Deleted empty registration directory: {}", registrationDir);
        }
        
        logger.info("Successfully deleted all files for registration ID: {}", registration.getId());
    }

    /**
     * Validate uploaded file
     * @param file Uploaded file
     * @param documentType Document type
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file, UploadedDocument.DocumentType documentType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type is unknown");
        }

        // Validate file size
        long maxSize = getMaxSizeForDocumentType(documentType);
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d bytes", maxSize));
        }

        // Validate content type
        Set<String> allowedTypes = getAllowedTypesForDocumentType(documentType);
        if (!allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("File type %s is not allowed for %s", contentType, documentType));
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!isValidExtensionForDocumentType(extension, documentType)) {
            throw new IllegalArgumentException(
                String.format("File extension .%s is not allowed for %s", extension, documentType));
        }
    }

    /**
     * Generate stored filename with timestamp
     * @param registrationId Registration ID
     * @param documentType Document type
     * @param extension File extension
     * @return Generated filename
     */
    private String generateStoredFilename(Long registrationId, UploadedDocument.DocumentType documentType, 
                                        String extension) {
        long timestamp = Instant.now().toEpochMilli();
        return String.format("%d_%s_%d.%s", registrationId, documentType.name(), timestamp, extension);
    }

    /**
     * Get maximum file size for document type
     * @param documentType Document type
     * @return Maximum size in bytes
     */
    private long getMaxSizeForDocumentType(UploadedDocument.DocumentType documentType) {
        return documentType == UploadedDocument.DocumentType.PHOTO ? maxPhotoSize : maxDocumentSize;
    }

    /**
     * Get allowed content types for document type
     * @param documentType Document type
     * @return Set of allowed content types
     */
    private Set<String> getAllowedTypesForDocumentType(UploadedDocument.DocumentType documentType) {
        String allowedTypes = documentType == UploadedDocument.DocumentType.PHOTO 
                ? allowedPhotoTypes : allowedDocumentTypes;
        return Set.of(allowedTypes.toLowerCase().split(","));
    }

    /**
     * Check if file extension is valid for document type
     * @param extension File extension
     * @param documentType Document type
     * @return true if valid
     */
    private boolean isValidExtensionForDocumentType(String extension, UploadedDocument.DocumentType documentType) {
        switch (documentType) {
            case PHOTO:
                return Arrays.asList("jpg", "jpeg", "png").contains(extension);
            case ID_DOCUMENT:
                return Arrays.asList("pdf", "jpg", "jpeg").contains(extension);
            case MEDICAL_CERTIFICATE:
                return "pdf".equals(extension);
            default:
                return false;
        }
    }
}
