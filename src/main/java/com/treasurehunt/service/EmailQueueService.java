package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.repository.EmailQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing email queue operations
 */
@Service
@Transactional
public class EmailQueueService {

    private static final Logger logger = LoggerFactory.getLogger(EmailQueueService.class);

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    /**
     * Add email to queue
     */
    public EmailQueue queueEmail(String recipientEmail, String recipientName, String subject, 
                                String body, EmailQueue.EmailType emailType) {
        logger.info("Queuing email to: {} with subject: {}", recipientEmail, subject);
        
        EmailQueue email = new EmailQueue(recipientEmail, recipientName, subject, body, emailType);
        email.setScheduledDate(LocalDateTime.now());
        
        return emailQueueRepository.save(email);
    }

    /**
     * Add email to queue with registration reference
     */
    public EmailQueue queueRegistrationEmail(UserRegistration registration, String subject, 
                                           String body, EmailQueue.EmailType emailType) {
        logger.info("Queuing registration email for registration ID: {}", registration.getId());
        
        EmailQueue email = new EmailQueue(registration.getEmail(), registration.getFullName(), 
                                         subject, body, emailType);
        email.setRegistrationId(registration.getId());
        email.setScheduledDate(LocalDateTime.now());
        
        return emailQueueRepository.save(email);
    }

    /**
     * Queue emails for all team members
     */
    public void queueTeamMemberEmails(UserRegistration registration, List<TeamMember> teamMembers, 
                                     String subject, String body, EmailQueue.EmailType emailType) {
        logger.info("Queuing emails for {} team members", teamMembers.size());
        
        for (TeamMember member : teamMembers) {
            EmailQueue email = new EmailQueue(member.getEmail(), member.getFullName(), 
                                             subject, body, emailType);
            email.setRegistrationId(registration.getId());
            email.setScheduledDate(LocalDateTime.now());
            emailQueueRepository.save(email);
        }
    }

    /**
     * Add email to queue with campaign information
     */
    public EmailQueue queueCampaignEmail(String recipientEmail, String recipientName, String subject, 
                                       String body, String campaignId, String campaignName) {
        logger.info("Queuing campaign email for campaign: {}", campaignName);
        
        EmailQueue email = new EmailQueue(recipientEmail, recipientName, subject, body, 
                                         EmailQueue.EmailType.CAMPAIGN_EMAIL);
        email.setCampaignId(campaignId);
        email.setCampaignName(campaignName);
        email.setScheduledDate(LocalDateTime.now());
        
        return emailQueueRepository.save(email);
    }

    /**
     * Schedule email for future sending
     */
    public EmailQueue scheduleEmail(String recipientEmail, String recipientName, String subject, 
                                  String body, EmailQueue.EmailType emailType, LocalDateTime scheduledDate) {
        logger.info("Scheduling email to: {} for: {}", recipientEmail, scheduledDate);
        
        EmailQueue email = new EmailQueue(recipientEmail, recipientName, subject, body, emailType);
        email.setScheduledDate(scheduledDate);
        email.setStatus(EmailQueue.EmailStatus.SCHEDULED);
        
        return emailQueueRepository.save(email);
    }

    /**
     * Process email queue - scheduled method that runs every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Async
    public void processEmailQueue() {
        logger.debug("Processing email queue...");
        
        List<EmailQueue> emailsToSend = emailQueueRepository.findEmailsReadyToSend(LocalDateTime.now());
        
        if (!emailsToSend.isEmpty()) {
            logger.info("Found {} emails ready to send", emailsToSend.size());
            
            for (EmailQueue email : emailsToSend) {
                try {
                    sendEmail(email);
                } catch (Exception e) {
                    logger.error("Error processing email ID: {}", email.getId(), e);
                }
            }
        }
    }

    /**
     * Send individual email
     */
    @Async
    public void sendEmail(EmailQueue email) {
        logger.info("Sending email ID: {} to: {}", email.getId(), email.getRecipientEmail());
        
        try {
            // Update status to processing
            email.setStatus(EmailQueue.EmailStatus.PROCESSING);
            emailQueueRepository.save(email);
            
            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), true); // true = HTML content
            helper.setFrom(fromEmail);
            
            mailSender.send(message);
            
            // Update status to sent
            email.setStatus(EmailQueue.EmailStatus.SENT);
            email.setSentDate(LocalDateTime.now());
            
            logger.info("Successfully sent email ID: {}", email.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send email ID: {}", email.getId(), e);
            
            // Update status to failed and increment retry count
            email.setStatus(EmailQueue.EmailStatus.FAILED);
            email.setErrorMessage(e.getMessage());
            email.incrementRetryCount();
            
            // If can retry, set back to pending
            if (email.canRetry()) {
                email.setStatus(EmailQueue.EmailStatus.PENDING);
                logger.info("Email ID: {} will be retried. Attempt: {}/{}", 
                           email.getId(), email.getRetryCount(), email.getMaxRetryAttempts());
            }
        } finally {
            emailQueueRepository.save(email);
        }
    }

    /**
     * Retry failed emails
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Async
    public void retryFailedEmails() {
        logger.debug("Checking for failed emails to retry...");
        
        List<EmailQueue> failedEmails = emailQueueRepository.findFailedEmailsForRetry();
        
        if (!failedEmails.isEmpty()) {
            logger.info("Found {} failed emails to retry", failedEmails.size());
            
            for (EmailQueue email : failedEmails) {
                email.setStatus(EmailQueue.EmailStatus.PENDING);
                emailQueueRepository.save(email);
                logger.info("Marked email ID: {} for retry", email.getId());
            }
        }
    }

    /**
     * Get all emails with pagination
     */
    public Page<EmailQueue> getAllEmails(Pageable pageable) {
        return emailQueueRepository.findAll(pageable);
    }

    /**
     * Get emails by status
     */
    public List<EmailQueue> getEmailsByStatus(EmailQueue.EmailStatus status) {
        return emailQueueRepository.findByStatusOrderByCreatedDateDesc(status);
    }

    /**
     * Get emails by status with pagination
     */
    public Page<EmailQueue> getEmailsByStatus(EmailQueue.EmailStatus status, Pageable pageable) {
        return emailQueueRepository.findByStatusOrderByCreatedDateDesc(status, pageable);
    }

    /**
     * Get email by ID
     */
    public EmailQueue getEmailById(Long id) {
        try {
            return emailQueueRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting email by ID: {}", id, e);
            return null;
        }
    }

    /**
     * Get email by ID as Optional
     */
    public Optional<EmailQueue> getEmailByIdOptional(Long id) {
        try {
            return emailQueueRepository.findById(id);
        } catch (Exception e) {
            logger.error("Error getting email by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Retry failed email
     */
    public boolean retryEmail(Long emailId) {
        try {
            Optional<EmailQueue> emailOpt = emailQueueRepository.findById(emailId);
            if (emailOpt.isPresent()) {
                EmailQueue email = emailOpt.get();
                if (email.getStatus() == EmailQueue.EmailStatus.FAILED ||
                    email.getStatus() == EmailQueue.EmailStatus.CANCELLED) {
                    email.setStatus(EmailQueue.EmailStatus.PENDING);
                    email.setErrorMessage(null);
                    email.setScheduledDate(LocalDateTime.now());
                    emailQueueRepository.save(email);
                    logger.info("Email ID {} queued for retry", emailId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error retrying email ID: {}", emailId, e);
            return false;
        }
    }

    /**
     * Cancel email
     */
    public boolean cancelEmail(Long emailId) {
        try {
            Optional<EmailQueue> emailOpt = emailQueueRepository.findById(emailId);
            if (emailOpt.isPresent()) {
                EmailQueue email = emailOpt.get();
                if (email.getStatus() == EmailQueue.EmailStatus.PENDING ||
                    email.getStatus() == EmailQueue.EmailStatus.SCHEDULED) {
                    email.setStatus(EmailQueue.EmailStatus.CANCELLED);
                    emailQueueRepository.save(email);
                    logger.info("Email ID {} cancelled", emailId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error cancelling email ID: {}", emailId, e);
            return false;
        }
    }

    /**
     * Get emails by campaign ID
     */
    public List<EmailQueue> getEmailsByCampaign(String campaignId) {
        return emailQueueRepository.findByCampaignIdOrderByCreatedDateDesc(campaignId);
    }

    /**
     * Get email statistics
     */
    public Map<String, Object> getEmailStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Count by status
            Map<String, Long> statusCounts = new HashMap<>();
            List<Object[]> statusStats = emailQueueRepository.getEmailStatisticsByStatus();
            for (Object[] stat : statusStats) {
                statusCounts.put(stat[0].toString(), (Long) stat[1]);
            }
            stats.put("statusCounts", statusCounts);

            // Count by type
            Map<String, Long> typeCounts = new HashMap<>();
            List<Object[]> typeStats = emailQueueRepository.getEmailStatisticsByType();
            for (Object[] stat : typeStats) {
                typeCounts.put(stat[0].toString(), (Long) stat[1]);
            }
            stats.put("typeCounts", typeCounts);

            // Total counts - using consistent field names expected by templates
            long totalEmails = emailQueueRepository.count();
            long pendingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PENDING);
            long sentEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.SENT);
            long failedEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.FAILED);
            long processingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PROCESSING);
            long cancelledEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.CANCELLED);
            long scheduledEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.SCHEDULED);

            stats.put("totalEmails", totalEmails);
            stats.put("pendingEmails", pendingEmails);
            stats.put("sentEmails", sentEmails);
            stats.put("failedEmails", failedEmails);
            stats.put("processingEmails", processingEmails);
            stats.put("cancelledEmails", cancelledEmails);
            stats.put("scheduledEmails", scheduledEmails);

            // Additional stats for dashboard
            stats.put("totalSent", sentEmails);
            stats.put("pending", pendingEmails);
            stats.put("delivered", sentEmails);
            stats.put("failed", failedEmails);

        } catch (Exception e) {
            logger.error("Error getting email statistics", e);
            // Return default values to prevent template errors
            stats.put("totalEmails", 0L);
            stats.put("pendingEmails", 0L);
            stats.put("sentEmails", 0L);
            stats.put("failedEmails", 0L);
            stats.put("processingEmails", 0L);
            stats.put("cancelledEmails", 0L);
            stats.put("scheduledEmails", 0L);
            stats.put("totalSent", 0L);
            stats.put("pending", 0L);
            stats.put("delivered", 0L);
            stats.put("failed", 0L);
            stats.put("statusCounts", new HashMap<>());
            stats.put("typeCounts", new HashMap<>());
        }

        return stats;
    }



    /**
     * Delete old sent emails (cleanup)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldEmails() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Keep emails for 30 days
        emailQueueRepository.deleteOldSentEmails(cutoffDate);
        logger.info("Cleaned up old sent emails before: {}", cutoffDate);
    }
}
