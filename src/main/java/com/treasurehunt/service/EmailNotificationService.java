package com.treasurehunt.service;

import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * Service for handling email notifications with proper transaction boundaries
 * Separates data preparation (transactional) from email sending (async)
 */
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final UserRegistrationRepository registrationRepository;
    private final EmailQueueService emailQueueService;

    @Autowired
    public EmailNotificationService(UserRegistrationRepository registrationRepository,
                                   EmailQueueService emailQueueService) {
        this.registrationRepository = registrationRepository;
        this.emailQueueService = emailQueueService;
    }

    /**
     * Data class for email information (immutable)
     */
    public static class EmailData {
        private final Long registrationId;
        private final String recipientEmail;
        private final String recipientName;
        private final String planName;
        private final boolean isTeamRegistration;
        private final List<TeamMemberData> teamMembers;
        private final String applicationId;

        public EmailData(UserRegistration registration) {
            this.registrationId = registration.getId();
            this.recipientEmail = registration.getEmail();
            this.recipientName = registration.getFullName();
            this.planName = registration.getPlan().getName();
            this.isTeamRegistration = registration.isTeamRegistration();
            this.applicationId = registration.getApplicationId();
            this.teamMembers = registration.getTeamMembers().stream()
                .map(TeamMemberData::new)
                .toList();
        }

        // Getters
        public Long getRegistrationId() { return registrationId; }
        public String getRecipientEmail() { return recipientEmail; }
        public String getRecipientName() { return recipientName; }
        public String getPlanName() { return planName; }
        public boolean isTeamRegistration() { return isTeamRegistration; }
        public List<TeamMemberData> getTeamMembers() { return teamMembers; }
        public String getApplicationId() { return applicationId; }
    }

    /**
     * Data class for team member information (immutable)
     */
    public static class TeamMemberData {
        private final String email;
        private final String fullName;
        private final Integer memberPosition;

        public TeamMemberData(TeamMember teamMember) {
            this.email = teamMember.getEmail();
            this.fullName = teamMember.getFullName();
            this.memberPosition = teamMember.getMemberPosition();
        }

        // Getters
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public Integer getMemberPosition() { return memberPosition; }
    }

    /**
     * Prepare confirmation email data within transaction
     * @param registrationId Registration ID
     * @return Email data for async processing
     */
    @Transactional(readOnly = true)
    public EmailData prepareConfirmationEmailData(Long registrationId) {
        logger.debug("Preparing confirmation email data for registration: {}", registrationId);
        
        UserRegistration registration = registrationRepository.findByIdWithTeamMembers(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));
        
        return new EmailData(registration);
    }

    /**
     * Prepare approval email data within transaction
     * @param registrationId Registration ID
     * @return Email data for async processing
     */
    @Transactional(readOnly = true)
    public EmailData prepareApprovalEmailData(Long registrationId) {
        logger.debug("Preparing approval email data for registration: {}", registrationId);
        
        UserRegistration registration = registrationRepository.findByIdWithTeamMembers(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));
        
        return new EmailData(registration);
    }

    /**
     * Prepare cancellation email data within transaction
     * @param registrationId Registration ID
     * @return Email data for async processing
     */
    @Transactional(readOnly = true)
    public EmailData prepareCancellationEmailData(Long registrationId) {
        logger.debug("Preparing cancellation email data for registration: {}", registrationId);
        
        UserRegistration registration = registrationRepository.findByIdWithTeamMembers(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));
        
        return new EmailData(registration);
    }

    /**
     * Send confirmation emails asynchronously (no transaction)
     * @param emailData Email data prepared in transaction
     */
    @Async("emailTaskExecutor")
    public void sendConfirmationEmailsAsync(EmailData emailData) {
        logger.info("Sending confirmation emails for registration: {}", emailData.getRegistrationId());
        
        try {
            // Queue confirmation email for team leader or individual
            emailQueueService.queueEmail(
                emailData.getRecipientEmail(),
                emailData.getRecipientName(),
                "Registration Confirmation - " + emailData.getPlanName(),
                generateConfirmationEmailBody(emailData),
                com.treasurehunt.entity.EmailQueue.EmailType.REGISTRATION_CONFIRMATION
            );

            // Queue emails for team members if it's a team registration
            if (emailData.isTeamRegistration()) {
                for (TeamMemberData member : emailData.getTeamMembers()) {
                    if (member.getMemberPosition() > 1) { // Skip team leader (position 1)
                        emailQueueService.queueEmail(
                            member.getEmail(),
                            member.getFullName(),
                            "Team Registration Confirmation - " + emailData.getPlanName(),
                            generateTeamMemberConfirmationEmailBody(emailData, member),
                            com.treasurehunt.entity.EmailQueue.EmailType.REGISTRATION_CONFIRMATION
                        );
                    }
                }
            }

            logger.info("Successfully queued confirmation emails for registration: {}", emailData.getRegistrationId());
            
        } catch (Exception e) {
            logger.error("Error sending confirmation emails for registration {}: {}", 
                        emailData.getRegistrationId(), e.getMessage(), e);
        }
    }

    /**
     * Send approval emails asynchronously (no transaction)
     * @param emailData Email data prepared in transaction
     */
    @Async("emailTaskExecutor")
    public void sendApprovalEmailsAsync(EmailData emailData) {
        logger.info("Sending approval emails for registration: {}", emailData.getRegistrationId());
        
        try {
            // Queue approval email for team leader or individual
            emailQueueService.queueEmail(
                emailData.getRecipientEmail(),
                emailData.getRecipientName(),
                "Application Approved - " + emailData.getPlanName(),
                generateApprovalEmailBody(emailData),
                com.treasurehunt.entity.EmailQueue.EmailType.APPLICATION_APPROVAL
            );

            // Queue emails for team members if it's a team registration
            if (emailData.isTeamRegistration()) {
                for (TeamMemberData member : emailData.getTeamMembers()) {
                    if (member.getMemberPosition() > 1) { // Skip team leader (position 1)
                        emailQueueService.queueEmail(
                            member.getEmail(),
                            member.getFullName(),
                            "Team Application Approved - " + emailData.getPlanName(),
                            generateTeamMemberApprovalEmailBody(emailData, member),
                            com.treasurehunt.entity.EmailQueue.EmailType.APPLICATION_APPROVAL
                        );
                    }
                }
            }

            logger.info("Successfully queued approval emails for registration: {}", emailData.getRegistrationId());
            
        } catch (Exception e) {
            logger.error("Error sending approval emails for registration {}: {}", 
                        emailData.getRegistrationId(), e.getMessage(), e);
        }
    }

    /**
     * Send cancellation email asynchronously (no transaction)
     * @param emailData Email data prepared in transaction
     */
    @Async("emailTaskExecutor")
    public void sendCancellationEmailAsync(EmailData emailData) {
        logger.info("Sending cancellation email for registration: {}", emailData.getRegistrationId());
        
        try {
            // Queue cancellation email for team leader or individual
            emailQueueService.queueEmail(
                emailData.getRecipientEmail(),
                emailData.getRecipientName(),
                "Registration Cancelled - " + emailData.getPlanName(),
                generateCancellationEmailBody(emailData),
                com.treasurehunt.entity.EmailQueue.EmailType.CANCELLATION_EMAIL
            );

            logger.info("Successfully queued cancellation email for registration: {}", emailData.getRegistrationId());
            
        } catch (Exception e) {
            logger.error("Error sending cancellation email for registration {}: {}", 
                        emailData.getRegistrationId(), e.getMessage(), e);
        }
    }

    /**
     * Generate confirmation email body
     */
    private String generateConfirmationEmailBody(EmailData emailData) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(emailData.getRecipientName()).append(",\n\n");
        body.append("Thank you for registering for ").append(emailData.getPlanName()).append("!\n\n");
        body.append("Your application ID is: ").append(emailData.getApplicationId()).append("\n\n");
        
        if (emailData.isTeamRegistration()) {
            body.append("Team Members:\n");
            for (TeamMemberData member : emailData.getTeamMembers()) {
                body.append("- ").append(member.getFullName()).append(" (").append(member.getEmail()).append(")\n");
            }
            body.append("\n");
        }
        
        body.append("Your registration is currently under review. You will receive a confirmation email once approved.\n\n");
        body.append("Best regards,\nTreasure Hunt Team");
        
        return body.toString();
    }

    /**
     * Generate team member confirmation email body
     */
    private String generateTeamMemberConfirmationEmailBody(EmailData emailData, TeamMemberData member) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(member.getFullName()).append(",\n\n");
        body.append("You have been registered as a team member for ").append(emailData.getPlanName()).append("!\n\n");
        body.append("Team Leader: ").append(emailData.getRecipientName()).append("\n");
        body.append("Application ID: ").append(emailData.getApplicationId()).append("\n\n");
        body.append("Your registration is currently under review. You will receive a confirmation email once approved.\n\n");
        body.append("Best regards,\nTreasure Hunt Team");
        
        return body.toString();
    }

    /**
     * Generate approval email body
     */
    private String generateApprovalEmailBody(EmailData emailData) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(emailData.getRecipientName()).append(",\n\n");
        body.append("Congratulations! Your application for ").append(emailData.getPlanName()).append(" has been approved!\n\n");
        body.append("Application ID: ").append(emailData.getApplicationId()).append("\n\n");
        body.append("Please keep this email for your records.\n\n");
        body.append("Best regards,\nTreasure Hunt Team");
        
        return body.toString();
    }

    /**
     * Generate team member approval email body
     */
    private String generateTeamMemberApprovalEmailBody(EmailData emailData, TeamMemberData member) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(member.getFullName()).append(",\n\n");
        body.append("Congratulations! Your team application for ").append(emailData.getPlanName()).append(" has been approved!\n\n");
        body.append("Team Leader: ").append(emailData.getRecipientName()).append("\n");
        body.append("Application ID: ").append(emailData.getApplicationId()).append("\n\n");
        body.append("Please keep this email for your records.\n\n");
        body.append("Best regards,\nTreasure Hunt Team");
        
        return body.toString();
    }

    /**
     * Generate cancellation email body
     */
    private String generateCancellationEmailBody(EmailData emailData) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(emailData.getRecipientName()).append(",\n\n");
        body.append("We regret to inform you that your registration for ").append(emailData.getPlanName()).append(" has been cancelled.\n\n");
        body.append("Application ID: ").append(emailData.getApplicationId()).append("\n\n");
        body.append("If you have any questions, please contact our support team.\n\n");
        body.append("Best regards,\nTreasure Hunt Team");
        
        return body.toString();
    }
}
