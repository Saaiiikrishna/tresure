package com.treasurehunt.controller;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.service.RegistrationService;
import com.treasurehunt.service.TreasureHuntPlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling user registrations
 * Manages registration form submission and file uploads
 */
@Controller
@RequestMapping("/api/register")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final RegistrationService registrationService;
    private final TreasureHuntPlanService planService;

    @Autowired
    public RegistrationController(RegistrationService registrationService,
                                 TreasureHuntPlanService planService) {
        this.registrationService = registrationService;
        this.planService = planService;
    }

    /**
     * Handle registration form submission with file uploads
     * @param registration Registration data
     * @param bindingResult Validation results
     * @param planId Plan ID
     * @param photoFile Passport photo file
     * @param idFile Government ID file
     * @param medicalFile Medical certificate file
     * @return JSON response with registration result
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitRegistration(
            @Valid @ModelAttribute UserRegistration registration,
            BindingResult bindingResult,
            @RequestParam("planId") Long planId,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "idFile", required = false) MultipartFile idFile,
            @RequestParam(value = "medicalFile", required = false) MultipartFile medicalFile) {

        logger.info("Processing registration submission for plan ID: {} and email: {}", 
                   planId, registration.getEmail());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate form data
            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors in registration form: {}", bindingResult.getAllErrors());
                response.put("success", false);
                response.put("message", "Please correct the form errors and try again.");
                response.put("errors", getValidationErrors(bindingResult));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file uploads
            String fileValidationError = validateFileUploads(photoFile, idFile, medicalFile);
            if (fileValidationError != null) {
                logger.warn("File validation error: {}", fileValidationError);
                response.put("success", false);
                response.put("message", fileValidationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Get and validate plan
            Optional<TreasureHuntPlan> planOpt = planService.getAvailablePlanById(planId);
            if (planOpt.isEmpty()) {
                logger.warn("Plan not available for registration: {}", planId);
                response.put("success", false);
                response.put("message", "Selected plan is not available for registration.");
                return ResponseEntity.badRequest().body(response);
            }

            // Set plan and create registration
            registration.setPlan(planOpt.get());
            UserRegistration savedRegistration = registrationService.createRegistration(
                    registration, photoFile, idFile, medicalFile);

            logger.info("Successfully created registration with ID: {}", savedRegistration.getId());

            // Prepare success response
            response.put("success", true);
            response.put("message", "Registration submitted successfully! You will receive a confirmation email shortly.");
            response.put("registrationId", savedRegistration.getId());
            response.put("registrationNumber", savedRegistration.getRegistrationNumber());
            response.put("planName", savedRegistration.getPlan().getName());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Registration validation error: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("Error processing registration", e);
            response.put("success", false);
            response.put("message", "An error occurred while processing your registration. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get registration details by ID
     * @param id Registration ID
     * @return JSON response with registration details
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRegistration(@PathVariable Long id) {
        logger.debug("Fetching registration details for ID: {}", id);

        try {
            Optional<UserRegistration> registrationOpt = registrationService.getRegistrationById(id);
            
            if (registrationOpt.isEmpty()) {
                logger.debug("Registration not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            UserRegistration registration = registrationOpt.get();
            Map<String, Object> response = new HashMap<>();
            
            response.put("id", registration.getId());
            response.put("registrationNumber", registration.getRegistrationNumber());
            response.put("fullName", registration.getFullName());
            response.put("email", registration.getEmail());
            response.put("status", registration.getStatus());
            response.put("registrationDate", registration.getRegistrationDate());
            response.put("planName", registration.getPlan().getName());
            response.put("hasRequiredDocuments", registration.hasRequiredDocuments());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching registration with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if email is already registered for a plan
     * @param email Email address
     * @param planId Plan ID
     * @return JSON response with availability status
     */
    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(
            @RequestParam String email,
            @RequestParam Long planId) {
        
        logger.debug("Checking email availability: {} for plan ID: {}", email, planId);

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<TreasureHuntPlan> planOpt = planService.getPlanById(planId);
            if (planOpt.isEmpty()) {
                response.put("available", false);
                response.put("message", "Plan not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Check for existing registration
            boolean isAvailable = registrationService.searchByEmail(email).stream()
                    .noneMatch(reg -> reg.getPlan().getId().equals(planId) && 
                              reg.getStatus() != UserRegistration.RegistrationStatus.CANCELLED);

            response.put("available", isAvailable);
            response.put("message", isAvailable ? "Email is available" : "Email is already registered for this plan");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking email availability", e);
            response.put("available", false);
            response.put("message", "Error checking email availability");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Validate file uploads
     * @param photoFile Photo file
     * @param idFile ID document file
     * @param medicalFile Medical certificate file
     * @return Error message if validation fails, null if valid
     */
    private String validateFileUploads(MultipartFile photoFile, MultipartFile idFile, MultipartFile medicalFile) {
        // Check if all required files are provided
        if (photoFile == null || photoFile.isEmpty()) {
            return "Passport photo is required";
        }
        
        if (idFile == null || idFile.isEmpty()) {
            return "Government ID document is required";
        }
        
        if (medicalFile == null || medicalFile.isEmpty()) {
            return "Medical certificate is required";
        }

        // Validate file sizes (basic check - detailed validation in FileStorageService)
        if (photoFile.getSize() > 2 * 1024 * 1024) { // 2MB
            return "Photo file size must not exceed 2MB";
        }
        
        if (idFile.getSize() > 5 * 1024 * 1024) { // 5MB
            return "ID document file size must not exceed 5MB";
        }
        
        if (medicalFile.getSize() > 5 * 1024 * 1024) { // 5MB
            return "Medical certificate file size must not exceed 5MB";
        }

        // Validate file types (basic check)
        String photoContentType = photoFile.getContentType();
        if (photoContentType == null || !photoContentType.startsWith("image/")) {
            return "Photo must be an image file (JPG, JPEG, PNG)";
        }

        String medicalContentType = medicalFile.getContentType();
        if (medicalContentType == null || !"application/pdf".equals(medicalContentType)) {
            return "Medical certificate must be a PDF file";
        }

        return null; // All validations passed
    }

    /**
     * Extract validation errors from BindingResult
     * @param bindingResult Binding result with errors
     * @return Map of field errors
     */
    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        
        bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        bindingResult.getGlobalErrors().forEach(error -> 
            errors.put("global", error.getDefaultMessage())
        );
        
        return errors;
    }
}
