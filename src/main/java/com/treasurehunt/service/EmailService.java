package com.treasurehunt.service;

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

            // Set email properties
            helper.setFrom(fromEmail);
            helper.setTo(registration.getEmail());
            helper.setSubject("Registration Confirmation - " + registration.getPlan().getName());

            // Create email content using Thymeleaf template
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());
            
            // Add pre-hunt checklist
            context.setVariable("checklist", getPreHuntChecklist());

            String htmlContent = templateEngine.process("email/registration-confirmation", context);
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
}
