package com.treasurehunt.controller;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.exception.ResourceNotFoundException;
import com.treasurehunt.exception.ValidationException;
import com.treasurehunt.service.AppSettingsService;
import com.treasurehunt.service.EmailNotificationService;
import com.treasurehunt.service.InputSanitizationService;
import com.treasurehunt.service.RegistrationService;
import com.treasurehunt.service.TreasureHuntPlanService;
import com.treasurehunt.service.FileStorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for admin panel functionality
 * Secured with Spring Security - requires admin authentication
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final TreasureHuntPlanService planService;
    private final RegistrationService registrationService;
    private final FileStorageService fileStorageService;
    private final AppSettingsService appSettingsService;
    private final InputSanitizationService inputSanitizationService;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public AdminController(TreasureHuntPlanService planService,
                          RegistrationService registrationService,
                          FileStorageService fileStorageService,
                          AppSettingsService appSettingsService,
                          InputSanitizationService inputSanitizationService,
                          EmailNotificationService emailNotificationService) {
        this.planService = planService;
        this.registrationService = registrationService;
        this.fileStorageService = fileStorageService;
        this.appSettingsService = appSettingsService;
        this.inputSanitizationService = inputSanitizationService;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Admin dashboard
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping
    public String dashboard(Model model) {
        logger.info("Loading admin dashboard");

        try {
            // PERFORMANCE FIX: Get statistics efficiently to avoid N+1 queries
            RegistrationService.RegistrationStatistics stats = registrationService.getRegistrationStatistics();
            List<UserRegistration> recentRegistrations = registrationService.getRecentRegistrations();

            // Get plan statistics from cache to avoid database queries
            List<TreasureHuntPlan> allPlans = planService.getAllPlans();
            long activePlansCount = allPlans.stream()
                    .filter(p -> p.getStatus() == TreasureHuntPlan.PlanStatus.ACTIVE).count();

            // Ensure null safety for template
            if (recentRegistrations == null) {
                recentRegistrations = new ArrayList<>();
                logger.warn("Recent registrations was null, initialized empty list");
            }

            model.addAttribute("totalPlans", allPlans.size());
            model.addAttribute("activePlans", activePlansCount);
            model.addAttribute("totalRegistrations", stats.getTotalRegistrations());
            model.addAttribute("pendingRegistrations", stats.getPendingCount());
            model.addAttribute("confirmedRegistrations", stats.getConfirmedCount());
            model.addAttribute("cancelledRegistrations", stats.getCancelledCount());
            model.addAttribute("recentRegistrations", recentRegistrations);

            return "admin/dashboard";

        } catch (Exception e) {
            logger.error("Error loading admin dashboard", e);
            // Provide safe defaults to prevent template errors
            model.addAttribute("totalPlans", 0);
            model.addAttribute("activePlans", 0);
            model.addAttribute("totalRegistrations", 0);
            model.addAttribute("pendingRegistrations", 0);
            model.addAttribute("confirmedRegistrations", 0);
            model.addAttribute("cancelledRegistrations", 0);
            model.addAttribute("recentRegistrations", new ArrayList<>());
            model.addAttribute("error", "Error loading dashboard data: " + e.getMessage());
            return "admin/dashboard";
        }
    }

    /**
     * Plan management page
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/plans")
    public String managePlans(Model model) {
        logger.debug("Loading plan management page");
        
        try {
            List<TreasureHuntPlan> plans = planService.getAllPlans();
            model.addAttribute("plans", plans);
            model.addAttribute("newPlan", new TreasureHuntPlan());
            
            return "admin/plans";
            
        } catch (Exception e) {
            logger.error("Error loading plan management page", e);
            model.addAttribute("error", "Error loading plans");
            return "admin/plans";
        }
    }

    /**
     * Create new plan
     * @param plan Plan data
     * @param bindingResult Validation results
     * @param redirectAttributes Redirect attributes
     * @return Redirect path
     */
    @PostMapping("/plans")
    public String createPlan(@Valid @ModelAttribute("newPlan") TreasureHuntPlan plan,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        
        logger.info("Creating new plan: {}", plan.getName());
        
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in plan creation: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Please correct the form errors");
            return "redirect:/admin/plans";
        }

        try {
            // Calculate duration from date/time fields before saving
            if (plan.getEventDate() != null && plan.getStartTime() != null &&
                plan.getEndDate() != null && plan.getEndTime() != null) {
                long calculatedHours = plan.calculateDurationHours();
                plan.setDurationHours((int) calculatedHours);
                logger.info("Calculated duration: {} hours for plan: {}", calculatedHours, plan.getName());
            } else {
                // Set default duration if date/time fields are missing
                plan.setDurationHours(8);
                logger.warn("Date/time fields missing, setting default duration of 8 hours for plan: {}", plan.getName());
            }

            planService.createPlan(plan);
            redirectAttributes.addFlashAttribute("success", "Plan created successfully");
            logger.info("Successfully created plan: {}", plan.getName());
            
        } catch (Exception e) {
            logger.error("Error creating plan", e);
            redirectAttributes.addFlashAttribute("error", "Error creating plan: " + e.getMessage());
        }

        return "redirect:/admin/plans";
    }

    /**
     * Create new plan (AJAX)
     * @param plan Plan data
     * @param bindingResult Validation results
     * @return JSON response
     */
    @PostMapping("/plans/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPlanAjax(@Valid @ModelAttribute TreasureHuntPlan plan,
                                                              BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        logger.info("Creating new plan via AJAX: {}", plan.getName());

        try {
            if (bindingResult.hasErrors()) {
                List<String> errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(java.util.stream.Collectors.toList());

                logger.warn("Validation errors in plan creation: {}", errors);
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate duration from date/time fields before saving
            if (plan.getEventDate() != null && plan.getStartTime() != null &&
                plan.getEndDate() != null && plan.getEndTime() != null) {
                long calculatedHours = plan.calculateDurationHours();
                plan.setDurationHours((int) calculatedHours);
                logger.info("Calculated duration: {} hours for plan: {}", calculatedHours, plan.getName());
            } else {
                // Set default duration if date/time fields are missing
                plan.setDurationHours(8);
                logger.warn("Date/time fields missing, setting default duration of 8 hours for plan: {}", plan.getName());
            }

            TreasureHuntPlan createdPlan = planService.createPlan(plan);
            response.put("success", true);
            response.put("message", "Plan created successfully");
            response.put("plan", createdPlan);
            logger.info("Successfully created plan via AJAX: {}", plan.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating plan via AJAX", e);
            response.put("success", false);
            response.put("message", "Error creating plan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update plan
     * @param id Plan ID
     * @param plan Updated plan data
     * @param bindingResult Validation results
     * @param redirectAttributes Redirect attributes
     * @return Redirect path
     */
    @PostMapping("/plans/{id}")
    public String updatePlan(@PathVariable Long id,
                            @Valid @ModelAttribute TreasureHuntPlan plan,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        
        logger.info("Updating plan with ID: {}", id);
        
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in plan update: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Please correct the form errors");
            return "redirect:/admin/plans";
        }

        try {
            planService.updatePlan(id, plan);
            redirectAttributes.addFlashAttribute("success", "Plan updated successfully");
            logger.info("Successfully updated plan with ID: {}", id);
            
        } catch (Exception e) {
            logger.error("Error updating plan with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating plan: " + e.getMessage());
        }

        return "redirect:/admin/plans";
    }

    /**
     * Toggle plan status (AJAX)
     * @param id Plan ID
     * @return JSON response
     */
    @PostMapping("/plans/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> togglePlanStatus(@PathVariable Long id) {
        logger.info("Toggling status for plan ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TreasureHuntPlan updatedPlan = planService.togglePlanStatus(id);
            response.put("success", true);
            response.put("newStatus", updatedPlan.getStatus().toString());
            response.put("message", "Plan status updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling plan status for ID: {}", id, e);
            response.put("success", false);
            response.put("message", "Error updating plan status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get plan for editing
     * @param id Plan ID
     * @return JSON response with plan data
     */
    @GetMapping("/plans/{id}/edit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPlanForEdit(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<TreasureHuntPlan> planOpt = planService.getPlanById(id);
            if (planOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Plan not found");
                return ResponseEntity.notFound().build();
            }

            TreasureHuntPlan plan = planOpt.get();
            response.put("success", true);
            response.put("plan", plan);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting plan for edit with ID: {}", id, e);
            response.put("success", false);
            response.put("message", "Error loading plan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update plan
     * @param id Plan ID
     * @param plan Updated plan data
     * @param bindingResult Validation results
     * @return JSON response
     */
    @PostMapping("/plans/{id}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePlan(@PathVariable Long id,
                                                          @Valid @ModelAttribute TreasureHuntPlan plan,
                                                          BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (bindingResult.hasErrors()) {
                response.put("success", false);
                response.put("message", "Validation errors occurred");
                response.put("errors", bindingResult.getAllErrors());
                return ResponseEntity.badRequest().body(response);
            }

            Optional<TreasureHuntPlan> existingPlanOpt = planService.getPlanById(id);
            if (existingPlanOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Plan not found");
                return ResponseEntity.notFound().build();
            }

            TreasureHuntPlan existingPlan = existingPlanOpt.get();

            // Update fields
            existingPlan.setName(plan.getName());
            existingPlan.setDescription(plan.getDescription());
            existingPlan.setDifficultyLevel(plan.getDifficultyLevel());
            existingPlan.setMaxParticipants(plan.getMaxParticipants());
            existingPlan.setTeamSize(plan.getTeamSize());
            existingPlan.setTeamType(plan.getTeamType());
            existingPlan.setPriceInr(plan.getPriceInr());
            existingPlan.setPreviewVideoUrl(plan.getPreviewVideoUrl());
            existingPlan.setEventDate(plan.getEventDate());
            existingPlan.setStartTime(plan.getStartTime());
            existingPlan.setEndDate(plan.getEndDate());
            existingPlan.setEndTime(plan.getEndTime());
            existingPlan.setBatchesCompleted(plan.getBatchesCompleted());
            existingPlan.setRating(plan.getRating());
            existingPlan.setPrizeMoney(plan.getPrizeMoney());
            existingPlan.setAvailableSlots(plan.getAvailableSlots());

            // Calculate and update duration based on new date/time fields
            if (plan.getEventDate() != null && plan.getStartTime() != null &&
                plan.getEndDate() != null && plan.getEndTime() != null) {
                long calculatedHours = existingPlan.calculateDurationHours();
                existingPlan.setDurationHours((int) calculatedHours);
            }

            TreasureHuntPlan updatedPlan = planService.updatePlan(existingPlan);

            response.put("success", true);
            response.put("message", "Plan updated successfully");
            response.put("plan", updatedPlan);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating plan with ID: {}", id, e);
            response.put("success", false);
            response.put("message", "Error updating plan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }



    /**
     * Delete plan
     * @param id Plan ID
     * @return JSON response
     */
    @PostMapping("/plans/{id}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            planService.deletePlan(id);

            response.put("success", true);
            response.put("message", "Plan deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting plan with ID: {}", id, e);
            response.put("success", false);
            response.put("message", "Error deleting plan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Registration management page
     * @param model Thymeleaf model
     * @param status Optional status filter
     * @param planId Optional plan filter
     * @return Template name
     */
    @GetMapping("/registrations")
    public String manageRegistrations(Model model,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Long planId) {
        logger.info("Loading registration management page with status filter: {} and plan filter: {}", status, planId);

        // Input validation and sanitization
        if (status != null) {
            status = inputSanitizationService.sanitizeText(status);
            if (status != null && !status.isEmpty() && !status.equals("all")) {
                try {
                    UserRegistration.RegistrationStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status filter provided: {}", status);
                    model.addAttribute("error", "Invalid status filter provided");
                    status = null;
                }
            }
        }

        if (planId != null && planId <= 0) {
            logger.warn("Invalid plan ID provided: {}", planId);
            model.addAttribute("error", "Invalid plan ID provided");
            planId = null;
        }

        try {
            List<UserRegistration> registrations;

            // Apply filters based on parameters
            if (planId != null && status != null && !status.isEmpty() && !status.equals("all")) {
                // Both plan and status filters
                UserRegistration.RegistrationStatus registrationStatus =
                    UserRegistration.RegistrationStatus.valueOf(status.toUpperCase());
                registrations = registrationService.getRegistrationsByPlanAndStatus(planId, registrationStatus);
                logger.debug("Found {} registrations for plan {} with status: {}", registrations.size(), planId, status);
            } else if (planId != null) {
                // Only plan filter
                registrations = registrationService.getRegistrationsByPlan(planId);
                logger.debug("Found {} registrations for plan: {}", registrations.size(), planId);
            } else if (status != null && !status.isEmpty() && !status.equals("all")) {
                // Only status filter
                UserRegistration.RegistrationStatus registrationStatus =
                    UserRegistration.RegistrationStatus.valueOf(status.toUpperCase());
                registrations = registrationService.getRegistrationsByStatus(registrationStatus);
                logger.debug("Found {} registrations with status: {}", registrations.size(), status);
            } else {
                // No filters
                registrations = registrationService.getAllRegistrations();
                logger.debug("Found {} total registrations", registrations.size());
            }

            model.addAttribute("registrations", registrations);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedPlanId", planId);
            model.addAttribute("statuses", UserRegistration.RegistrationStatus.values());

            // PERFORMANCE FIX: Get plans from cache to avoid additional database query
            List<TreasureHuntPlan> allPlans = planService.getAllPlans();
            model.addAttribute("allPlans", allPlans);

            return "admin/registrations";

        } catch (Exception e) {
            logger.error("Error loading registration management page", e);
            // Provide safe defaults to prevent template errors
            model.addAttribute("registrations", new ArrayList<>());
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedPlanId", planId);
            model.addAttribute("statuses", UserRegistration.RegistrationStatus.values());
            model.addAttribute("allPlans", new ArrayList<>());
            model.addAttribute("error", "Error loading registrations: " + e.getMessage());
            return "admin/registrations";
        }
    }



    /**
     * Get registration details (HTML fragment for modal)
     * @param id Registration ID
     * @param model Thymeleaf model
     * @return HTML fragment
     */
    @GetMapping("/registrations/{id}")
    public String getRegistrationDetails(@PathVariable Long id, Model model) {
        logger.debug("Fetching registration details for ID: {}", id);

        try {
            Optional<UserRegistration> registrationOpt = registrationService.getRegistrationByIdWithDetails(id);

            if (registrationOpt.isPresent()) {
                UserRegistration registration = registrationOpt.get();
                model.addAttribute("registration", registration);
                model.addAttribute("isTeamRegistration", registration.isTeamRegistration());
                model.addAttribute("teamMembers", registration.getTeamMembers());
                model.addAttribute("documents", registration.getDocuments());

                logger.debug("Successfully loaded registration details for ID: {} (Team: {}, Members: {}, Documents: {})",
                           id, registration.isTeamRegistration(), registration.getTeamMembers().size(), registration.getDocuments().size());

                return "admin/fragments/registration-details :: registrationDetails";
            } else {
                logger.warn("Registration not found for ID: {}", id);
                model.addAttribute("error", "Registration not found");
                return "admin/fragments/registration-details :: error";
            }

        } catch (Exception e) {
            logger.error("Error fetching registration details for ID: {}", id, e);
            model.addAttribute("error", "Error loading registration details: " + e.getMessage());
            return "admin/fragments/registration-details :: error";
        }
    }

    /**
     * Search registrations (AJAX)
     * @param query Search query
     * @param type Search type (email or name)
     * @return JSON response with search results
     */
    @GetMapping("/registrations/search")
    @ResponseBody
    public ResponseEntity<List<UserRegistration>> searchRegistrations(
            @RequestParam String query,
            @RequestParam(defaultValue = "email") String type) {

        // CRITICAL: Sanitize search input to prevent injection attacks
        String sanitizedQuery = inputSanitizationService.sanitizeSearchQuery(query);
        if (sanitizedQuery.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        logger.debug("Searching registrations by {}: [SANITIZED]", type);

        try {
            List<UserRegistration> results;

            if ("email".equals(type)) {
                results = registrationService.searchByEmail(sanitizedQuery);
            } else if ("name".equals(type)) {
                results = registrationService.searchByName(sanitizedQuery);
            } else {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error searching registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update registration status (AJAX)
     * @param id Registration ID
     * @param status New status
     * @return JSON response
     */
    @PostMapping("/registrations/{id}/status")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateRegistrationStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        logger.debug("Updating registration {} status to: {}", id, status);

        Map<String, Object> response = new HashMap<>();

        try {
            UserRegistration.RegistrationStatus newStatus =
                UserRegistration.RegistrationStatus.valueOf(status.toUpperCase());

            UserRegistration updatedRegistration = registrationService.updateRegistrationStatus(id, newStatus);

            response.put("success", true);
            response.put("message", "Status updated successfully");
            response.put("newStatus", updatedRegistration.getStatus().toString());

            // Send appropriate emails based on status change
            if (newStatus == UserRegistration.RegistrationStatus.CONFIRMED) {
                // Send approval emails to all participants
                logger.info("Sending approval emails for confirmed registration: {}", id);
                registrationService.sendConfirmationEmails(updatedRegistration);
            } else if (newStatus == UserRegistration.RegistrationStatus.CANCELLED) {
                // Send cancellation email to team leader or individual
                logger.info("Sending cancellation email for cancelled registration: {}", id);
                registrationService.sendCancellationEmail(updatedRegistration);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", status, e);
            response.put("success", false);
            response.put("message", "Invalid status value");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("Error updating registration status for ID: {}", id, e);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Download uploaded document
     * @param documentId Document ID
     * @return File download response
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        logger.debug("Downloading document with ID: {}", documentId);

        try {
            UploadedDocument document = fileStorageService.getDocumentById(documentId);
            Resource resource = fileStorageService.loadFileAsResource(document.getStoredFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading document with ID: {}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * View uploaded document (inline)
     * @param documentId Document ID
     * @return File view response
     */
    @GetMapping("/documents/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long documentId) {
        logger.debug("Viewing document with ID: {}", documentId);

        try {
            UploadedDocument document = fileStorageService.getDocumentById(documentId);
            Resource resource = fileStorageService.loadFileAsResource(document.getStoredFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error viewing document with ID: {}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Settings management page
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        logger.debug("Loading settings management page");

        try {
            // PRODUCTION FIX: Add safe fallbacks for all settings to prevent 500 errors
            String heroPreviewVideoUrl = safeGetSetting(() -> appSettingsService.getHeroPreviewVideoUrl(), "https://www.youtube.com/embed/dQw4w9WgXcQ");
            List<TreasureHuntPlan> activePlans = safeGetPlans(() -> planService.getActivePlans());
            TreasureHuntPlan featuredPlan = safeGetFeaturedPlan(() -> planService.getFeaturedPlan());

            model.addAttribute("heroPreviewVideoUrl", heroPreviewVideoUrl);
            model.addAttribute("activePlans", activePlans);
            model.addAttribute("featuredPlan", featuredPlan);
            model.addAttribute("companyInfo", safeGetSetting(() -> appSettingsService.getCompanyInfo(), new HashMap<>()));
            model.addAttribute("socialLinks", safeGetSetting(() -> appSettingsService.getSocialMediaLinks(), new HashMap<>()));
            model.addAttribute("contactInfo", safeGetSetting(() -> appSettingsService.getContactInfo(), new HashMap<>()));
            model.addAttribute("heroBlurIntensity", safeGetSetting(() -> appSettingsService.getHeroBlurIntensity(), 5));

            // FIXED: Add image URLs for admin settings page with safe fallbacks
            model.addAttribute("heroFallbackImageUrl", safeGetSetting(() -> appSettingsService.getHeroFallbackImageUrl(), "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"));
            model.addAttribute("aboutSectionImageUrl", safeGetSetting(() -> appSettingsService.getAboutSectionImageUrl(), "https://images.unsplash.com/photo-1559827260-dc66d52bef19?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"));
            model.addAttribute("contactBackgroundImageUrl", safeGetSetting(() -> appSettingsService.getContactBackgroundImageUrl(), "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80"));

            return "admin/settings";

        } catch (Exception e) {
            logger.error("Error loading settings page", e);
            model.addAttribute("error", "Error loading settings: " + e.getMessage());
            // PRODUCTION FIX: Add emergency fallback attributes to prevent template errors
            addEmergencyFallbackAttributes(model);
            return "admin/settings";
        }
    }

    /**
     * Update hero preview video URL (YouTube embed only)
     * @param videoUrl New YouTube embed URL
     * @return JSON response
     */
    @PostMapping("/settings/hero-video")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateHeroVideo(@RequestParam String videoUrl) {
        Map<String, Object> response = new HashMap<>();

        try {
            appSettingsService.updateHeroPreviewVideoUrl(videoUrl);

            response.put("success", true);
            response.put("message", "Hero preview video updated successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating hero preview video URL", e);
            response.put("success", false);
            response.put("message", "Error updating video URL: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Set featured plan
     * @param planId Plan ID to set as featured
     * @return JSON response
     */
    @PostMapping("/settings/featured-plan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setFeaturedPlan(@RequestParam Long planId) {
        Map<String, Object> response = new HashMap<>();

        try {
            planService.setFeaturedPlan(planId);

            response.put("success", true);
            response.put("message", "Featured plan updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error setting featured plan", e);
            response.put("success", false);
            response.put("message", "Error updating featured plan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update hero fallback image URL
     * @param imageUrl New image URL
     * @return JSON response
     */
    @PostMapping("/settings/hero-fallback-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateHeroFallbackImage(@RequestParam String imageUrl) {
        Map<String, Object> response = new HashMap<>();

        try {
            appSettingsService.updateHeroFallbackImageUrl(imageUrl);

            response.put("success", true);
            response.put("message", "Hero fallback image updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating hero fallback image", e);
            response.put("success", false);
            response.put("message", "Error updating image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update about section image URL
     * @param imageUrl New image URL
     * @return JSON response
     */
    @PostMapping("/settings/about-section-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAboutSectionImage(@RequestParam String imageUrl) {
        Map<String, Object> response = new HashMap<>();

        try {
            appSettingsService.updateAboutSectionImageUrl(imageUrl);

            response.put("success", true);
            response.put("message", "About section image updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating about section image", e);
            response.put("success", false);
            response.put("message", "Error updating image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update contact background image URL
     * @param imageUrl New image URL
     * @return JSON response
     */
    @PostMapping("/settings/contact-background-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateContactBackgroundImage(@RequestParam String imageUrl) {
        Map<String, Object> response = new HashMap<>();

        try {
            appSettingsService.updateContactBackgroundImageUrl(imageUrl);

            response.put("success", true);
            response.put("message", "Contact background image updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating contact background image", e);
            response.put("success", false);
            response.put("message", "Error updating image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update company information
     * @param companyName Company name
     * @param companyAddress Company address
     * @param companyPhone Company phone
     * @param companyEmail Company email
     * @return JSON response
     */
    @PostMapping("/settings/company-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCompanyInfo(
            @RequestParam String companyName,
            @RequestParam String companyAddress,
            @RequestParam String companyPhone,
            @RequestParam String companyEmail) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> companyInfo = new HashMap<>();
            companyInfo.put("name", companyName);
            companyInfo.put("address", companyAddress);
            companyInfo.put("phone", companyPhone);
            companyInfo.put("email", companyEmail);

            appSettingsService.updateCompanyInfo(companyInfo);

            response.put("success", true);
            response.put("message", "Company information updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating company information", e);
            response.put("success", false);
            response.put("message", "Error updating company information: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update social media links
     * @param facebookUrl Facebook URL
     * @param twitterUrl Twitter URL
     * @param instagramUrl Instagram URL
     * @param linkedinUrl LinkedIn URL
     * @param youtubeUrl YouTube URL
     * @return JSON response
     */
    @PostMapping("/settings/social-media")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSocialMedia(
            @RequestParam String facebookUrl,
            @RequestParam String twitterUrl,
            @RequestParam String instagramUrl,
            @RequestParam String linkedinUrl,
            @RequestParam String youtubeUrl) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> socialLinks = new HashMap<>();
            socialLinks.put("facebook", facebookUrl);
            socialLinks.put("twitter", twitterUrl);
            socialLinks.put("instagram", instagramUrl);
            socialLinks.put("linkedin", linkedinUrl);
            socialLinks.put("youtube", youtubeUrl);

            appSettingsService.updateSocialMediaLinks(socialLinks);

            response.put("success", true);
            response.put("message", "Social media links updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating social media links", e);
            response.put("success", false);
            response.put("message", "Error updating social media links: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update available slots for a plan
     * @param planId Plan ID
     * @param availableSlots New available slots value
     * @return JSON response
     */
    @PostMapping("/plans/{planId}/update-slots")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAvailableSlots(
            @PathVariable Long planId,
            @RequestParam Integer availableSlots) {
        Map<String, Object> response = new HashMap<>();

        try {
            TreasureHuntPlan updatedPlan = planService.updateAvailableSlots(planId, availableSlots);

            response.put("success", true);
            response.put("message", "Available slots updated successfully");
            response.put("newSlots", updatedPlan.getAvailableSlots());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating available slots for plan ID: {}", planId, e);
            response.put("success", false);
            response.put("message", "Error updating available slots: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update contact information
     * @param contactPhone Contact phone number
     * @param contactEmail Contact email address
     * @param contactAddress Contact address
     * @param contactHours Business hours
     * @param contactEmergency Emergency contact
     * @return JSON response
     */
    @PostMapping("/settings/contact-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateContactInfo(
            @RequestParam String contactPhone,
            @RequestParam String contactEmail,
            @RequestParam String contactAddress,
            @RequestParam String contactHours,
            @RequestParam String contactEmergency) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> contactInfo = new HashMap<>();
            contactInfo.put("phone", contactPhone);
            contactInfo.put("email", contactEmail);
            contactInfo.put("address", contactAddress);
            contactInfo.put("hours", contactHours);
            contactInfo.put("emergency", contactEmergency);

            appSettingsService.updateContactInfo(contactInfo);

            response.put("success", true);
            response.put("message", "Contact information updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating contact information", e);
            response.put("success", false);
            response.put("message", "Error updating contact information: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // PRODUCTION FIX: Helper methods for safe settings retrieval to prevent 500 errors

    /**
     * Safely get a setting with fallback value
     */
    private <T> T safeGetSetting(java.util.function.Supplier<T> supplier, T fallback) {
        try {
            T result = supplier.get();
            return result != null ? result : fallback;
        } catch (Exception e) {
            logger.warn("Error getting setting, using fallback: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * Safely get plans with fallback
     */
    private List<TreasureHuntPlan> safeGetPlans(java.util.function.Supplier<List<TreasureHuntPlan>> supplier) {
        try {
            List<TreasureHuntPlan> result = supplier.get();
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Error getting plans, using empty list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Safely get featured plan with fallback
     */
    private TreasureHuntPlan safeGetFeaturedPlan(java.util.function.Supplier<TreasureHuntPlan> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            logger.warn("Error getting featured plan, using null: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Add emergency fallback attributes to prevent template errors
     */
    private void addEmergencyFallbackAttributes(Model model) {
        model.addAttribute("heroPreviewVideoUrl", "https://www.youtube.com/embed/dQw4w9WgXcQ");
        model.addAttribute("activePlans", new ArrayList<>());
        model.addAttribute("featuredPlan", null);
        model.addAttribute("companyInfo", new HashMap<>());
        model.addAttribute("socialLinks", new HashMap<>());
        model.addAttribute("contactInfo", new HashMap<>());
        model.addAttribute("heroBlurIntensity", 5);
        model.addAttribute("heroFallbackImageUrl", "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
        model.addAttribute("aboutSectionImageUrl", "https://images.unsplash.com/photo-1559827260-dc66d52bef19?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
        model.addAttribute("contactBackgroundImageUrl", "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=80");
    }
}
