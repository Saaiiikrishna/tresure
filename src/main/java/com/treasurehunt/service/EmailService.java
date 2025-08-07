package com.treasurehunt.service;

import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.UserRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Service class for generating email content using Thymeleaf templates.
 * This service is responsible for creating the HTML body of emails.
 * The actual sending is handled by other services (e.g., EmailQueueService).
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final TemplateEngine templateEngine;

    @Value("${app.email.company-name}")
    private String companyName;

    @Value("${app.email.support}")
    private String supportEmail;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Generate registration confirmation email HTML body.
     * @param registration User registration
     * @return HTML content for the email
     */
    public String createRegistrationConfirmationBody(UserRegistration registration) {
        logger.debug("Creating registration confirmation email body for: {}", registration.getEmail());
        Context context = new Context();
        context.setVariable("registration", registration);
        context.setVariable("plan", registration.getPlan());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("registrationNumber", registration.getRegistrationNumber());
        context.setVariable("checklist", getPreHuntChecklist());
        return templateEngine.process("email/registration-confirmation", context);
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
