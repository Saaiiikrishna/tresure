package com.treasurehunt.controller;

import com.treasurehunt.dto.RegistrationRequestDTO;
import com.treasurehunt.dto.TeamMemberDTO;
import com.treasurehunt.entity.TeamMember;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

            // Validate file uploads with conditional medical certificate logic
            String fileValidationError = validateFileUploads(photoFile, idFile, medicalFile, registration.getMedicalConsentGiven());
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
     * Validate file uploads with conditional medical certificate logic
     * @param photoFile Photo file
     * @param idFile ID document file
     * @param medicalFile Medical certificate file
     * @param medicalConsentGiven Whether medical consent was given
     * @return Error message if validation fails, null if valid
     */
    private String validateFileUploads(MultipartFile photoFile, MultipartFile idFile, MultipartFile medicalFile, Boolean medicalConsentGiven) {
        // Check if all required files are provided
        if (photoFile == null || photoFile.isEmpty()) {
            return "Passport photo is required";
        }

        if (idFile == null || idFile.isEmpty()) {
            return "Government ID document is required";
        }

        // Conditional medical certificate validation
        if (medicalConsentGiven == null || !medicalConsentGiven) {
            // If medical consent is NOT given, medical certificate is MANDATORY
            if (medicalFile == null || medicalFile.isEmpty()) {
                return "Medical certificate is required when medical consent is not given";
            }
        }
        // If medical consent IS given, medical certificate is OPTIONAL (no validation needed)

        // Temporarily disable file size validation for testing email functionality
        /*
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
        */

        // Temporarily disable file type validation for testing email functionality
        /*
        // Validate file types (basic check)
        String photoContentType = photoFile.getContentType();
        if (photoContentType == null || !photoContentType.startsWith("image/")) {
            return "Photo must be an image file (JPG, JPEG, PNG)";
        }

        String medicalContentType = medicalFile.getContentType();
        if (medicalContentType == null || !"application/pdf".equals(medicalContentType)) {
            return "Medical certificate must be a PDF file";
        }
        */

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

    /**
     * Handle team registration form submission with file uploads
     * @param registrationData Team registration data
     * @param bindingResult Validation results
     * @param photoFile Passport photo file
     * @param idFile Government ID file
     * @param medicalFile Medical certificate file
     * @return JSON response with registration result
     */
    @PostMapping("/team")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitTeamRegistration(
            @Valid @ModelAttribute RegistrationRequestDTO registrationData,
            BindingResult bindingResult,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "idFile", required = false) MultipartFile idFile,
            @RequestParam(value = "medicalFile", required = false) MultipartFile medicalFile) {

        logger.info("Processing team registration submission for plan ID: {} with {} members",
                   registrationData.getPlanId(), registrationData.getMembers().size());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate form data
            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors in team registration form: {}", bindingResult.getAllErrors());
                response.put("success", false);
                response.put("message", "Please correct the form errors and try again.");
                response.put("errors", getValidationErrors(bindingResult));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file uploads with conditional medical certificate logic
            String fileValidationError = validateFileUploads(photoFile, idFile, medicalFile, registrationData.getMedicalConsentGiven());
            if (fileValidationError != null) {
                logger.warn("File validation error in team registration: {}", fileValidationError);
                response.put("success", false);
                response.put("message", fileValidationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate team registration data
            if (!registrationData.isValidTeamRegistration()) {
                logger.warn("Invalid team registration data");
                response.put("success", false);
                response.put("message", "Invalid team registration data. Please check team size and member information.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate emergency contact requirements for team members
            for (int i = 0; i < registrationData.getMembers().size(); i++) {
                TeamMemberDTO member = registrationData.getMembers().get(i);
                boolean isTeamLeader = i == 0; // First member is team leader
                boolean isIndividualRegistration = false; // This is team registration

                if (!member.validateEmergencyContacts(isTeamLeader, isIndividualRegistration)) {
                    logger.warn("Emergency contact validation failed for team member {}", i + 1);
                    response.put("success", false);
                    response.put("message", "Emergency contact information is required for the team leader.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Get the treasure hunt plan
            Optional<TreasureHuntPlan> planOpt = planService.getPlanById(registrationData.getPlanId());
            if (planOpt.isEmpty()) {
                logger.warn("Plan not found with ID: {}", registrationData.getPlanId());
                response.put("success", false);
                response.put("message", "Selected treasure hunt plan not found.");
                return ResponseEntity.badRequest().body(response);
            }

            TreasureHuntPlan plan = planOpt.get();

            // Validate team size matches plan requirements
            if (!plan.getTeamSize().equals(registrationData.getTeamSize())) {
                logger.warn("Team size mismatch. Plan requires: {}, submitted: {}",
                           plan.getTeamSize(), registrationData.getTeamSize());
                response.put("success", false);
                response.put("message", "Team size does not match plan requirements.");
                return ResponseEntity.badRequest().body(response);
            }

            // Create UserRegistration from team leader data
            TeamMemberDTO teamLeader = registrationData.getTeamLeader();
            UserRegistration registration = new UserRegistration();
            registration.setFullName(teamLeader.getFullName());
            registration.setAge(teamLeader.getAge());
            registration.setGender(UserRegistration.Gender.valueOf(teamLeader.getGender()));
            registration.setEmail(teamLeader.getEmail());
            registration.setPhoneNumber(teamLeader.getPhoneNumber());
            registration.setEmergencyContactName(teamLeader.getEmergencyContactName());
            registration.setEmergencyContactPhone(teamLeader.getEmergencyContactPhone());
            registration.setMedicalConsentGiven(registrationData.getMedicalConsentGiven());
            registration.setPlan(plan);
            registration.setTeamName(registrationData.getTeamName());

            // Create team members
            List<TeamMember> teamMembers = new ArrayList<>();
            for (int i = 0; i < registrationData.getMembers().size(); i++) {
                TeamMemberDTO memberDTO = registrationData.getMembers().get(i);
                TeamMember teamMember = new TeamMember();
                teamMember.setFullName(memberDTO.getFullName());
                teamMember.setAge(memberDTO.getAge());
                teamMember.setGender(memberDTO.getGender());
                teamMember.setEmail(memberDTO.getEmail());
                teamMember.setPhoneNumber(memberDTO.getPhoneNumber());

                // Only set emergency contacts if they are provided (required for team leader, optional for members)
                if (memberDTO.getEmergencyContactName() != null && !memberDTO.getEmergencyContactName().trim().isEmpty()) {
                    teamMember.setEmergencyContactName(memberDTO.getEmergencyContactName());
                }
                if (memberDTO.getEmergencyContactPhone() != null && !memberDTO.getEmergencyContactPhone().trim().isEmpty()) {
                    teamMember.setEmergencyContactPhone(memberDTO.getEmergencyContactPhone());
                }

                // Set bio if provided
                if (memberDTO.getBio() != null && !memberDTO.getBio().trim().isEmpty()) {
                    teamMember.setBio(memberDTO.getBio());
                }

                teamMember.setMemberPosition(i + 1); // 1-based position

                // Set medical consent - all team members inherit team leader's consent
                teamMember.setMedicalConsentGiven(registrationData.getMedicalConsentGiven());

                teamMember.setRegistration(registration);
                teamMembers.add(teamMember);
            }
            registration.setTeamMembers(teamMembers);

            // Process the registration
            UserRegistration savedRegistration = registrationService.createRegistration(
                registration, photoFile, idFile, medicalFile);

            logger.info("Team registration successful with ID: {}", savedRegistration.getId());

            response.put("success", true);
            response.put("message", "Team registration submitted successfully! You will receive a confirmation email shortly.");
            response.put("registrationNumber", savedRegistration.getRegistrationNumber());
            response.put("teamName", savedRegistration.getTeamName());
            response.put("teamSize", savedRegistration.getTeamSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing team registration", e);
            response.put("success", false);
            response.put("message", "An error occurred while processing your registration. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
