package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.repository.EmailQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.company-name}")
    private String companyName;

    @Value("${app.email.support}")
    private String supportEmail;

    /**
     * Add email to queue
     */
    public EmailQueue queueEmail(String recipientEmail, String recipientName, String subject,
                                String body, EmailQueue.EmailType emailType) {
        logger.debug("Queuing {} email to: {}", emailType, recipientEmail);

        EmailQueue email = new EmailQueue(recipientEmail, recipientName, subject, body, emailType);
        email.setScheduledDate(LocalDateTime.now());

        return emailQueueRepository.save(email);
    }

    /**
     * Add email to queue with registration reference
     */
    public EmailQueue queueRegistrationEmail(UserRegistration registration, String subject,
                                           String body, EmailQueue.EmailType emailType) {
        logger.debug("Queuing {} email for registration ID: {}", emailType, registration.getId());

        // If body is null, generate it using EmailService template
        String emailBody = body;
        if (emailBody == null || emailBody.trim().isEmpty()) {
            logger.debug("Generating email body using template for registration ID: {}", registration.getId());
            try {
                emailBody = generateRegistrationEmailBody(registration);
            } catch (Exception e) {
                logger.error("Error generating email body for registration ID: {}, using fallback", registration.getId(), e);
                emailBody = generateFallbackEmailBody(registration);
            }
        }

        EmailQueue email = new EmailQueue(registration.getEmail(), registration.getFullName(),
                                         subject, emailBody, emailType);
        email.setRegistrationId(registration.getId());
        email.setScheduledDate(LocalDateTime.now());

        return emailQueueRepository.save(email);
    }

    /**
     * Queue emails for all team members
     */
    public void queueTeamMemberEmails(UserRegistration registration, List<TeamMember> teamMembers,
                                     String subject, String body, EmailQueue.EmailType emailType) {
        logger.debug("Queuing {} emails for {} team members", emailType, teamMembers.size());

        for (TeamMember member : teamMembers) {
            EmailQueue email = new EmailQueue(member.getEmail(), member.getFullName(),
                                             subject, body, emailType);
            email.setRegistrationId(registration.getId());
            email.setScheduledDate(LocalDateTime.now());
            emailQueueRepository.save(email);
        }

        logger.info("Queued {} team member emails for registration {}", teamMembers.size(), registration.getId());
    }

    /**
     * Add email to queue with campaign information
     */
    public EmailQueue queueCampaignEmail(String recipientEmail, String recipientName, String subject,
                                       String body, String campaignId, String campaignName) {
        logger.debug("Queuing campaign email for campaign: {}", campaignName);

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
        logger.debug("Scheduling {} email to: {} for: {}", emailType, recipientEmail, scheduledDate);

        EmailQueue email = new EmailQueue(recipientEmail, recipientName, subject, body, emailType);
        email.setScheduledDate(scheduledDate);
        email.setStatus(EmailQueue.EmailStatus.SCHEDULED);

        return emailQueueRepository.save(email);
    }

    /**
     * Process email queue - DISABLED to prevent conflicts with ThreadSafeEmailProcessor
     * Email processing is now handled by ThreadSafeEmailProcessor.processEmailQueue()
     */
    // @Scheduled(fixedRate = 60000) // DISABLED - using ThreadSafeEmailProcessor instead
    // @Async
    public void processEmailQueueLegacy() {
        logger.debug("Legacy email queue processing method - DISABLED");
        // This method is disabled to prevent conflicts with ThreadSafeEmailProcessor
        // All email processing is now handled by ThreadSafeEmailProcessor
    }

    /**
     * Send individual email - DEPRECATED, use ThreadSafeEmailProcessor instead
     */
    @Transactional
    public void sendEmail(EmailQueue email) {
        logger.debug("Sending email ID: {} to: {} (via legacy method)", email.getId(), email.getRecipientEmail());
        
        try {
            // Update status to processing
            email.setStatus(EmailQueue.EmailStatus.PROCESSING);
            emailQueueRepository.save(email);
            
            // Create and send email (if mail sender is available)
            if (mailSender == null) {
                logger.warn("JavaMailSender not available - email will be marked as failed: {}", email.getSubject());
                email.setStatus(EmailQueue.EmailStatus.FAILED);
                email.setErrorMessage("JavaMailSender not configured");
                emailQueueRepository.save(email);
                return CompletableFuture.completedFuture(false);
            }

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

    /**
     * Generate registration email body using Thymeleaf template
     */
    private String generateRegistrationEmailBody(UserRegistration registration) {
        logger.info("Generating registration email body using Thymeleaf template for registration ID: {}", registration.getId());

        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("registrationNumber", registration.getRegistrationNumber());

        // Add pre-hunt checklist
        context.setVariable("checklist", getPreHuntChecklist());

        String htmlContent = templateEngine.process("email/registration-confirmation", context);
        logger.info("Successfully generated registration email body using Thymeleaf template");

        return htmlContent;
    }

    /**
     * Generate fallback email body if template processing fails
     */
    private String generateFallbackEmailBody(UserRegistration registration) {
        logger.info("Generating fallback email body for registration ID: {}", registration.getId());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Registration Confirmed</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header with corporate colors
        html.append("<div style='background: linear-gradient(135deg, #2c5aa0 0%, #3182ce 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🎉 Registration Confirmed!</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Welcome to the Adventure!</p>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;'>");
        html.append("<h2 style='color: #2c5aa0; margin-top: 0;'>Hello ").append(registration.getFullName()).append("!</h2>");
        html.append("<p>Congratulations! Your registration has been confirmed. Here are your details:</p>");

        // Registration Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #2c5aa0;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📋 Registration Details</h3>");
        html.append("<p><strong>Registration Number:</strong> <span style='background: #2c5aa0; color: white; padding: 4px 8px; border-radius: 4px; font-family: monospace;'>")
                   .append(registration.getRegistrationNumber()).append("</span></p>");
        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Registration Date:</strong> ").append(registration.getRegistrationDate().toLocalDate()).append("</p>");
        html.append("</div>");

        // Footer
        html.append("<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; margin: 0;'>Questions? Contact us at <a href='mailto:").append(supportEmail).append("' style='color: #2c5aa0;'>").append(supportEmail).append("</a></p>");
        html.append("<p style='color: #6c757d; margin: 5px 0 0 0; font-size: 14px;'>© 2024 ").append(companyName).append(". All rights reserved.</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Get pre-hunt checklist items
     */
    private List<String> getPreHuntChecklist() {
        return Arrays.asList(
            "Bring a fully charged mobile phone",
            "Wear comfortable walking shoes",
            "Carry a valid photo ID",
            "Arrive 15 minutes before start time",
            "Bring a water bottle",
            "Follow all safety instructions"
        );
    }
}
