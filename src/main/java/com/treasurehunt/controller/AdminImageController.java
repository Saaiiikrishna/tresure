package com.treasurehunt.controller;

import com.treasurehunt.entity.UploadedImage;
import com.treasurehunt.service.AppSettingsService;
import com.treasurehunt.service.FileCleanupService;
import com.treasurehunt.service.ImageManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for admin image management functionality
 * Handles image uploads, URL management, and storage monitoring
 */
@Controller
@RequestMapping("/admin/images")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageController {

    private static final Logger logger = LoggerFactory.getLogger(AdminImageController.class);

    private final ImageManagementService imageManagementService;
    private final FileCleanupService fileCleanupService;
    private final AppSettingsService appSettingsService;

    @Autowired
    public AdminImageController(ImageManagementService imageManagementService,
                               FileCleanupService fileCleanupService,
                               AppSettingsService appSettingsService) {
        this.imageManagementService = imageManagementService;
        this.fileCleanupService = fileCleanupService;
        this.appSettingsService = appSettingsService;
    }

    /**
     * Display image management page
     */
    @GetMapping
    public String imageManagement(Model model) {
        logger.info("Displaying image management page");

        try {
            // Get current images for each category
            Map<String, UploadedImage> currentImages = new HashMap<>();
            
            for (UploadedImage.ImageCategory category : UploadedImage.ImageCategory.values()) {
                Optional<UploadedImage> image = imageManagementService.getActiveImageByCategory(category.getValue());
                if (image.isPresent()) {
                    currentImages.put(category.getValue(), image.get());
                }
            }

            // Get storage statistics
            FileCleanupService.StorageStats storageStats = fileCleanupService.getStorageStats();

            // Get recent cleanup logs
            List<FileCleanupService.CleanupLog> recentLogs = fileCleanupService.getCleanupLogs()
                .stream()
                .limit(5)
                .toList();

            // Get background media toggle setting
            boolean backgroundMediaEnabled = appSettingsService.getBackgroundMediaEnabled();

            model.addAttribute("currentImages", currentImages);
            model.addAttribute("imageCategories", UploadedImage.ImageCategory.values());
            model.addAttribute("storageStats", storageStats);
            model.addAttribute("recentCleanupLogs", recentLogs);
            model.addAttribute("backgroundMediaEnabled", backgroundMediaEnabled);

        } catch (Exception e) {
            logger.error("Error loading image management page", e);
            model.addAttribute("error", "Error loading image data: " + e.getMessage());
        }

        return "admin/image-management";
    }

    /**
     * Upload image file
     */
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {

        logger.info("Uploading image for category: {}, file: {}, size: {}",
                   category, file.getOriginalFilename(), file.getSize());

        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user is authenticated
            if (principal == null) {
                logger.warn("Upload attempt without authentication");
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "No file selected");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate category
            if (!isValidCategory(category)) {
                response.put("success", false);
                response.put("message", "Invalid image category: " + category);
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Authenticated user: {}, proceeding with upload", principal.getName());

            // Upload image
            UploadedImage uploadedImage = imageManagementService.uploadImage(
                file, category, altText, description, principal.getName());

            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("imageId", uploadedImage.getId());
            response.put("imageUrl", imageManagementService.getImageUrl(uploadedImage));

            logger.info("Image uploaded successfully with ID: {}", uploadedImage.getId());

        } catch (Exception e) {
            logger.error("Error uploading image", e);
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Save image from URL
     */
    @PostMapping("/url")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveImageFromUrl(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam("category") String category,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {

        logger.info("Saving image from URL for category: {}, URL: {}", category, imageUrl);

        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user is authenticated
            if (principal == null) {
                logger.warn("URL save attempt without authentication");
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            // Validate category
            if (!isValidCategory(category)) {
                response.put("success", false);
                response.put("message", "Invalid image category: " + category);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate URL
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Image URL is required");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Authenticated user: {}, proceeding with URL save", principal.getName());

            // Save image from URL
            UploadedImage uploadedImage = imageManagementService.saveImageFromUrl(
                imageUrl.trim(), category, altText, description, principal.getName());

            response.put("success", true);
            response.put("message", "Image saved successfully from URL");
            response.put("imageId", uploadedImage.getId());
            response.put("imageUrl", imageUrl.trim());

            logger.info("Image saved from URL successfully with ID: {}", uploadedImage.getId());

        } catch (Exception e) {
            logger.error("Error saving image from URL", e);
            response.put("success", false);
            response.put("message", "Save failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete image
     */
    @DeleteMapping("/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long imageId) {
        logger.info("Deleting image with ID: {}", imageId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = imageManagementService.deleteImage(imageId);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Image deleted successfully");
                logger.info("Image deleted successfully: {}", imageId);
            } else {
                response.put("success", false);
                response.put("message", "Image not found or could not be deleted");
                logger.warn("Failed to delete image: {}", imageId);
            }

        } catch (Exception e) {
            logger.error("Error deleting image", e);
            response.put("success", false);
            response.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get images by category
     */
    @GetMapping("/category/{category}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImagesByCategory(@PathVariable String category) {
        logger.info("Getting images for category: {}", category);

        Map<String, Object> response = new HashMap<>();

        try {
            if (!isValidCategory(category)) {
                response.put("success", false);
                response.put("message", "Invalid image category: " + category);
                return ResponseEntity.badRequest().body(response);
            }

            List<UploadedImage> images = imageManagementService.getImagesByCategory(category);
            
            response.put("success", true);
            response.put("images", images);
            response.put("count", images.size());

        } catch (Exception e) {
            logger.error("Error getting images for category: {}", category, e);
            response.put("success", false);
            response.put("message", "Error retrieving images: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get storage statistics
     */
    @GetMapping("/storage-stats")
    @ResponseBody
    public ResponseEntity<FileCleanupService.StorageStats> getStorageStats() {
        logger.info("Getting storage statistics");

        try {
            FileCleanupService.StorageStats stats = fileCleanupService.getStorageStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting storage statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Toggle background media setting
     */
    @PostMapping("/toggle-background-media")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleBackgroundMedia(@RequestBody Map<String, Boolean> request) {
        logger.info("Toggling background media setting");

        Map<String, Object> response = new HashMap<>();

        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                response.put("success", false);
                response.put("message", "Missing 'enabled' parameter");
                return ResponseEntity.badRequest().body(response);
            }

            // Update the setting
            appSettingsService.setBackgroundMediaEnabled(enabled);

            response.put("success", true);
            response.put("enabled", enabled);
            response.put("message", "Background media " + (enabled ? "enabled" : "disabled") + " successfully");

            logger.info("Background media setting updated to: {}", enabled);

        } catch (Exception e) {
            logger.error("Error toggling background media setting", e);
            response.put("success", false);
            response.put("message", "Error updating setting: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger manual cleanup
     */
    @PostMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        logger.info("Triggering manual cleanup");

        Map<String, Object> response = new HashMap<>();

        try {
            // Run cleanup operations
            FileCleanupService.CleanupResult documentResult = fileCleanupService.cleanupOldRegistrationDocuments();
            FileCleanupService.CleanupResult orphanedResult = fileCleanupService.cleanupOrphanedFiles();
            FileCleanupService.CleanupResult imageResult = fileCleanupService.cleanupInactiveImages();

            int totalFilesDeleted = documentResult.getFilesDeleted() + 
                                  orphanedResult.getFilesDeleted() + 
                                  imageResult.getFilesDeleted();
            
            long totalSpaceFreed = documentResult.getSpaceFreed() + 
                                 orphanedResult.getSpaceFreed() + 
                                 imageResult.getSpaceFreed();

            response.put("success", true);
            response.put("message", "Cleanup completed successfully");
            response.put("filesDeleted", totalFilesDeleted);
            response.put("spaceFreedMB", totalSpaceFreed / (1024.0 * 1024.0));
            response.put("documentFilesDeleted", documentResult.getFilesDeleted());
            response.put("orphanedFilesDeleted", orphanedResult.getFilesDeleted());
            response.put("imageFilesDeleted", imageResult.getFilesDeleted());

            logger.info("Manual cleanup completed. Files deleted: {}, Space freed: {} MB", 
                       totalFilesDeleted, totalSpaceFreed / (1024.0 * 1024.0));

        } catch (Exception e) {
            logger.error("Error during manual cleanup", e);
            response.put("success", false);
            response.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get cleanup logs
     */
    @GetMapping("/cleanup-logs")
    @ResponseBody
    public ResponseEntity<List<FileCleanupService.CleanupLog>> getCleanupLogs() {
        logger.info("Getting cleanup logs");

        try {
            List<FileCleanupService.CleanupLog> logs = fileCleanupService.getCleanupLogs();
            return ResponseEntity.ok(logs);

        } catch (Exception e) {
            logger.error("Error getting cleanup logs", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Validate image category
     */
    private boolean isValidCategory(String category) {
        try {
            UploadedImage.ImageCategory.fromValue(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Debug endpoint removed for production security
}
