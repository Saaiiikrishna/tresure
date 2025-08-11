package com.treasurehunt.service;

import com.treasurehunt.config.ApplicationConfigurationManager;
import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.UserRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for generating email content using Thymeleaf templates.
 * Also provides a simple API to send emails using JavaMailSender.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final TemplateEngine templateEngine;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final ApplicationConfigurationManager config;

    private String defaultFromEmail;
    private String companyName;
    private String supportEmail;
    private String baseUrl;

    public EmailService(TemplateEngine templateEngine, ApplicationConfigurationManager config) {
        this.templateEngine = templateEngine;
        this.config = config;
        // Initialize from central configuration
        this.defaultFromEmail = config.getEmail().getFromAddress();
        this.companyName = config.getEmail().getCompanyName();
        this.supportEmail = config.getEmail().getSupportAddress();
        this.baseUrl = "http://localhost:8080"; // Default for development
    }

    /**
     * Generate registration confirmation email HTML body.
     * @param registration User registration
     * @return HTML content for the email
     */
    public String createRegistrationConfirmationBody(UserRegistration registration) {
        logger.debug("Creating registration confirmation email body for: {}", registration.getEmail());
        try {
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());
            context.setVariable("checklist", getPreHuntChecklist());
            context.setVariable("baseUrl", baseUrl);
            return templateEngine.process("email/registration-confirmation", context);
        } catch (Exception e) {
            logger.error("Failed to process registration confirmation email template for {}: {}",
                        registration.getEmail(), e.getMessage(), e);
            return createFallbackRegistrationConfirmationBody(registration);
        }
    }

    /**
     * Generate admin notification email HTML body.
     * @param registration User registration
     * @return HTML content for the email
     */
    public String createAdminNotificationBody(UserRegistration registration) {
        logger.debug("Creating admin notification email body for registration ID: {}", registration.getId());
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);

        // Provide absolute URLs for email links
        context.setVariable("adminRegistrationsUrl", baseUrl + "/admin/registrations");
        context.setVariable("registrationDetailsUrl", baseUrl + "/admin/registrations/" + registration.getId());
        context.setVariable("baseUrl", baseUrl);

        return templateEngine.process("email/admin-notification", context);
    }

    /**
     * Generate application approval email HTML body.
     * @param registration User registration
     * @return HTML content for the email
     */
    public String createApplicationApprovalBody(UserRegistration registration) {
        logger.debug("Creating application approval email body for: {}", registration.getEmail());
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("registrationNumber", registration.getRegistrationNumber());
        return templateEngine.process("email/application-approval", context);
    }

    /**
     * Generate team application approval email HTML body for a specific member.
     * @param registration Team registration
     * @param member The team member to generate the email for
     * @return HTML content for the email
     */
    public String createTeamApplicationApprovalBody(UserRegistration registration, TeamMember member) {
        logger.debug("Creating team application approval email body for member: {}", member.getEmail());
        TeamMember teamLeader = registration.getTeamMembers().stream()
                .filter(m -> m.getMemberPosition() != null && m.getMemberPosition() == 1)
                .findFirst()
                .orElse(null);

        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("member", member);
        context.setVariable("teamLeader", teamLeader);
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        return templateEngine.process("email/team-application-approval", context);
    }

    /**
     * Generate team member confirmation email HTML body.
     * @param registration Team registration
     * @param member The team member to generate the email for
     * @return HTML content for the email
     */
    public String createTeamMemberConfirmationBody(UserRegistration registration, TeamMember member) {
        logger.debug("Creating team member confirmation email body for: {}", member.getEmail());
        try {
            Context context = new Context();
            context.setVariable("registration", registration);
            context.setVariable("member", member);
            context.setVariable("plan", registration.getPlan());
            context.setVariable("companyName", companyName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("registrationNumber", registration.getRegistrationNumber());
            context.setVariable("teamName", registration.getTeamName());
            context.setVariable("isTeamLeader", member.isTeamLeader());
            context.setVariable("checklist", getPreHuntChecklist());
            return templateEngine.process("email/team-member-confirmation", context);
        } catch (Exception e) {
            logger.error("Failed to process team member confirmation email template for {}: {}",
                        member.getEmail(), e.getMessage(), e);
            return createFallbackTeamMemberConfirmationBody(registration, member);
        }
    }

    /**
     * Generate team cancellation email HTML body.
     * @param registration Team registration
     * @param teamLeader The team leader to whom the email is addressed
     * @return HTML content for the email
     */
    public String createTeamCancellationBody(UserRegistration registration, TeamMember teamLeader) {
        logger.debug("Creating team cancellation email body for team leader: {}", teamLeader.getEmail());
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("teamLeader", teamLeader);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("registrationNumber", registration.getRegistrationNumber());
        context.setVariable("teamName", registration.getTeamName());
        context.setVariable("teamMembers", registration.getTeamMembers());
        return templateEngine.process("email/team-cancellation", context);
    }

    /**
     * Generate individual cancellation email HTML body.
     * @param registration Individual registration
     * @return HTML content for the email
     */
    public String createIndividualCancellationBody(UserRegistration registration) {
        logger.debug("Creating individual cancellation email body for: {}", registration.getEmail());
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("registrationNumber", registration.getRegistrationNumber());
        return templateEngine.process("email/individual-cancellation", context);
    }



    /**
     * Send an email using JavaMailSender (HTML enabled).
     * @param to recipient email
     * @param subject subject
     * @param body HTML body
     * @param from optional from address; if null/blank uses configured default
     */
    public void sendEmail(String to, String subject, String body, String from) {
        if (mailSender == null) {
            logger.warn("JavaMailSender not available - skipping actual send for subject: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            String fromAddress = (from != null && !from.isBlank()) ? from : defaultFromEmail;
            helper.setFrom(fromAddress);
            mailSender.send(message);
            logger.debug("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception e) {
            logger.error("Failed to send email to {} with subject '{}': {}", to, subject, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    /**
     * Backward-compatible overload without 'from' argument.
     */
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, null);
    }


    /**
     * Create fallback registration confirmation email body
     */
    private String createFallbackRegistrationConfirmationBody(UserRegistration registration) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Registration Confirmed</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<h2>Registration Confirmed!</h2>");
        html.append("<p>Dear ").append(registration.getFullName()).append(",</p>");
        html.append("<p>Your registration for <strong>").append(registration.getPlan().getName()).append("</strong> has been confirmed.</p>");
        html.append("<p>Registration Number: <strong>").append(registration.getRegistrationNumber()).append("</strong></p>");
        html.append("<p>We will contact you with further details soon.</p>");
        html.append("<p>Best regards,<br>").append(companyName).append("</p>");
        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Create fallback team member confirmation email body
     */
    private String createFallbackTeamMemberConfirmationBody(UserRegistration registration, TeamMember member) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Team Registration Confirmed</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<h2>Team Registration Confirmed!</h2>");
        html.append("<p>Dear ").append(member.getFullName()).append(",</p>");
        html.append("<p>Your team registration for <strong>").append(registration.getPlan().getName()).append("</strong> has been confirmed.</p>");
        html.append("<p>Team Name: <strong>").append(registration.getTeamName()).append("</strong></p>");
        html.append("<p>Registration Number: <strong>").append(registration.getRegistrationNumber()).append("</strong></p>");
        if (member.isTeamLeader()) {
            html.append("<p><strong>You are the team leader.</strong></p>");
        }
        html.append("<p>We will contact you with further details soon.</p>");
        html.append("<p>Best regards,<br>").append(companyName).append("</p>");
        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Get pre-hunt checklist items
     * @return Array of checklist items
     */
    private List<String> getPreHuntChecklist() {
        return Arrays.asList(
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
        );
    }
}
