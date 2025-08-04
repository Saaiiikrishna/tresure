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

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
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
     * Display main landing page with available plans
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
            List<TreasureHuntPlan> availablePlans = planService.getAvailablePlans();
            String heroVideoUrl = appSettingsService.getHeroVideoUrl();
            String heroFallbackImageUrl = appSettingsService.getHeroFallbackImageUrl();
            String aboutSectionImageUrl = appSettingsService.getAboutSectionImageUrl();
            String contactBackgroundImageUrl = appSettingsService.getContactBackgroundImageUrl();
            boolean backgroundMediaEnabled = appSettingsService.getBackgroundMediaEnabled();
            TreasureHuntPlan featuredPlan = planService.getFeaturedPlan();

            // Debug logging for image URLs
            logger.info("=== HOME PAGE IMAGE URLS ===");
            logger.info("Hero Video URL: {}", heroVideoUrl);
            logger.info("Hero Fallback Image URL: {}", heroFallbackImageUrl);
            logger.info("About Section Image URL: {}", aboutSectionImageUrl);
            logger.info("Contact Background Image URL: {}", contactBackgroundImageUrl);
            logger.info("Background Media Enabled: {}", backgroundMediaEnabled);
            logger.info("=== END IMAGE URLS ===");

            // If no featured plan, use the first available plan as fallback
            if (featuredPlan == null && !availablePlans.isEmpty()) {
                featuredPlan = availablePlans.get(0);
            }

            model.addAttribute("plans", availablePlans);
            model.addAttribute("totalPlans", availablePlans.size());
            model.addAttribute("heroVideoUrl", heroVideoUrl); // Background video
            model.addAttribute("heroPreviewVideoUrl", "https://www.youtube.com/embed/dQw4w9WgXcQ"); // Preview video
            model.addAttribute("heroFallbackImageUrl", heroFallbackImageUrl);
            model.addAttribute("aboutSectionImageUrl", aboutSectionImageUrl);
            model.addAttribute("contactBackgroundImageUrl", contactBackgroundImageUrl);
            model.addAttribute("backgroundMediaEnabled", backgroundMediaEnabled);
            model.addAttribute("featuredPlan", featuredPlan);

            // Add footer data
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

    /**
     * Test page for JavaScript debugging
     * @return Template name
     */
    @GetMapping("/test")
    public String test() {
        logger.info("Displaying test page");
        return "test";
    }

    /**
     * Test admin functionality page
     * @return Template name
     */
    @GetMapping("/test-admin")
    public String testAdmin() {
        logger.info("Displaying test admin page");
        return "test-admin";
    }

    /**
     * Debug endpoint to serve JavaScript file directly
     */
    @GetMapping(value = "/debug/js", produces = "application/javascript; charset=utf-8")
    @ResponseBody
    public ResponseEntity<String> debugJs() {
        try {
            // Read the JavaScript file directly
            ClassPathResource resource = new ClassPathResource("static/js/app.js");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            logger.info("Serving JavaScript file, content length: {}", content.length());

            return ResponseEntity.ok()
                    .header("Content-Type", "application/javascript; charset=utf-8")
                    .header("Cache-Control", "no-cache")
                    .body(content);
        } catch (Exception e) {
            logger.error("Error reading JavaScript file", e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "text/plain")
                    .body("Error: " + e.getMessage());
        }
    }

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
     * Health check endpoint
     * @return Health status
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        try {
            // Simple health check - verify we can access the database
            long planCount = planService.getActivePlanCount();
            logger.debug("Health check passed - {} active plans", planCount);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }

    /**
     * Actuator health check endpoint (backup)
     * @return Health status
     */
    @GetMapping("/actuator/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actuatorHealthCheck() {
        try {
            // Simple health check - verify we can access the database
            long planCount = planService.getActivePlanCount();
            logger.debug("Actuator health check passed - {} active plans", planCount);

            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("components", Map.of(
                "db", Map.of("status", "UP", "details", Map.of("activePlans", planCount)),
                "diskSpace", Map.of("status", "UP")
            ));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Actuator health check failed", e);

            Map<String, Object> health = new HashMap<>();
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
            // Very simple health check
            planService.getActivePlanCount();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Simple health check failed", e);
            return ResponseEntity.status(503).body("ERROR");
        }
    }
}
