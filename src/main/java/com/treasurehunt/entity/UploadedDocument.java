package com.treasurehunt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an Uploaded Document for a User Registration
 * Maps to uploaded_documents table in PostgreSQL database
 */
@Entity
@Table(name = "uploaded_documents")
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private UserRegistration registration;

    @NotNull(message = "Document type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @NotBlank(message = "Stored filename is required")
    @Size(max = 255, message = "Stored filename must not exceed 255 characters")
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @NotBlank(message = "File path is required")
    @Size(max = 500, message = "File path must not exceed 500 characters")
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0")
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    // Enums
    public enum DocumentType {
        PHOTO, ID_DOCUMENT, MEDICAL_CERTIFICATE
    }

    // Constructors
    public UploadedDocument() {}

    public UploadedDocument(UserRegistration registration, DocumentType documentType,
                           String originalFilename, String storedFilename, String filePath,
                           Long fileSizeBytes, String contentType) {
        this.registration = registration;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.contentType = contentType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserRegistration getRegistration() { return registration; }
    public void setRegistration(UserRegistration registration) { this.registration = registration; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    // Helper methods
    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "0 B";
        
        long bytes = fileSizeBytes;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }
}
