package com.treasurehunt.controller;

import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UploadedImage;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.UploadedDocumentRepository;
import com.treasurehunt.repository.UploadedImageRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Secure file controller that handles file serving with proper authorization and security checks
 * Replaces direct file serving to prevent path traversal and unauthorized access
 */
@Controller
@RequestMapping("/secure/files")
public class SecureFileController {

    private static final Logger logger = LoggerFactory.getLogger(SecureFileController.class);

    // Pattern to validate safe filenames (alphanumeric, dots, hyphens, underscores only)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    // Maximum file size to serve (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Value("${app.file-storage.upload-dir}")
    private String documentUploadDir;

    @Value("${app.file-storage.image-dir}")
    private String imageUploadDir;

    private final UploadedDocumentRepository documentRepository;
    private final UploadedImageRepository imageRepository;
    private final UserRegistrationRepository registrationRepository;

    @Autowired
    public SecureFileController(UploadedDocumentRepository documentRepository,
                               UploadedImageRepository imageRepository,
                               UserRegistrationRepository registrationRepository) {
        this.documentRepository = documentRepository;
        this.imageRepository = imageRepository;
        this.registrationRepository = registrationRepository;
    }

    /**
     * Serve registration documents with authorization checks
     * Only admins can access registration documents
     */
    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> serveDocument(@PathVariable Long documentId, Authentication auth) {
        logger.info("Admin {} requesting document ID: {}", auth.getName(), documentId);

        try {
            // Find document in database
            Optional<UploadedDocument> documentOpt = documentRepository.findById(documentId);
            if (documentOpt.isEmpty()) {
                logger.warn("Document not found: {}", documentId);
                return ResponseEntity.notFound().build();
            }

            UploadedDocument document = documentOpt.get();
            
            // Validate file path and existence
            Path filePath = Paths.get(document.getFilePath());
            if (!isSecureFilePath(filePath) || !Files.exists(filePath)) {
                logger.error("Invalid or missing file path for document {}: {}", documentId, document.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // Check file size
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                logger.error("File too large for document {}: {} bytes", documentId, fileSize);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            // Serve file with security headers
            Resource resource = new FileSystemResource(filePath);
            String contentType = determineContentType(document.getOriginalFilename());
            
            logger.info("Serving document {} to admin {}", documentId, auth.getName());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + sanitizeFilename(document.getOriginalFilename()) + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "DENY")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            logger.error("Error serving document {}: {}", documentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Unexpected error serving document {}: {}", documentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve public images (hero images, etc.) with basic security checks
     * These are publicly accessible but still need security validation
     */
    @GetMapping("/images/{imageId}")
    public ResponseEntity<Resource> serveImage(@PathVariable Long imageId) {
        logger.debug("Serving public image ID: {}", imageId);

        try {
            // Find image in database
            Optional<UploadedImage> imageOpt = imageRepository.findById(imageId);
            if (imageOpt.isEmpty()) {
                logger.warn("Image not found: {}", imageId);
                return ResponseEntity.notFound().build();
            }

            UploadedImage image = imageOpt.get();
            
            // Only serve active images
            if (!image.getIsActive()) {
                logger.warn("Attempt to access inactive image: {}", imageId);
                return ResponseEntity.notFound().build();
            }

            // Validate file path and existence
            Path filePath = Paths.get(image.getFilePath());
            if (!isSecureFilePath(filePath) || !Files.exists(filePath)) {
                logger.error("Invalid or missing file path for image {}: {}", imageId, image.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // Check file size
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                logger.error("File too large for image {}: {} bytes", imageId, fileSize);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            // Serve image with caching headers for performance
            Resource resource = new FileSystemResource(filePath);
            String contentType = determineContentType(image.getOriginalFilename());
            
            return ResponseEntity.ok()
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "public, max-age=3600") // Cache for 1 hour
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            logger.error("Error serving image {}: {}", imageId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Unexpected error serving image {}: {}", imageId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate that file path is secure and within allowed directories
     */
    private boolean isSecureFilePath(Path filePath) {
        try {
            // Normalize path to resolve any .. or . components
            Path normalizedPath = filePath.normalize().toAbsolutePath();
            
            // Check if path is within allowed directories
            Path documentDir = Paths.get(documentUploadDir).normalize().toAbsolutePath();
            Path imageDir = Paths.get(imageUploadDir).normalize().toAbsolutePath();
            
            return normalizedPath.startsWith(documentDir) || normalizedPath.startsWith(imageDir);
            
        } catch (Exception e) {
            logger.error("Error validating file path: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sanitize filename to prevent injection attacks
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        
        // Remove path separators and other dangerous characters
        String sanitized = filename.replaceAll("[/\\\\:*?\"<>|]", "_");
        
        // Ensure filename matches safe pattern
        if (!SAFE_FILENAME_PATTERN.matcher(sanitized).matches()) {
            // If filename contains unsafe characters, generate a safe name
            String extension = getFileExtension(sanitized);
            return "file_" + System.currentTimeMillis() + extension;
        }
        
        return sanitized;
    }

    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case ".pdf":
                return MediaType.APPLICATION_PDF_VALUE;
            case ".jpg":
            case ".jpeg":
                return MediaType.IMAGE_JPEG_VALUE;
            case ".png":
                return MediaType.IMAGE_PNG_VALUE;
            case ".gif":
                return MediaType.IMAGE_GIF_VALUE;
            case ".webp":
                return "image/webp";
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
