package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.TeamMember;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling email notifications with proper transaction boundaries.
 * This service prepares the necessary data in a transaction and then queues the email
 * for asynchronous sending by the EmailQueueService.
 */
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final UserRegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final EmailQueueService emailQueueService;

    @Autowired
    public EmailNotificationService(UserRegistrationRepository registrationRepository,
                                   EmailService emailService,
                                   EmailQueueService emailQueueService) {
        this.registrationRepository = registrationRepository;
        this.emailService = emailService;
        this.emailQueueService = emailQueueService;
    }

    /**
     * Data class for email information, created within a transaction.
     */
    public static class EmailData {
        private final Long registrationId;
        // Add other fields as needed if you want to pass more detached data

        public EmailData(UserRegistration registration) {
            this.registrationId = registration.getId();
        }

        public Long getRegistrationId() { return registrationId; }
    }

    /**
     * Prepare email data within a transaction. This can be a generic method.
     * @param registrationId Registration ID
     * @return EmailData containing the ID.
     */
    @Transactional(readOnly = true)
    public EmailData prepareEmailData(Long registrationId) {
        logger.debug("Preparing email data for registration: {}", registrationId);
        UserRegistration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));
        return new EmailData(registration);
    }


    /**
     * Backward-compatibility alias for older callers expecting prepareApprovalEmailData
     */
    @Transactional(readOnly = true)
    public EmailData prepareApprovalEmailData(Long registrationId) {
        return prepareEmailData(registrationId);
    }

    /**
     * Send approval emails asynchronously.
     * @param emailData Data prepared in a transaction
     */
    @Async("emailTaskExecutor")
    public void sendApprovalEmailsAsync(EmailData emailData) {
        logger.info("Queuing approval emails for registration: {}", emailData.getRegistrationId());
        try {
            UserRegistration registration = registrationRepository.findByIdWithTeamMembers(emailData.getRegistrationId())
                .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + emailData.getRegistrationId()));

            if (registration.isTeamRegistration()) {
                // Queue email for each team member
                for (TeamMember member : registration.getTeamMembers()) {
                    String body = emailService.createTeamApplicationApprovalBody(registration, member);
                    emailQueueService.queueEmail(
                        member.getEmail(),
                        member.getFullName(),
                        "Team Application Approved - " + registration.getPlan().getName(),
                        body,
                        EmailQueue.EmailType.APPLICATION_APPROVAL
                    );
                }
            } else {
                // Queue email for individual participant
                String body = emailService.createApplicationApprovalBody(registration);
                emailQueueService.queueEmail(
                    registration.getEmail(),
                    registration.getFullName(),
                    "Application Approved - " + registration.getPlan().getName(),
                    body,
                    EmailQueue.EmailType.APPLICATION_APPROVAL
                );
            }
            logger.info("Successfully queued approval emails for registration: {}", emailData.getRegistrationId());
        } catch (Exception e) {
            logger.error("Error queuing approval emails for registration {}: {}",
                        emailData.getRegistrationId(), e.getMessage(), e);
        }
    }

    /**
     * Send cancellation email asynchronously.
     * @param emailData Data prepared in a transaction
     */
    @Async("emailTaskExecutor")
    public void sendCancellationEmailAsync(EmailData emailData) {
        logger.info("Queuing cancellation email for registration: {}", emailData.getRegistrationId());
        try {
            UserRegistration registration = registrationRepository.findByIdWithTeamMembers(emailData.getRegistrationId())
                .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + emailData.getRegistrationId()));

            if (registration.isTeamRegistration()) {
                // For teams, only the leader gets the cancellation email
                TeamMember teamLeader = registration.getTeamMembers().stream()
                    .filter(m -> m.getMemberPosition() != null && m.getMemberPosition() == 1)
                    .findFirst()
                    .orElse(null);

                if (teamLeader != null) {
                    String body = emailService.createTeamCancellationBody(registration, teamLeader);
                    emailQueueService.queueEmail(
                        teamLeader.getEmail(),
                        teamLeader.getFullName(),
                        "Team Registration Cancelled - " + registration.getPlan().getName(),
                        body,
                        EmailQueue.EmailType.CANCELLATION_EMAIL
                    );
                } else {
                     logger.warn("Could not find team leader for cancellation email for registration {}", registration.getId());
                }
            } else {
                // Queue email for individual participant
                String body = emailService.createIndividualCancellationBody(registration);
                emailQueueService.queueEmail(
                    registration.getEmail(),
                    registration.getFullName(),
                    "Registration Cancelled - " + registration.getPlan().getName(),
                    body,
                    EmailQueue.EmailType.CANCELLATION_EMAIL
                );
            }
            logger.info("Successfully queued cancellation email for registration: {}", emailData.getRegistrationId());
        } catch (Exception e) {
            logger.error("Error queuing cancellation email for registration {}: {}",
                        emailData.getRegistrationId(), e.getMessage(), e);
        }
    }
}
