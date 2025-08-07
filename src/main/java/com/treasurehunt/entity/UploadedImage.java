package com.treasurehunt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing uploaded images for the application
 * Used for admin image management functionality
 */
@Entity
@Table(name = "uploaded_images")
public class UploadedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @NotBlank(message = "Image category is required")
    @Size(max = 50, message = "Image category must not exceed 50 characters")
    @Column(name = "image_category", nullable = false)
    private String imageCategory;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @Size(max = 100, message = "Uploaded by must not exceed 100 characters")
    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Size(max = 500, message = "Alt text must not exceed 500 characters")
    @Column(name = "alt_text")
    private String altText;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(name = "description", length = 2000)
    private String description;

    // Image categories enum
    public enum ImageCategory {
        HERO_VIDEO("hero_video", "Hero Background Video"),
        HERO_FALLBACK("hero_fallback", "Hero Fallback Image"),
        ABOUT_SECTION("about_section", "About Section Image"),
        CONTACT_BACKGROUND("contact_background", "Contact Background Image");

        private final String value;
        private final String displayName;

        ImageCategory(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ImageCategory fromValue(String value) {
            for (ImageCategory category : values()) {
                if (category.value.equals(value)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Unknown image category: " + value);
        }
    }

    // Constructors
    public UploadedImage() {}

    public UploadedImage(String originalFilename, String storedFilename, String filePath, 
                        Long fileSize, String contentType, String imageCategory, 
                        String uploadedBy, String altText, String description) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.imageCategory = imageCategory;
        this.uploadedBy = uploadedBy;
        this.altText = altText;
        this.description = description;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getImageCategory() {
        return imageCategory;
    }

    public void setImageCategory(String imageCategory) {
        this.imageCategory = imageCategory;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Utility methods
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isVideo() {
        return contentType != null && contentType.startsWith("video/");
    }

    public ImageCategory getCategoryEnum() {
        try {
            return ImageCategory.fromValue(imageCategory);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "UploadedImage{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", imageCategory='" + imageCategory + '\'' +
                ", fileSize=" + fileSize +
                ", isActive=" + isActive +
                '}';
    }
}
