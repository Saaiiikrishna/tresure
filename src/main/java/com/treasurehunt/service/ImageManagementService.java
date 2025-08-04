package com.treasurehunt.service;

import com.treasurehunt.entity.UploadedImage;
import com.treasurehunt.repository.UploadedImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing uploaded images
 * Handles file upload, storage, and database operations
 */
@Service
@Transactional
public class ImageManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ImageManagementService.class);

    private final UploadedImageRepository imageRepository;
    private final AppSettingsService appSettingsService;

    @Value("${app.file-storage.upload-dir:uploads/documents}")
    private String uploadDirectory;

    @Value("${app.file-storage.max-image-size:5242880}") // 5MB default for images
    private long maxImageSize;

    @Value("${app.file-storage.max-video-size:52428800}") // 50MB default for videos
    private long maxVideoSize;

    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/webm", "video/ogg"};

    @Autowired
    public ImageManagementService(UploadedImageRepository imageRepository, AppSettingsService appSettingsService) {
        this.imageRepository = imageRepository;
        this.appSettingsService = appSettingsService;
    }

    /**
     * Upload and save an image file
     * @param file Multipart file to upload
     * @param category Image category
     * @param altText Alt text for the image
     * @param description Image description
     * @param uploadedBy Username of the uploader
     * @return Saved UploadedImage entity
     * @throws IOException If file upload fails
     */
    public UploadedImage uploadImage(MultipartFile file, String category, String altText, 
                                   String description, String uploadedBy) throws IOException {
        
        logger.info("Starting image upload for category: {}, file: {}", category, file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = generateUniqueFilename(category, fileExtension);
        
        // Save file to disk
        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("File saved to: {}", filePath);

        // Create database record
        UploadedImage uploadedImage = new UploadedImage(
            originalFilename,
            storedFilename,
            filePath.toString(),
            file.getSize(),
            file.getContentType(),
            category,
            uploadedBy,
            altText,
            description
        );

        // Deactivate previous images in the same category (only one active per category)
        deactivatePreviousImages(category);

        // Save to database
        UploadedImage savedImage = imageRepository.save(uploadedImage);
        
        // Update app settings with new image URL
        updateAppSettingForCategory(category, getImageUrl(savedImage));

        logger.info("Image uploaded successfully with ID: {}", savedImage.getId());
        return savedImage;
    }

    /**
     * Save image from URL (supports regular images and YouTube videos)
     * @param imageUrl URL of the image or YouTube video
     * @param category Image category
     * @param altText Alt text for the image
     * @param description Image description
     * @param uploadedBy Username of the uploader
     * @return Saved UploadedImage entity
     */
    public UploadedImage saveImageFromUrl(String imageUrl, String category, String altText,
                                        String description, String uploadedBy) {

        logger.info("Saving image from URL for category: {}, URL: {}", category, imageUrl);

        // Process YouTube URLs to extract video ID and create proper embed URL
        String processedUrl = processVideoUrl(imageUrl);

        // Extract filename from URL or generate one
        String filename = extractFilenameFromUrl(processedUrl);

        // Create database record
        UploadedImage uploadedImage = new UploadedImage(
            filename,
            filename,
            processedUrl,
            0L, // Size unknown for external URLs
            guessContentTypeFromUrl(processedUrl),
            category,
            uploadedBy,
            altText,
            description
        );

        // Deactivate previous images in the same category
        deactivatePreviousImages(category);

        // Save to database
        UploadedImage savedImage = imageRepository.save(uploadedImage);

        // Update app settings with new image URL
        updateAppSettingForCategory(category, processedUrl);

        logger.info("Image from URL saved successfully with ID: {}", savedImage.getId());
        return savedImage;
    }

    /**
     * Get active image by category
     * @param category Image category
     * @return Optional UploadedImage
     */
    @Transactional(readOnly = true)
    public Optional<UploadedImage> getActiveImageByCategory(String category) {
        return imageRepository.findLatestByCategory(category);
    }

    /**
     * Get all active images
     * @return List of active images
     */
    @Transactional(readOnly = true)
    public List<UploadedImage> getAllActiveImages() {
        return imageRepository.findByIsActiveTrueOrderByUploadDateDesc();
    }

    /**
     * Get images by category
     * @param category Image category
     * @return List of images in the category
     */
    @Transactional(readOnly = true)
    public List<UploadedImage> getImagesByCategory(String category) {
        return imageRepository.findByImageCategoryOrderByUploadDateDesc(category);
    }

    /**
     * Delete image by ID
     * @param id Image ID
     * @return true if deleted successfully
     */
    public boolean deleteImage(Long id) {
        try {
            Optional<UploadedImage> imageOpt = imageRepository.findById(id);
            if (imageOpt.isPresent()) {
                UploadedImage image = imageOpt.get();
                
                // Delete physical file if it's a local file
                if (!image.getFilePath().startsWith("http")) {
                    Path filePath = Paths.get(image.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        logger.info("Deleted physical file: {}", filePath);
                    }
                }
                
                // Delete database record
                imageRepository.delete(image);
                logger.info("Deleted image with ID: {}", id);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error deleting image with ID: {}", id, e);
        }
        return false;
    }

    /**
     * Get image URL for display
     * @param image UploadedImage entity
     * @return URL string
     */
    public String getImageUrl(UploadedImage image) {
        if (image.getFilePath().startsWith("http")) {
            return image.getFilePath();
        } else {
            // For local files, return a URL that can be served by the application
            // Using documents path since we're storing everything in the same directory
            return "/uploads/documents/" + image.getStoredFilename();
        }
    }

    // Private helper methods

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String contentType = file.getContentType();
        boolean isImage = false;
        boolean isVideo = false;

        // Check if it's an image
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                isImage = true;
                break;
            }
        }

        // Check if it's a video
        if (!isImage) {
            for (String allowedType : ALLOWED_VIDEO_TYPES) {
                if (allowedType.equals(contentType)) {
                    isVideo = true;
                    break;
                }
            }
        }

        // Validate file type
        if (!isImage && !isVideo) {
            throw new IOException("File type not allowed: " + contentType + ". Allowed types: Images (JPEG, PNG, WebP) and Videos (MP4, WebM, OGG)");
        }

        // Validate file size based on type
        if (isImage && file.getSize() > maxImageSize) {
            throw new IOException("Image size exceeds maximum allowed size of " + (maxImageSize / 1024 / 1024) + "MB");
        }

        if (isVideo && file.getSize() > maxVideoSize) {
            throw new IOException("Video size exceeds maximum allowed size of " + (maxVideoSize / 1024 / 1024) + "MB");
        }

        logger.debug("File validation passed: {} ({}), size: {} bytes",
                    file.getOriginalFilename(), contentType, file.getSize());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateUniqueFilename(String category, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return category + "_" + timestamp + "_" + uuid + extension;
    }

    private void deactivatePreviousImages(String category) {
        List<UploadedImage> previousImages = imageRepository.findByImageCategoryAndIsActiveTrueOrderByUploadDateDesc(category);
        for (UploadedImage image : previousImages) {
            image.setIsActive(false);
            imageRepository.save(image);
        }
        logger.info("Deactivated {} previous images in category: {}", previousImages.size(), category);
    }

    private void updateAppSettingForCategory(String category, String imageUrl) {
        String settingKey = getSettingKeyForCategory(category);
        logger.info("=== UPDATING APP SETTING ===");
        logger.info("Category: {}", category);
        logger.info("Setting Key: {}", settingKey);
        logger.info("Image URL: {}", imageUrl);

        if (settingKey != null) {
            String description = getDescriptionForCategory(category);
            appSettingsService.updateSetting(settingKey, imageUrl, description);
            logger.info("Successfully updated app setting {} with new image URL: {}", settingKey, imageUrl);

            // Verify the setting was actually saved
            String retrievedValue = appSettingsService.getSettingValue(settingKey, "NOT_FOUND");
            logger.info("Verification - Retrieved value for {}: {}", settingKey, retrievedValue);
        } else {
            logger.warn("No setting key found for category: {}", category);
        }
        logger.info("=== APP SETTING UPDATE COMPLETE ===");
    }

    private String getSettingKeyForCategory(String category) {
        switch (category) {
            case "hero_video":
                return "hero_background_video_url";
            case "hero_fallback":
                return "hero_background_fallback_image";
            case "about_section":
                return "about_section_image";
            case "contact_background":
                return "contact_background_image";
            default:
                return null;
        }
    }

    private String getDescriptionForCategory(String category) {
        switch (category) {
            case "hero_video":
                return "Hero section background video URL";
            case "hero_fallback":
                return "Hero section fallback image for video";
            case "about_section":
                return "Why Choose Our Adventure section image";
            case "contact_background":
                return "Contact section background image";
            default:
                return "Image setting";
        }
    }

    private String extractFilenameFromUrl(String url) {
        try {
            String[] parts = url.split("\\/");
            String lastPart = parts[parts.length - 1];
            // Remove query parameters
            if (lastPart.contains("?")) {
                lastPart = lastPart.substring(0, lastPart.indexOf("?"));
            }
            return lastPart.isEmpty() ? "external_image.jpg" : lastPart;
        } catch (Exception e) {
            return "external_image.jpg";
        }
    }

    private String guessContentTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            return "video/youtube";
        } else if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.contains(".png")) {
            return "image/png";
        } else if (lowerUrl.contains(".webp")) {
            return "image/webp";
        } else if (lowerUrl.contains(".mp4")) {
            return "video/mp4";
        } else if (lowerUrl.contains(".webm")) {
            return "video/webm";
        } else {
            return "image/jpeg"; // Default
        }
    }

    /**
     * Process video URLs, especially YouTube URLs
     * Converts regular YouTube URLs to the format expected by the application
     * @param url Original URL
     * @return Processed URL
     */
    private String processVideoUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        String trimmedUrl = url.trim();

        // Handle YouTube URLs
        if (isYouTubeUrl(trimmedUrl)) {
            String videoId = extractYouTubeVideoId(trimmedUrl);
            if (videoId != null) {
                // For hero video background, we can use the direct MP4 URL or keep the original
                // The frontend will handle the conversion to embed format as needed
                logger.info("Processed YouTube URL - Video ID: {}", videoId);
                return trimmedUrl; // Keep original URL for flexibility
            }
        }

        return trimmedUrl;
    }

    /**
     * Check if URL is a YouTube URL
     * @param url URL to check
     * @return true if it's a YouTube URL
     */
    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    /**
     * Extract video ID from YouTube URL
     * Supports both youtube.com/watch?v=ID and youtu.be/ID formats
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    private String extractYouTubeVideoId(String url) {
        try {
            if (url.contains("youtube.com/watch?v=")) {
                String[] parts = url.split("v\\=");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    // Remove additional parameters
                    if (videoId.contains("&")) {
                        videoId = videoId.substring(0, videoId.indexOf("&"));
                    }
                    return videoId;
                }
            } else if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu\\.be\\/");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    // Remove additional parameters
                    if (videoId.contains("?")) {
                        videoId = videoId.substring(0, videoId.indexOf("?"));
                    }
                    return videoId;
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting YouTube video ID from URL: {}", url, e);
        }
        return null;
    }
}
