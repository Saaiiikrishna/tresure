package com.treasurehunt.controller;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.service.TreasureHuntPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for home page and plan display
 * Handles main landing page and plan-related API endpoints
 */
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final TreasureHuntPlanService planService;

    @Autowired
    public HomeController(TreasureHuntPlanService planService) {
        this.planService = planService;
    }

    /**
     * Display main landing page with available plans
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/")
    public String home(Model model) {
        logger.debug("Displaying home page");
        
        try {
            List<TreasureHuntPlan> availablePlans = planService.getAvailablePlans();
            model.addAttribute("plans", availablePlans);
            model.addAttribute("totalPlans", availablePlans.size());
            
            logger.debug("Found {} available plans for home page", availablePlans.size());
            return "index";
            
        } catch (Exception e) {
            logger.error("Error loading home page", e);
            model.addAttribute("error", "Unable to load treasure hunt plans. Please try again later.");
            return "error";
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
}
