package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive file validation service that provides security-focused file validation
 * Validates file types, sizes, content, and detects potential security threats
 */
@Service
public class SecureFileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(SecureFileValidationService.class);

    // File type definitions
    public enum FileType {
        PHOTO, DOCUMENT, IMAGE, VIDEO
    }

    // Maximum file sizes (in bytes)
    private static final Map<FileType, Long> MAX_FILE_SIZES = Map.of(
        FileType.PHOTO, 2L * 1024 * 1024,      // 2MB
        FileType.DOCUMENT, 5L * 1024 * 1024,   // 5MB
        FileType.IMAGE, 5L * 1024 * 1024,      // 5MB
        FileType.VIDEO, 50L * 1024 * 1024      // 50MB
    );

    // Allowed MIME types for each file type
    private static final Map<FileType, String[]> ALLOWED_MIME_TYPES = Map.of(
        FileType.PHOTO, new String[]{"image/jpeg", "image/jpg", "image/png"},
        FileType.DOCUMENT, new String[]{"application/pdf", "image/jpeg", "image/jpg"},
        FileType.IMAGE, new String[]{"image/jpeg", "image/png", "image/webp"},
        FileType.VIDEO, new String[]{"video/mp4", "video/webm", "video/ogg"}
    );

    // File signature validation (magic bytes)
    private static final Map<String, byte[]> FILE_SIGNATURES = new HashMap<>();
    static {
        FILE_SIGNATURES.put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        FILE_SIGNATURES.put("image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        FILE_SIGNATURES.put("application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        FILE_SIGNATURES.put("video/mp4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70});
    }

    // Dangerous file patterns to detect
    private static final Pattern[] MALICIOUS_PATTERNS = {
        Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL),
        Pattern.compile("(?i)javascript:", Pattern.DOTALL),
        Pattern.compile("(?i)vbscript:", Pattern.DOTALL),
        Pattern.compile("(?i)onload\\s*=", Pattern.DOTALL),
        Pattern.compile("(?i)onerror\\s*=", Pattern.DOTALL)
    };

    // Safe filename pattern
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    /**
     * Comprehensive file validation
     * @param file File to validate
     * @param expectedType Expected file type
     * @throws SecurityException if file fails validation
     */
    public void validateFile(MultipartFile file, FileType expectedType) throws SecurityException {
        logger.debug("Validating file: {} for type: {}", file.getOriginalFilename(), expectedType);

        // 1. Basic null and empty checks
        if (file == null || file.isEmpty()) {
            throw new SecurityException("File is null or empty");
        }

        // 2. Validate filename
        validateFilename(file.getOriginalFilename());

        // 3. Validate file size
        validateFileSize(file, expectedType);

        // 4. Validate MIME type
        validateMimeType(file, expectedType);

        // 5. Validate file signature (magic bytes)
        validateFileSignature(file, expectedType);

        // 6. Scan for malicious content
        scanForMaliciousContent(file);

        logger.debug("File validation passed for: {}", file.getOriginalFilename());
    }

    /**
     * Validate filename for security
     */
    private void validateFilename(String filename) throws SecurityException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new SecurityException("Filename is null or empty");
        }

        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("Filename contains path traversal characters");
        }

        // Check for dangerous extensions
        String lowerFilename = filename.toLowerCase();
        String[] dangerousExtensions = {".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar", ".php", ".asp", ".jsp"};
        for (String ext : dangerousExtensions) {
            if (lowerFilename.endsWith(ext)) {
                throw new SecurityException("Dangerous file extension detected: " + ext);
            }
        }

        // Check for double extensions (e.g., file.pdf.exe)
        if (filename.split("\\.").length > 2) {
            logger.warn("File with multiple extensions detected: {}", filename);
        }
    }

    /**
     * Validate file size
     */
    private void validateFileSize(MultipartFile file, FileType expectedType) throws SecurityException {
        long maxSize = MAX_FILE_SIZES.get(expectedType);
        if (file.getSize() > maxSize) {
            throw new SecurityException(String.format(
                "File size (%d bytes) exceeds maximum allowed size (%d bytes) for type %s",
                file.getSize(), maxSize, expectedType
            ));
        }

        // Check for zero-byte files
        if (file.getSize() == 0) {
            throw new SecurityException("File is empty (0 bytes)");
        }
    }

    /**
     * Validate MIME type
     */
    private void validateMimeType(MultipartFile file, FileType expectedType) throws SecurityException {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new SecurityException("File content type is null");
        }

        String[] allowedTypes = ALLOWED_MIME_TYPES.get(expectedType);
        boolean isAllowed = Arrays.asList(allowedTypes).contains(contentType.toLowerCase());
        
        if (!isAllowed) {
            throw new SecurityException(String.format(
                "File type '%s' is not allowed for %s. Allowed types: %s",
                contentType, expectedType, Arrays.toString(allowedTypes)
            ));
        }
    }

    /**
     * Validate file signature (magic bytes)
     */
    private void validateFileSignature(MultipartFile file, FileType expectedType) throws SecurityException {
        String contentType = file.getContentType();
        if (contentType == null) {
            return; // Already validated in MIME type check
        }

        byte[] expectedSignature = FILE_SIGNATURES.get(contentType.toLowerCase());
        if (expectedSignature == null) {
            logger.debug("No signature validation available for content type: {}", contentType);
            return;
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] fileHeader = new byte[expectedSignature.length];
            int bytesRead = inputStream.read(fileHeader);
            
            if (bytesRead < expectedSignature.length) {
                throw new SecurityException("File is too small to contain valid signature");
            }

            if (!Arrays.equals(fileHeader, expectedSignature)) {
                throw new SecurityException(String.format(
                    "File signature does not match expected signature for content type: %s", contentType
                ));
            }

        } catch (IOException e) {
            throw new SecurityException("Error reading file for signature validation: " + e.getMessage());
        }
    }

    /**
     * Scan file content for malicious patterns
     */
    private void scanForMaliciousContent(MultipartFile file) throws SecurityException {
        try (InputStream inputStream = file.getInputStream()) {
            // Read first 8KB for content scanning (enough to detect most threats)
            byte[] buffer = new byte[8192];
            int bytesRead = inputStream.read(buffer);
            
            if (bytesRead > 0) {
                String content = new String(buffer, 0, bytesRead);
                
                // Check for malicious patterns
                for (Pattern pattern : MALICIOUS_PATTERNS) {
                    if (pattern.matcher(content).find()) {
                        throw new SecurityException("Malicious content pattern detected in file");
                    }
                }

                // Check for embedded executables
                if (content.contains("MZ") && content.contains("PE")) {
                    throw new SecurityException("Embedded executable detected in file");
                }

                // Check for suspicious URLs
                if (content.toLowerCase().contains("http://") || content.toLowerCase().contains("https://")) {
                    logger.warn("File contains URLs - potential security risk: {}", file.getOriginalFilename());
                }
            }

        } catch (IOException e) {
            throw new SecurityException("Error scanning file content: " + e.getMessage());
        }
    }

    /**
     * Sanitize filename for safe storage
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown_file";
        }

        // Remove dangerous characters
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Ensure it doesn't start with a dot
        if (sanitized.startsWith(".")) {
            sanitized = "file" + sanitized;
        }

        // Limit length
        if (sanitized.length() > 100) {
            String extension = getFileExtension(sanitized);
            sanitized = sanitized.substring(0, 100 - extension.length()) + extension;
        }

        return sanitized;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Generate secure filename for storage
     */
    public String generateSecureFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return "file_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(originalFilename.hashCode()) + extension;
    }
}
