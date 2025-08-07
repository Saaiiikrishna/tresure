package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.treasurehunt.repository.UserRegistrationRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique application IDs for treasure hunt registrations.
 * The generated IDs follow the format TH-TYPE-PPPPSS where:
 *  - TYPE is IND or TEAM
 *  - PPPP is the 4 digit plan id
 *  - SS   is a 2 digit sequence number for that plan
 */
@Service
public class ApplicationIdService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationIdService.class);

    @Autowired
    private UserRegistrationRepository registrationRepository;

    // Thread-safe counters for each plan (Plan ID -> Sequence Counter)
    private final ConcurrentHashMap<Long, AtomicLong> planSequenceCounters = new ConcurrentHashMap<>();
    
    // Base prefix for all treasure hunt registrations
    private static final String BASE_PREFIX = "TH";
    
    /**
     * Generate a unique application ID for individual registration
     * Format: TH-IND-000101 (Plan 1, 1st application)
     *
     * @param planId Plan ID
     * @return Formatted application ID
     */
    public String generateIndividualApplicationId(Long planId) {
        return generateApplicationId(planId, "IND");
    }

    /**
     * Generate a unique application ID for team registration
     * Format: TH-TEAM-000101 (Plan 1, 1st application)
     *
     * @param planId Plan ID
     * @return Formatted application ID
     */
    public String generateTeamApplicationId(Long planId) {
        return generateApplicationId(planId, "TEAM");
    }
    
    /**
     * Generate a unique application ID with custom type
     * Format: TH-TYPE-PPPPSS where PPPP=Plan ID (4 digits) and SS=sequence (2 digits)
     *
     * @param planId Plan ID
     * @param type Registration type (IND, TEAM, etc.)
     * @return Formatted application ID
     */
    public String generateApplicationId(Long planId, String type) {
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration type cannot be null or empty");
        }

        try {
            // Get next sequence number for this plan (thread-safe)
            AtomicLong planCounter = planSequenceCounters.computeIfAbsent(planId, k -> {
                long existingCount = getExistingRegistrationCountForPlan(planId);
                return new AtomicLong(existingCount + 1);
            });

            long sequenceNumber = planCounter.getAndIncrement();

            String planPart = String.format("%04d", planId);
            String sequencePart = String.format("%02d", sequenceNumber);

            String applicationId = String.format("%s-%s-%s%s",
                BASE_PREFIX, type.toUpperCase(), planPart, sequencePart);

            logger.debug("Generated application ID: {} for plan ID: {}, sequence: {}",
                applicationId, planId, sequenceNumber);

            return applicationId;

        } catch (Exception e) {
            logger.error("Error generating application ID for plan ID: {}", planId, e);
            long fallbackSequence = System.currentTimeMillis() % 100; // Last 2 digits of timestamp
            String planPart = String.format("%04d", planId);
            String sequencePart = String.format("%02d", fallbackSequence);
            return String.format("%s-%s-%s%s", BASE_PREFIX, type.toUpperCase(), planPart, sequencePart);
        }
    }

    /**
     * Get existing registration count for a plan (for initializing sequence counter)
     *
     * @param planId Plan ID
     * @return Number of existing registrations for this plan
     */
    private long getExistingRegistrationCountForPlan(Long planId) {
        try {
            return registrationRepository.countByPlanId(planId);
        } catch (Exception e) {
            logger.warn("Could not get existing registration count for plan {}, starting from 0", planId);
            return 0;
        }
    }
    
    /**
     * Generate a unique campaign tracking ID
     * Format: CAMP-2408-001234
     * 
     * @param campaignId Database ID of the campaign
     * @return Formatted campaign tracking ID
     */
    public String generateCampaignTrackingId(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID cannot be null");
        }
        
        try {
            String sequence = String.format("%06d", campaignId);
            String trackingId = String.format("CAMP-%s", sequence);
            logger.debug("Generated campaign tracking ID: {} for campaign ID: {}", trackingId, campaignId);
            return trackingId;
        } catch (Exception e) {
            logger.error("Error generating campaign tracking ID for campaign ID: {}", campaignId, e);
            return String.format("CAMP-%06d", campaignId);
        }
    }
    
    /**
     * Generate a unique document reference ID
     * Format: DOC-2408-001234
     * 
     * @param documentId Database ID of the document
     * @return Formatted document reference ID
     */
    public String generateDocumentReferenceId(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        
        try {
            String sequence = String.format("%06d", documentId);
            String referenceId = String.format("DOC-%s", sequence);
            logger.debug("Generated document reference ID: {} for document ID: {}", referenceId, documentId);
            return referenceId;
        } catch (Exception e) {
            logger.error("Error generating document reference ID for document ID: {}", documentId, e);
            return String.format("DOC-%06d", documentId);
        }
    }
    
    /**
     * Parse application ID to extract registration ID
     * 
     * @param applicationId Application ID to parse
     * @return Registration ID or null if parsing fails
     */
    public Long parseRegistrationId(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Expected format: TH-TYPE-PPPPSS
            String[] parts = applicationId.split("\\-");
            if (parts.length == 3) {
                String sequencePart = parts[2];
                return Long.parseLong(sequencePart);
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse registration ID from application ID: {}", applicationId);
        }
        
        return null;
    }
    
    /**
     * Validate application ID format
     * 
     * @param applicationId Application ID to validate
     * @return true if format is valid, false otherwise
     */
    public boolean isValidApplicationId(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return false;
        }
        
        // Basic format validation: TH-TYPE-PPPPSS
        return applicationId.matches("^TH-(IND|TEAM)-\\d{6}$");
    }
    
    /**
     * Get next sequence number for a specific plan (for testing purposes)
     *
     * @param planId Plan ID
     * @return Next sequence number for the plan
     */
    public long getNextSequenceNumber(Long planId) {
        if (planId == null) {
            return 1;
        }

        AtomicLong planCounter = planSequenceCounters.computeIfAbsent(planId, k -> {
            long existingCount = getExistingRegistrationCountForPlan(planId);
            return new AtomicLong(existingCount + 1);
        });

        return planCounter.getAndIncrement();
    }

    /**
     * Reset sequence counter for a specific plan (for testing purposes)
     *
     * @param planId Plan ID to reset counter for
     */
    public void resetSequenceCounter(Long planId) {
        if (planId != null) {
            planSequenceCounters.put(planId, new AtomicLong(1));
            logger.info("Application ID sequence counter reset to 1 for plan ID: {}", planId);
        }
    }

    /**
     * Reset all sequence counters (for testing purposes)
     */
    public void resetAllSequenceCounters() {
        planSequenceCounters.clear();
        logger.info("All application ID sequence counters reset");
    }
}
