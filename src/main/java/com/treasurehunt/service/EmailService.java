package com.treasurehunt.service;

import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.UserRegistration;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.CompletableFuture;

/**
 * Service class for handling email operations
 * Sends registration confirmations and other notifications
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.support}")
    private String supportEmail;

    @Value("${app.email.company-name}")
    private String companyName;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send registration confirmation email asynchronously
     * @param registration User registration
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendRegistrationConfirmation(UserRegistration registration) {
        try {
            logger.info("Sending registration confirmation email to: {}", registration.getEmail());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties with validation
            String cleanFrom = validateAndCleanEmail(fromEmail);
            String cleanTo = validateAndCleanEmail(registration.getEmail());

            if (cleanFrom == null || cleanTo == null) {
                logger.error("Invalid email addresses - From: {}, To: {}", fromEmail, registration.getEmail());
                return CompletableFuture.failedFuture(new MessagingException("Invalid email addresses"));
            }

            helper.setFrom(cleanFrom);
            helper.setTo(cleanTo);
            helper.setSubject("Registration Received for " + registration.getPlan().getName());

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());

            // Add pre-hunt checklist
            context.setVariable("checklist", getPreHuntChecklist());

            logger.info("=== PROCESSING EMAIL TEMPLATE ===");
            logger.info("Template: email/registration-confirmation");
            logger.info("Registration ID: {}", registration.getId());
            logger.info("Plan: {}", registration.getPlan().getName());

            String htmlContent = templateEngine.process("email/registration-confirmation", context);

            // Log template content preview for debugging
            String preview = htmlContent.length() > 500 ? htmlContent.substring(0, 500) + "..." : htmlContent;
            logger.info("Template content preview: {}", preview);
            logger.info("=== EMAIL TEMPLATE PROCESSING COMPLETE ===");
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            
            logger.info("Successfully sent registration confirmation email to: {}", registration.getEmail());
            return CompletableFuture.completedFuture(null);
            
        } catch (MessagingException e) {
            logger.error("Failed to send registration confirmation email to: {}", registration.getEmail(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send registration status update email
     * @param registration User registration
     * @param newStatus New registration status
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendStatusUpdateEmail(UserRegistration registration, 
                                                        UserRegistration.RegistrationStatus newStatus) {
        try {
            logger.info("Sending status update email to: {} for status: {}", 
                       registration.getEmail(), newStatus);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            
            String subject = getSubjectForStatus(newStatus, registration.getPlan().getName());
            helper.setSubject(subject);

            // Create email content
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("newStatus", newStatus);
            context.setVariable("statusMessage", getStatusMessage(newStatus));

            String htmlContent = templateEngine.process("email/status-update", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            logger.info("Successfully sent status update email to: {}", registration.getEmail());
            return CompletableFuture.completedFuture(null);
            
        } catch (MessagingException e) {
            logger.error("Failed to send status update email to: {}", registration.getEmail(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send admin notification email for new registration
     * @param registration User registration
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendAdminNotification(UserRegistration registration) {
        try {
            logger.info("Sending admin notification for new registration ID: {}", registration.getId());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(supportEmail);
            helper.setSubject("New Registration - " + registration.getPlan().getName());

            // Create admin notification content
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);

            String htmlContent = templateEngine.process("email/admin-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Successfully sent admin notification for registration ID: {}", registration.getId());
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            logger.error("Failed to send admin notification for registration ID: {}", registration.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send application approval email asynchronously
     * @param registration User registration
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendApplicationApproval(UserRegistration registration) {
        try {
            logger.info("Sending application approval email to: {}", registration.getEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Application Approved - " + registration.getPlan().getName() + " Participation Confirmed");

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());

            String htmlContent = templateEngine.process("email/application-approval", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Successfully sent application approval email to: {}", registration.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            logger.error("Failed to send application approval email to: {}", registration.getEmail(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Get pre-hunt checklist items
     * @return Array of checklist items
     */
    private String[] getPreHuntChecklist() {
        return new String[]{
            "Comfortable walking shoes with good grip",
            "Weather-appropriate clothing (layers recommended)",
            "Water bottle (minimum 1 liter)",
            "Small backpack for personal items",
            "Fully charged mobile phone",
            "Government-issued photo ID",
            "Copy of medical certificate",
            "Any personal medications",
            "Sunscreen and hat (for outdoor hunts)",
            "Emergency contact information",
            "Positive attitude and team spirit!"
        };
    }

    /**
     * Get email subject for status update
     * @param status Registration status
     * @param planName Plan name
     * @return Email subject
     */
    private String getSubjectForStatus(UserRegistration.RegistrationStatus status, String planName) {
        switch (status) {
            case CONFIRMED:
                return "Registration Confirmed - " + planName;
            case CANCELLED:
                return "Registration Cancelled - " + planName;
            default:
                return "Registration Update - " + planName;
        }
    }

    /**
     * Get status message for email
     * @param status Registration status
     * @return Status message
     */
    private String getStatusMessage(UserRegistration.RegistrationStatus status) {
        switch (status) {
            case CONFIRMED:
                return "Great news! Your registration has been confirmed. We're excited to have you join us for this adventure!";
            case CANCELLED:
                return "We're sorry to inform you that your registration has been cancelled. If you have any questions, please contact our support team.";
            case PENDING:
                return "Your registration is currently being reviewed. We'll update you once the review is complete.";
            default:
                return "Your registration status has been updated.";
        }
    }

    /**
     * Send confirmation email to individual team member
     * @param registration Team registration
     * @param member Team member to send email to
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendTeamMemberConfirmation(UserRegistration registration, TeamMember member) {
        logger.info("Sending team member confirmation email to: {}", member.getEmail());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(member.getEmail());
            helper.setSubject("Registration Confirmed - " + registration.getPlan().getName());

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("member", member);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());
            context.setVariable("teamName", registration.getTeamName());
            context.setVariable("isTeamLeader", member.isTeamLeader());

            // Add pre-hunt checklist
            context.setVariable("checklist", getPreHuntChecklist());

            String htmlContent = templateEngine.process("email/team-member-confirmation", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Successfully sent team member confirmation email to: {}", member.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            logger.error("Failed to send team member confirmation email to: {}", member.getEmail(), e);
            throw new RuntimeException("Failed to send team member confirmation email", e);
        }
    }

    /**
     * Send cancellation email to team leader
     * @param registration Team registration
     * @param teamLeader Team leader to send email to
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendTeamCancellationEmail(UserRegistration registration, TeamMember teamLeader) {
        logger.info("Sending team cancellation email to team leader: {}", teamLeader.getEmail());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(teamLeader.getEmail());
            helper.setSubject("Team Registration Cancelled - " + registration.getPlan().getName());

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("teamLeader", teamLeader);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());
            context.setVariable("teamName", registration.getTeamName());
            context.setVariable("teamMembers", registration.getTeamMembers());

            String htmlContent = templateEngine.process("email/team-cancellation", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Successfully sent team cancellation email to team leader: {}", teamLeader.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            logger.error("Failed to send team cancellation email to team leader: {}", teamLeader.getEmail(), e);
            throw new RuntimeException("Failed to send team cancellation email", e);
        }
    }

    /**
     * Send individual cancellation email
     * @param registration Individual registration
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> sendCancellationEmail(UserRegistration registration) {
        logger.info("Sending cancellation email to: {}", registration.getEmail());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Registration Cancelled - " + registration.getPlan().getName());

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());

            String htmlContent = templateEngine.process("email/individual-cancellation", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Successfully sent cancellation email to: {}", registration.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            logger.error("Failed to send cancellation email to: {}", registration.getEmail(), e);
            throw new RuntimeException("Failed to send cancellation email", e);
        }
    }

    /**
     * Send email with basic parameters (used by ThreadSafeEmailProcessor)
     * @param to Recipient email
     * @param subject Email subject
     * @param body Email body
     * @param from Sender email
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEmail(String to, String subject, String body, String from) {
        try {
            // Validate and clean email addresses
            String cleanTo = validateAndCleanEmail(to);
            String cleanFrom = validateAndCleanEmail(from != null ? from : fromEmail);

            if (cleanTo == null || cleanFrom == null) {
                logger.error("Invalid email addresses - To: {}, From: {}", to, from);
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(cleanTo);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates HTML content
            helper.setFrom(cleanFrom);

            mailSender.send(message);
            logger.info("Successfully sent email to: {}", to);
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}", to, e);
            return false;
        }
    }

    /**
     * Validate and clean email address to prevent parsing errors
     * @param email Email address to validate
     * @return Cleaned email address or null if invalid
     */
    private String validateAndCleanEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        // Remove any extra whitespace and potential formatting issues
        String cleanEmail = email.trim();

        // Basic email validation regex
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!cleanEmail.matches(emailRegex)) {
            logger.warn("Invalid email format: {}", email);
            return null;
        }

        // Check for common formatting issues that cause "Extra route-addr" errors
        if (cleanEmail.contains("<") || cleanEmail.contains(">") ||
            cleanEmail.contains("\"") || cleanEmail.contains("\\")) {
            logger.warn("Email contains invalid characters: {}", email);
            return null;
        }

        return cleanEmail;
    }

    /**
     * Cleanup method for application shutdown
     */
    @javax.annotation.PreDestroy
    public void cleanup() {
        logger.info("EmailService cleanup initiated");
        // Any cleanup operations if needed (e.g., cancel pending async operations)
        logger.info("EmailService cleanup completed");
    }
}
