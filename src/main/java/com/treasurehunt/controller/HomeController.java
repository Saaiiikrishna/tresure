package com.treasurehunt.controller;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.service.AppSettingsService;
import com.treasurehunt.service.TreasureHuntPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for home page and plan display
 * Handles main landing page and plan-related API endpoints
 */
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final TreasureHuntPlanService planService;
    private final AppSettingsService appSettingsService;

    @Autowired
    public HomeController(TreasureHuntPlanService planService, AppSettingsService appSettingsService) {
        this.planService = planService;
        this.appSettingsService = appSettingsService;
    }



    /**
     * Clear session and redirect to home
     */
    @GetMapping("/clear-session")
    public String clearSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // Clear cookies
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/";
    }

    /**
     * Display main landing page with available plans (ORIGINAL DESIGN)
     * @param model Thymeleaf model
     * @param response HTTP response for cache control
     * @return Template name
     */
    @GetMapping("/")
    public String home(Model model, HttpServletResponse response) {
        logger.debug("Displaying home page");

        // Prevent caching to ensure fresh data
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            // CRITICAL FIX: Use correct setting keys with underscores (not dots)
            List<String> requiredSettings = List.of(
                "hero_background_video_url",
                "hero_fallback_image_url",
                "about_section_image_url",
                "contact_background_image_url",
                "background_media_enabled",
                "hero_preview_video_url",
                "hero_blur_intensity"
            );

            Map<String, String> settings = appSettingsService.getMultipleSettings(requiredSettings);
            logger.debug("Bulk retrieved {} settings in single operation", settings.size());

            // Get data from services
            List<TreasureHuntPlan> availablePlans = planService.getAvailablePlans();
            TreasureHuntPlan featuredPlan = planService.getFeaturedPlan();

            // Extract settings from bulk result with correct keys
            String heroVideoUrl = settings.get("hero_background_video_url");
            String heroFallbackImageUrl = settings.get("hero_fallback_image_url");
            String aboutSectionImageUrl = settings.get("about_section_image_url");
            String contactBackgroundImageUrl = settings.get("contact_background_image_url");
            boolean backgroundMediaEnabled = Boolean.parseBoolean(settings.getOrDefault("background_media_enabled", "true"));
            String heroPreviewVideoUrl = settings.get("hero_preview_video_url");
            String heroBlurIntensity = settings.getOrDefault("hero_blur_intensity", "5");

            // Debug logging for image URLs
            logger.info("=== HOME PAGE IMAGE URLS ===");
            logger.info("Hero Video URL: {}", heroVideoUrl);
            logger.info("Hero Fallback Image URL: {}", heroFallbackImageUrl);
            logger.info("About Section Image URL: {}", aboutSectionImageUrl);
            logger.info("Contact Background Image URL: {}", contactBackgroundImageUrl);
            logger.info("Background Media Enabled: {}", backgroundMediaEnabled);
            logger.info("=== END IMAGE URLS ===");

            // Note: getFeaturedPlan() now always returns a plan (never null)
            // so no fallback logic is needed here

            model.addAttribute("plans", availablePlans);
            model.addAttribute("totalPlans", availablePlans.size());
            // Add cache-busting parameter only for non-YouTube URLs to force refresh
            String cacheBustedVideoUrl = heroVideoUrl;
            if (heroVideoUrl != null && !heroVideoUrl.isEmpty() &&
                !heroVideoUrl.contains("youtube.com") && !heroVideoUrl.contains("youtu.be")) {
                String separator = heroVideoUrl.contains("?") ? "&" : "?";
                cacheBustedVideoUrl = heroVideoUrl + separator + "t=" + System.currentTimeMillis();
            }
            model.addAttribute("heroVideoUrl", cacheBustedVideoUrl); // Background video
            model.addAttribute("heroPreviewVideoUrl", heroPreviewVideoUrl); // Preview video (from bulk settings)
            model.addAttribute("heroFallbackImageUrl", heroFallbackImageUrl);
            model.addAttribute("aboutSectionImageUrl", aboutSectionImageUrl);
            model.addAttribute("contactBackgroundImageUrl", contactBackgroundImageUrl);
            model.addAttribute("backgroundMediaEnabled", backgroundMediaEnabled);
            model.addAttribute("heroBlurIntensity", heroBlurIntensity); // From bulk settings
            model.addAttribute("featuredPlan", featuredPlan);

            // FIXED: Use original service methods for complex objects
            model.addAttribute("companyInfo", appSettingsService.getCompanyInfo());
            model.addAttribute("socialLinks", appSettingsService.getSocialMediaLinks());
            model.addAttribute("footerLinks", appSettingsService.getFooterLinks());
            model.addAttribute("contactInfo", appSettingsService.getContactInfo());

            logger.debug("Found {} available plans for home page", availablePlans.size());
            if (featuredPlan != null) {
                logger.debug("Featured plan: {}", featuredPlan.getName());
            }
            return "index";
            
        } catch (Exception e) {
            logger.error("Error loading home page", e);
            model.addAttribute("error", "Unable to load treasure hunt plans. Please try again later.");
            return "error";
        }
    }

    // Debug endpoints removed for production security

    // Debug JavaScript endpoint removed for production security

    /**
     * REST API endpoint to get all available plans
     * @return JSON response with available plans
     */
    @GetMapping("/api/plans")
    @ResponseBody
    public ResponseEntity<List<TreasureHuntPlan>> getAvailablePlans() {
        logger.debug("API request for available plans");
        
        try {
            List<TreasureHuntPlan> plans = planService.getAvailablePlans();
            logger.debug("Returning {} available plans via API", plans.size());
            return ResponseEntity.ok(plans);
            
        } catch (Exception e) {
            logger.error("Error fetching plans via API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get registration form fragment
     * @param planId Plan ID
     * @param model Thymeleaf model
     * @return HTML fragment for the registration form
     */
    @GetMapping("/register/form/{planId}")
    public String getRegistrationForm(@PathVariable Long planId, Model model) {
        logger.debug("Fetching registration form for plan ID: {}", planId);
        try {
            TreasureHuntPlan plan = planService.getPlanById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
            model.addAttribute("plan", plan);
            return "fragments/registration-form :: form";
        } catch (Exception e) {
            logger.error("Error fetching registration form for plan ID: {}", planId, e);
            // You can return an error fragment here if you have one
            return "fragments/registration-form :: error";
        }
    }

    /**
     * REST API endpoint to get a specific plan by ID
     * @param id Plan ID
     * @return JSON response with plan details
     */
    @GetMapping("/api/plans/{id}")
    @ResponseBody
    public ResponseEntity<TreasureHuntPlan> getPlanById(@PathVariable Long id) {
        logger.debug("API request for plan with ID: {}", id);
        
        try {
            Optional<TreasureHuntPlan> plan = planService.getAvailablePlanById(id);
            
            if (plan.isPresent()) {
                logger.debug("Found plan with ID: {}", id);
                return ResponseEntity.ok(plan.get());
            } else {
                logger.debug("Plan not found or not available with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching plan with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * REST API endpoint to get plans by difficulty level
     * @param difficulty Difficulty level
     * @return JSON response with filtered plans
     */
    @GetMapping("/api/plans/difficulty/{difficulty}")
    @ResponseBody
    public ResponseEntity<List<TreasureHuntPlan>> getPlansByDifficulty(
            @PathVariable String difficulty) {
        
        logger.debug("API request for plans with difficulty: {}", difficulty);
        
        try {
            TreasureHuntPlan.DifficultyLevel difficultyLevel = 
                    TreasureHuntPlan.DifficultyLevel.valueOf(difficulty.toUpperCase());
            
            List<TreasureHuntPlan> plans = planService.getPlansByDifficulty(difficultyLevel);
            logger.debug("Found {} plans with difficulty: {}", plans.size(), difficulty);
            
            return ResponseEntity.ok(plans);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid difficulty level requested: {}", difficulty);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error fetching plans by difficulty: {}", difficulty, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * REST API endpoint to search plans by name
     * @param name Name to search for
     * @return JSON response with matching plans
     */
    @GetMapping("/api/plans/search")
    @ResponseBody
    public ResponseEntity<List<TreasureHuntPlan>> searchPlans(@RequestParam String name) {
        logger.debug("API request to search plans by name: {}", name);
        
        try {
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<TreasureHuntPlan> plans = planService.searchPlansByName(name.trim());
            logger.debug("Found {} plans matching name: {}", plans.size(), name);
            
            return ResponseEntity.ok(plans);
            
        } catch (Exception e) {
            logger.error("Error searching plans by name: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * About page
     * @return Template name
     */
    @GetMapping("/about")
    public String about() {
        logger.debug("Displaying about page");
        return "about";
    }

    /**
     * Contact page
     * @return Template name
     */
    @GetMapping("/contact")
    public String contact() {
        logger.debug("Displaying contact page");
        return "contact";
    }

    /**
     * Privacy policy page
     * @return Template name
     */
    @GetMapping("/privacy")
    public String privacy() {
        logger.debug("Displaying privacy policy page");
        return "privacy";
    }

    /**
     * Terms of service page
     * @return Template name
     */
    @GetMapping("/terms")
    public String terms() {
        logger.debug("Displaying terms of service page");
        return "terms";
    }



    /**
     * Actuator health check endpoint (backup)
     * @return Health status
     */
    @GetMapping("/actuator/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actuatorHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Lightweight health check - verify we can access the database
            long planCount = planService.getActivePlanCount();
            logger.debug("Actuator health check passed - {} active plans", planCount);

            health.put("status", "UP");
            health.put("components", Map.of(
                "db", Map.of("status", "UP", "details", Map.of("activePlans", planCount)),
                "diskSpace", Map.of("status", "UP")
            ));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.warn("Actuator health check failed: {}", e.getMessage());

            health.put("status", "DOWN");
            health.put("components", Map.of(
                "db", Map.of("status", "DOWN", "details", Map.of("error", e.getMessage()))
            ));

            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Simple health endpoint for Azure health checks
     * @return Simple OK response
     */
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> simpleHealthCheck() {
        try {
            // Very lightweight health check - just verify service is running
            long planCount = planService.getActivePlanCount();
            logger.debug("Simple health check passed - {} plans", planCount);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.warn("Simple health check failed: {}", e.getMessage());
            return ResponseEntity.status(503).body("ERROR");
        }
    }
}
