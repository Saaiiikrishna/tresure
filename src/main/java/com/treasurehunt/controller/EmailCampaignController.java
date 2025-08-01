package com.treasurehunt.controller;

import com.treasurehunt.entity.EmailCampaign;
import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.service.EmailCampaignService;
import com.treasurehunt.service.EmailQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for email campaign management in admin panel
 */
@Controller
@RequestMapping("/admin/email-campaigns")
public class EmailCampaignController {

    private static final Logger logger = LoggerFactory.getLogger(EmailCampaignController.class);

    @Autowired
    private EmailCampaignService campaignService;

    @Autowired
    private EmailQueueService emailQueueService;

    /**
     * Display email campaigns dashboard
     */
    @GetMapping
    public String emailCampaignsDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        logger.info("Displaying email campaigns dashboard");
        
        try {
            // Create pageable
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Get campaigns
            Page<EmailCampaign> campaigns = campaignService.getAllCampaigns(pageable);
            
            // Get statistics
            Map<String, Object> campaignStats = campaignService.getCampaignStatistics();
            Map<String, Object> emailStats = emailQueueService.getEmailStatistics();
            
            // Add to model
            model.addAttribute("campaigns", campaigns);
            model.addAttribute("campaignStats", campaignStats);
            model.addAttribute("emailStats", emailStats);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", campaigns.getTotalPages());
            model.addAttribute("totalElements", campaigns.getTotalElements());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            
            return "admin/email-campaigns/dashboard";
            
        } catch (Exception e) {
            logger.error("Error loading email campaigns dashboard", e);
            model.addAttribute("error", "Error loading campaigns: " + e.getMessage());
            return "admin/email-campaigns/dashboard";
        }
    }

    /**
     * Display create campaign form
     */
    @GetMapping("/create")
    public String createCampaignForm(Model model) {
        logger.info("Displaying create campaign form");
        
        model.addAttribute("campaign", new EmailCampaign());
        model.addAttribute("campaignTypes", EmailCampaign.CampaignType.values());
        model.addAttribute("targetAudiences", getTargetAudiences());
        
        return "admin/email-campaigns/create";
    }

    /**
     * Create new campaign
     */
    @PostMapping("/create")
    public String createCampaign(@ModelAttribute EmailCampaign campaign,
                                @RequestParam(required = false) String sendNow,
                                @RequestParam(required = false) String schedule,
                                @RequestParam(required = false) String scheduledDateTime,
                                Model model) {
        logger.info("Creating new email campaign: {}", campaign.getName());

        try {
            // Set creator (in real app, get from authentication)
            campaign.setCreatedBy("admin");

            // Create campaign
            EmailCampaign savedCampaign = campaignService.createCampaign(campaign);

            // Handle different submission types
            if ("true".equals(sendNow)) {
                // Send immediately
                campaignService.sendCampaign(savedCampaign.getId());
                logger.info("Campaign sent immediately with ID: {}", savedCampaign.getId());
                return "redirect:/admin/email-campaigns?success=Campaign created and sent successfully";

            } else if ("true".equals(schedule) && scheduledDateTime != null) {
                // Schedule for later
                LocalDateTime scheduledDate = LocalDateTime.parse(scheduledDateTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                campaignService.scheduleCampaign(savedCampaign.getId(), scheduledDate);
                logger.info("Campaign scheduled with ID: {} for: {}", savedCampaign.getId(), scheduledDate);
                return "redirect:/admin/email-campaigns?success=Campaign created and scheduled successfully";

            } else {
                // Save as draft
                logger.info("Campaign saved as draft with ID: {}", savedCampaign.getId());
                return "redirect:/admin/email-campaigns?success=Campaign created as draft successfully";
            }

        } catch (Exception e) {
            logger.error("Error creating campaign", e);
            model.addAttribute("error", "Error creating campaign: " + e.getMessage());
            model.addAttribute("campaign", campaign);
            model.addAttribute("campaignTypes", EmailCampaign.CampaignType.values());
            model.addAttribute("targetAudiences", getTargetAudiences());
            return "admin/email-campaigns/create";
        }
    }

    /**
     * Display campaign details
     */
    @GetMapping("/{id}")
    public String campaignDetails(@PathVariable Long id, Model model) {
        logger.info("Displaying campaign details for ID: {}", id);
        
        try {
            EmailCampaign campaign = campaignService.getAllCampaigns(PageRequest.of(0, 1))
                .getContent().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
            
            // Get emails for this campaign
            List<EmailQueue> campaignEmails = emailQueueService.getEmailsByCampaign("CAMP-" + id + "-*");
            
            model.addAttribute("campaign", campaign);
            model.addAttribute("campaignEmails", campaignEmails);
            
            return "admin/email-campaigns/details";
            
        } catch (Exception e) {
            logger.error("Error loading campaign details for ID: {}", id, e);
            return "redirect:/admin/email-campaigns?error=Campaign not found";
        }
    }

    /**
     * Send campaign immediately
     */
    @PostMapping("/{id}/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendCampaign(@PathVariable Long id) {
        logger.info("Sending campaign ID: {}", id);
        
        try {
            campaignService.sendCampaign(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign is being sent. Check the email queue for progress."
            ));
            
        } catch (Exception e) {
            logger.error("Error sending campaign ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error sending campaign: " + e.getMessage()
            ));
        }
    }

    /**
     * Schedule campaign
     */
    @PostMapping("/{id}/schedule")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> scheduleCampaign(
            @PathVariable Long id,
            @RequestParam String scheduledDateTime) {
        
        logger.info("Scheduling campaign ID: {} for: {}", id, scheduledDateTime);
        
        try {
            LocalDateTime scheduledDate = LocalDateTime.parse(scheduledDateTime, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            
            campaignService.scheduleCampaign(id, scheduledDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign scheduled successfully for " + scheduledDate
            ));
            
        } catch (Exception e) {
            logger.error("Error scheduling campaign ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error scheduling campaign: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancel campaign
     */
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelCampaign(@PathVariable Long id) {
        logger.info("Cancelling campaign ID: {}", id);
        
        try {
            campaignService.cancelCampaign(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign cancelled successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error cancelling campaign ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error cancelling campaign: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete campaign
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteCampaign(@PathVariable Long id) {
        logger.info("Deleting campaign ID: {}", id);
        
        try {
            campaignService.deleteCampaign(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign deleted successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting campaign ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error deleting campaign: " + e.getMessage()
            ));
        }
    }

    /**
     * Display email queue
     */
    @GetMapping("/email-queue")
    public String emailQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        logger.info("Displaying email queue");
        
        try {
            // Create pageable
            Pageable pageable = PageRequest.of(page, size, 
                Sort.by("createdDate").descending());
            
            // Get emails
            Page<EmailQueue> emails = emailQueueService.getAllEmails(pageable);
            
            // Get statistics
            Map<String, Object> emailStats = emailQueueService.getEmailStatistics();
            
            // Add to model
            model.addAttribute("emails", emails);
            model.addAttribute("emailStats", emailStats);
            model.addAttribute("emailStatuses", EmailQueue.EmailStatus.values());
            model.addAttribute("emailTypes", EmailQueue.EmailType.values());
            model.addAttribute("selectedStatus", status);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", emails.getTotalPages());
            model.addAttribute("totalElements", emails.getTotalElements());
            
            return "admin/email-campaigns/email-queue";
            
        } catch (Exception e) {
            logger.error("Error loading email queue", e);
            model.addAttribute("error", "Error loading email queue: " + e.getMessage());
            return "admin/email-campaigns/email-queue";
        }
    }

    /**
     * Get campaign statistics API
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCampaignStatistics() {
        try {
            Map<String, Object> campaignStats = campaignService.getCampaignStatistics();
            Map<String, Object> emailStats = emailQueueService.getEmailStatistics();
            
            return ResponseEntity.ok(Map.of(
                "campaignStats", campaignStats,
                "emailStats", emailStats
            ));
            
        } catch (Exception e) {
            logger.error("Error getting campaign statistics", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error getting statistics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get email queue API
     */
    @GetMapping("/api/email-queue")
    @ResponseBody
    public ResponseEntity<List<EmailQueue>> getEmailQueue(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<EmailQueue> emails;

            if ("ALL".equals(status)) {
                emails = emailQueueService.getAllEmails(pageable);
            } else {
                EmailQueue.EmailStatus emailStatus = EmailQueue.EmailStatus.valueOf(status);
                emails = emailQueueService.getEmailsByStatus(emailStatus, pageable);
            }

            return ResponseEntity.ok(emails.getContent());

        } catch (Exception e) {
            logger.error("Error getting email queue via API", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get email details API
     */
    @GetMapping("/api/email/{id}")
    @ResponseBody
    public ResponseEntity<EmailQueue> getEmailDetails(@PathVariable Long id) {
        try {
            Optional<EmailQueue> email = emailQueueService.getEmailByIdOptional(id);
            if (email.isPresent()) {
                return ResponseEntity.ok(email.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting email details for ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retry email API
     */
    @PostMapping("/api/email/{id}/retry")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> retryEmail(@PathVariable Long id) {
        try {
            boolean success = emailQueueService.retryEmail(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Email queued for retry" : "Failed to retry email");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrying email ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error retrying email: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancel email API
     */
    @PostMapping("/api/email/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelEmail(@PathVariable Long id) {
        try {
            boolean success = emailQueueService.cancelEmail(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Email cancelled successfully" : "Failed to cancel email");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error cancelling email ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error cancelling email: " + e.getMessage()
            ));
        }
    }









    /**
     * Get target audience options
     */
    private String[] getTargetAudiences() {
        return new String[]{
            "ALL",
            "INDIVIDUAL_REGISTRATIONS",
            "TEAM_REGISTRATIONS",
            "RECENT_REGISTRATIONS"
        };
    }
}
