package com.treasurehunt.service;

import com.treasurehunt.entity.EmailCampaign;
import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.EmailCampaignRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Service for managing email campaigns
 */
@Service
@Transactional
public class EmailCampaignService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCampaignService.class);

    @Autowired
    private EmailCampaignRepository campaignRepository;

    @Autowired
    private UserRegistrationRepository registrationRepository;

    @Autowired
    private EmailQueueService emailQueueService;

    /**
     * Create a new email campaign
     */
    public EmailCampaign createCampaign(EmailCampaign campaign) {
        logger.info("Creating new email campaign: {}", campaign.getName());
        
        // Validate campaign
        validateCampaign(campaign);
        
        // Set default values
        if (campaign.getStatus() == null) {
            campaign.setStatus(EmailCampaign.CampaignStatus.DRAFT);
        }
        
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        logger.info("Successfully created campaign with ID: {}", savedCampaign.getId());
        
        return savedCampaign;
    }

    /**
     * Update an existing campaign
     */
    public EmailCampaign updateCampaign(Long campaignId, EmailCampaign updatedCampaign) {
        logger.info("Updating campaign ID: {}", campaignId);
        
        EmailCampaign existingCampaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        // Check if campaign can be updated
        if (existingCampaign.getStatus() == EmailCampaign.CampaignStatus.SENDING ||
            existingCampaign.getStatus() == EmailCampaign.CampaignStatus.SENT) {
            throw new RuntimeException("Cannot update campaign that is being sent or already sent");
        }
        
        // Update fields
        existingCampaign.setName(updatedCampaign.getName());
        existingCampaign.setDescription(updatedCampaign.getDescription());
        existingCampaign.setSubject(updatedCampaign.getSubject());
        existingCampaign.setBody(updatedCampaign.getBody());
        existingCampaign.setCampaignType(updatedCampaign.getCampaignType());
        existingCampaign.setTargetAudience(updatedCampaign.getTargetAudience());
        existingCampaign.setPriority(updatedCampaign.getPriority());
        
        return campaignRepository.save(existingCampaign);
    }

    /**
     * Send campaign immediately
     */
    @Async
    public void sendCampaign(Long campaignId) {
        logger.info("Starting to send campaign ID: {}", campaignId);
        
        EmailCampaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.canBeSent()) {
            throw new RuntimeException("Campaign cannot be sent in current status: " + campaign.getStatus());
        }
        
        try {
            // Update status to sending
            campaign.setStatus(EmailCampaign.CampaignStatus.SENDING);
            campaignRepository.save(campaign);
            
            // Get recipients based on target audience
            List<UserRegistration> recipients = getRecipients(campaign.getTargetAudience());
            
            if (recipients.isEmpty()) {
                logger.warn("No recipients found for campaign: {}", campaign.getName());
                campaign.setStatus(EmailCampaign.CampaignStatus.FAILED);
                campaignRepository.save(campaign);
                return;
            }
            
            // Generate unique campaign ID for tracking
            String campaignTrackingId = "CAMP-" + campaign.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            // Queue emails for all recipients
            int emailsQueued = 0;
            for (UserRegistration recipient : recipients) {
                try {
                    // Personalize email content
                    String personalizedSubject = personalizeContent(campaign.getSubject(), recipient);
                    String personalizedBody = personalizeContent(campaign.getBody(), recipient);
                    
                    // Queue the email
                    EmailQueue queuedEmail = emailQueueService.queueCampaignEmail(
                        recipient.getEmail(),
                        recipient.getFullName(),
                        personalizedSubject,
                        personalizedBody,
                        campaignTrackingId,
                        campaign.getName()
                    );
                    
                    // Set priority from campaign
                    queuedEmail.setPriority(campaign.getPriority());
                    emailsQueued++;
                    
                } catch (Exception e) {
                    logger.error("Error queuing email for recipient: {}", recipient.getEmail(), e);
                }
            }
            
            // Update campaign statistics
            campaign.setTotalRecipients(recipients.size());
            campaign.setEmailsPending(emailsQueued);
            campaign.setEmailsSent(0);
            campaign.setEmailsFailed(recipients.size() - emailsQueued);
            campaign.setSentDate(LocalDateTime.now());
            campaign.setStatus(EmailCampaign.CampaignStatus.SENT);
            
            campaignRepository.save(campaign);
            
            logger.info("Successfully queued {} emails for campaign: {}", emailsQueued, campaign.getName());
            
        } catch (Exception e) {
            logger.error("Error sending campaign ID: {}", campaignId, e);
            campaign.setStatus(EmailCampaign.CampaignStatus.FAILED);
            campaignRepository.save(campaign);
            throw e;
        }
    }

    /**
     * Schedule campaign for future sending
     */
    public void scheduleCampaign(Long campaignId, LocalDateTime scheduledDate) {
        logger.info("Scheduling campaign ID: {} for: {}", campaignId, scheduledDate);
        
        EmailCampaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.canBeSent()) {
            throw new RuntimeException("Campaign cannot be scheduled in current status: " + campaign.getStatus());
        }
        
        campaign.setScheduledDate(scheduledDate);
        campaign.setStatus(EmailCampaign.CampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);
        
        logger.info("Campaign scheduled successfully");
    }

    /**
     * Process scheduled campaigns - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Async
    public void processScheduledCampaigns() {
        logger.debug("Processing scheduled campaigns...");
        
        List<EmailCampaign> scheduledCampaigns = campaignRepository.findCampaignsReadyToSend(LocalDateTime.now());
        
        if (!scheduledCampaigns.isEmpty()) {
            logger.info("Found {} scheduled campaigns ready to send", scheduledCampaigns.size());
            
            for (EmailCampaign campaign : scheduledCampaigns) {
                try {
                    sendCampaign(campaign.getId());
                } catch (Exception e) {
                    logger.error("Error processing scheduled campaign ID: {}", campaign.getId(), e);
                }
            }
        }
    }

    /**
     * Get recipients based on target audience
     */
    private List<UserRegistration> getRecipients(String targetAudience) {
        if (targetAudience == null || targetAudience.equals("ALL")) {
            return registrationRepository.findAll();
        }
        
        switch (targetAudience) {
            case "INDIVIDUAL_REGISTRATIONS":
                return registrationRepository.findByTeamNameIsNull();
            case "TEAM_REGISTRATIONS":
                return registrationRepository.findByTeamNameIsNotNull();
            case "RECENT_REGISTRATIONS":
                LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
                return registrationRepository.findByRegistrationDateAfter(oneWeekAgo);
            default:
                return registrationRepository.findAll();
        }
    }

    /**
     * Personalize email content with recipient data
     */
    private String personalizeContent(String content, UserRegistration recipient) {
        if (content == null) return "";
        
        return content
            .replace("{{fullName}}", recipient.getFullName())
            .replace("{{email}}", recipient.getEmail())
            .replace("{{teamName}}", recipient.getTeamName() != null ? recipient.getTeamName() : "")
            .replace("{{registrationDate}}", recipient.getRegistrationDate().toLocalDate().toString());
    }

    /**
     * Get all campaigns with pagination
     */
    public Page<EmailCampaign> getAllCampaigns(Pageable pageable) {
        return campaignRepository.findAll(pageable);
    }

    /**
     * Get campaigns by status
     */
    public List<EmailCampaign> getCampaignsByStatus(EmailCampaign.CampaignStatus status) {
        return campaignRepository.findByStatusOrderByCreatedDateDesc(status);
    }

    /**
     * Get campaign statistics
     */
    public Map<String, Object> getCampaignStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        Map<String, Long> statusCounts = new HashMap<>();
        List<Object[]> statusStats = campaignRepository.getCampaignStatisticsByStatus();
        for (Object[] stat : statusStats) {
            statusCounts.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("statusCounts", statusCounts);
        
        // Count by type
        Map<String, Long> typeCounts = new HashMap<>();
        List<Object[]> typeStats = campaignRepository.getCampaignStatisticsByType();
        for (Object[] stat : typeStats) {
            typeCounts.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("typeCounts", typeCounts);
        
        // Total counts
        stats.put("totalCampaigns", campaignRepository.count());
        stats.put("activeCampaigns", campaignRepository.countByStatus(EmailCampaign.CampaignStatus.SENDING));
        stats.put("scheduledCampaigns", campaignRepository.countByStatus(EmailCampaign.CampaignStatus.SCHEDULED));
        stats.put("sentCampaigns", campaignRepository.countByStatus(EmailCampaign.CampaignStatus.SENT));
        
        return stats;
    }

    /**
     * Cancel campaign
     */
    public void cancelCampaign(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (campaign.getStatus() == EmailCampaign.CampaignStatus.DRAFT ||
            campaign.getStatus() == EmailCampaign.CampaignStatus.SCHEDULED) {
            campaign.setStatus(EmailCampaign.CampaignStatus.CANCELLED);
            campaignRepository.save(campaign);
            logger.info("Cancelled campaign ID: {}", campaignId);
        } else {
            throw new RuntimeException("Cannot cancel campaign with status: " + campaign.getStatus());
        }
    }

    /**
     * Delete campaign
     */
    public void deleteCampaign(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (campaign.getStatus() == EmailCampaign.CampaignStatus.SENDING) {
            throw new RuntimeException("Cannot delete campaign that is currently being sent");
        }
        
        campaignRepository.delete(campaign);
        logger.info("Deleted campaign ID: {}", campaignId);
    }

    /**
     * Validate campaign data
     */
    private void validateCampaign(EmailCampaign campaign) {
        if (campaign.getName() == null || campaign.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        
        if (campaign.getSubject() == null || campaign.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign subject is required");
        }
        
        if (campaign.getBody() == null || campaign.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign body is required");
        }
        
        if (campaign.getCampaignType() == null) {
            throw new IllegalArgumentException("Campaign type is required");
        }
        
        if (campaign.getCreatedBy() == null || campaign.getCreatedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign creator is required");
        }
    }
}
