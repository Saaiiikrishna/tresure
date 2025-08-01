package com.treasurehunt.controller;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.UploadedDocument;
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

    @Autowired
    public AdminController(TreasureHuntPlanService planService,
                          RegistrationService registrationService,
                          FileStorageService fileStorageService) {
        this.planService = planService;
        this.registrationService = registrationService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Admin dashboard
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping
    public String dashboard(Model model) {
        logger.debug("Loading admin dashboard");
        
        try {
            // Get statistics
            List<TreasureHuntPlan> allPlans = planService.getAllPlans();
            RegistrationService.RegistrationStatistics stats = registrationService.getRegistrationStatistics();
            List<UserRegistration> recentRegistrations = registrationService.getRecentRegistrations();

            model.addAttribute("totalPlans", allPlans.size());
            model.addAttribute("activePlans", allPlans.stream()
                    .filter(p -> p.getStatus() == TreasureHuntPlan.PlanStatus.ACTIVE).count());
            model.addAttribute("totalRegistrations", stats.getTotalRegistrations());
            model.addAttribute("pendingRegistrations", stats.getPendingCount());
            model.addAttribute("confirmedRegistrations", stats.getConfirmedCount());
            model.addAttribute("cancelledRegistrations", stats.getCancelledCount());
            model.addAttribute("recentRegistrations", recentRegistrations);

            return "admin/dashboard";
            
        } catch (Exception e) {
            logger.error("Error loading admin dashboard", e);
            model.addAttribute("error", "Error loading dashboard data");
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
     * Delete plan
     * @param id Plan ID
     * @param redirectAttributes Redirect attributes
     * @return Redirect path
     */
    @PostMapping("/plans/{id}/delete")
    public String deletePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Deleting plan with ID: {}", id);
        
        try {
            planService.deletePlan(id);
            redirectAttributes.addFlashAttribute("success", "Plan deleted successfully");
            logger.info("Successfully deleted plan with ID: {}", id);
            
        } catch (Exception e) {
            logger.error("Error deleting plan with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting plan: " + e.getMessage());
        }

        return "redirect:/admin/plans";
    }

    /**
     * Registration management page
     * @param model Thymeleaf model
     * @param status Optional status filter
     * @return Template name
     */
    @GetMapping("/registrations")
    public String manageRegistrations(Model model, 
                                    @RequestParam(required = false) String status) {
        logger.debug("Loading registration management page with status filter: {}", status);
        
        try {
            List<UserRegistration> registrations;
            
            if (status != null && !status.isEmpty()) {
                UserRegistration.RegistrationStatus statusEnum = 
                        UserRegistration.RegistrationStatus.valueOf(status.toUpperCase());
                registrations = registrationService.getRegistrationsByStatus(statusEnum);
            } else {
                registrations = registrationService.getAllRegistrations();
            }
            
            model.addAttribute("registrations", registrations);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("statuses", UserRegistration.RegistrationStatus.values());
            
            return "admin/registrations";
            
        } catch (Exception e) {
            logger.error("Error loading registration management page", e);
            model.addAttribute("error", "Error loading registrations");
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
            Optional<UserRegistration> registrationOpt = registrationService.getRegistrationById(id);

            if (registrationOpt.isPresent()) {
                UserRegistration registration = registrationOpt.get();
                model.addAttribute("registration", registration);
                model.addAttribute("isTeamRegistration", registration.isTeamRegistration());
                model.addAttribute("teamMembers", registration.getTeamMembers());
                model.addAttribute("documents", registration.getDocuments());

                return "admin/fragments/registration-details :: registrationDetails";
            } else {
                model.addAttribute("error", "Registration not found");
                return "admin/fragments/registration-details :: error";
            }

        } catch (Exception e) {
            logger.error("Error fetching registration details for ID: {}", id, e);
            model.addAttribute("error", "Error loading registration details");
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
        
        logger.debug("Searching registrations by {}: {}", type, query);
        
        try {
            List<UserRegistration> results;
            
            if ("email".equals(type)) {
                results = registrationService.searchByEmail(query);
            } else if ("name".equals(type)) {
                results = registrationService.searchByName(query);
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
                // Send confirmation emails to all participants
                registrationService.sendConfirmationEmails(updatedRegistration);
            } else if (newStatus == UserRegistration.RegistrationStatus.CANCELLED) {
                // Send cancellation email to team leader or individual
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
}
