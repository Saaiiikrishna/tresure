package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for processing YouTube URLs and converting them to proper embed format
 */
@Service
public class YouTubeUrlProcessor {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeUrlProcessor.class);

    /**
     * Process YouTube URL to embed format
     * @param url Original YouTube URL
     * @param isBackground Whether this is for background video (different parameters)
     * @return Processed embed URL
     */
    public String processToEmbedUrl(String url, boolean isBackground) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        String trimmedUrl = url.trim();
        
        // If already an embed URL, just add parameters
        if (trimmedUrl.contains("youtube.com/embed/")) {
            return addYouTubeParameters(trimmedUrl, isBackground);
        }

        // Extract video ID from various YouTube URL formats
        String videoId = extractVideoId(trimmedUrl);
        if (videoId != null) {
            String embedUrl = "https://www.youtube.com/embed/" + videoId;
            return addYouTubeParameters(embedUrl, isBackground);
        }

        logger.warn("Could not process YouTube URL: {}", trimmedUrl);
        return trimmedUrl;
    }

    /**
     * Extract video ID from YouTube URL
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    private String extractVideoId(String url) {
        try {
            // Handle youtube.com/watch?v=VIDEO_ID format
            if (url.contains("youtube.com/watch?v=")) {
                String[] parts = url.split("v\\=");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    // Remove additional parameters
                    if (videoId.contains("&")) {
                        videoId = videoId.substring(0, videoId.indexOf("&"));
                    }
                    if (videoId.contains("#")) {
                        videoId = videoId.substring(0, videoId.indexOf("#"));
                    }
                    return videoId;
                }
            }
            
            // Handle youtu.be/VIDEO_ID format
            if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu\\.be\\/");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    // Remove additional parameters
                    if (videoId.contains("?")) {
                        videoId = videoId.substring(0, videoId.indexOf("?"));
                    }
                    if (videoId.contains("#")) {
                        videoId = videoId.substring(0, videoId.indexOf("#"));
                    }
                    return videoId;
                }
            }

            // Handle youtube.com/embed/VIDEO_ID format
            if (url.contains("youtube.com/embed/")) {
                String[] parts = url.split("youtube\\.com\\/embed\\/");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    // Remove additional parameters
                    if (videoId.contains("?")) {
                        videoId = videoId.substring(0, videoId.indexOf("?"));
                    }
                    if (videoId.contains("#")) {
                        videoId = videoId.substring(0, videoId.indexOf("#"));
                    }
                    return videoId;
                }
            }

        } catch (Exception e) {
            logger.error("Error extracting video ID from URL: {}", url, e);
        }

        return null;
    }

    /**
     * Add appropriate parameters to YouTube embed URL
     * @param embedUrl Base embed URL
     * @param isBackground Whether this is for background video
     * @return URL with parameters
     */
    private String addYouTubeParameters(String embedUrl, boolean isBackground) {
        StringBuilder urlBuilder = new StringBuilder(embedUrl);
        
        // Remove existing parameters to avoid conflicts
        if (embedUrl.contains("?")) {
            urlBuilder = new StringBuilder(embedUrl.substring(0, embedUrl.indexOf("?")));
        }

        if (isBackground) {
            // Parameters for background video (autoplay, muted, no controls)
            urlBuilder.append("?autoplay=1")
                     .append("&mute=1")
                     .append("&loop=1")
                     .append("&controls=0")
                     .append("&showinfo=0")
                     .append("&rel=0")
                     .append("&iv_load_policy=3")
                     .append("&modestbranding=1")
                     .append("&playsinline=1")
                     .append("&enablejsapi=0")
                     .append("&origin=").append(getCurrentDomain());
        } else {
            // Parameters for preview video (user can interact)
            urlBuilder.append("?rel=0")
                     .append("&modestbranding=1")
                     .append("&showinfo=0")
                     .append("&iv_load_policy=3");
        }

        return urlBuilder.toString();
    }

    /**
     * Get current domain for origin parameter
     * @return Current domain
     */
    private String getCurrentDomain() {
        // In production, this should be the actual domain
        // For now, using localhost for development
        return "http://localhost:8080";
    }

    /**
     * Check if URL is a YouTube URL
     * @param url URL to check
     * @return true if it's a YouTube URL
     */
    public boolean isYouTubeUrl(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || url.contains("youtu.be");
    }
}
