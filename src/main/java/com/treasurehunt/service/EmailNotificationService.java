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

/**
 * Service for handling email notifications with proper transaction boundaries.
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
     * Send approval emails asynchronously.
     * @param registrationId Registration ID
     */
    @Async("emailTaskExecutor")
    @Transactional(readOnly = true)
    public void sendApprovalEmailsAsync(Long registrationId) {
        logger.info("Queuing approval emails for registration: {}", registrationId);
        try {
            UserRegistration registration = registrationRepository.findByIdWithPlan(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));

            // Force initialization of the plan to avoid LazyInitializationException
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers the lazy loading within the transaction
            }

            if (registration.isTeamRegistration()) {
                // Get team members
                UserRegistration regWithMembers = registrationRepository.findByIdWithTeamMembers(registrationId)
                    .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));

                // Queue email for each team member
                for (TeamMember member : regWithMembers.getTeamMembers()) {
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
            logger.info("Successfully queued approval emails for registration: {}", registrationId);
        } catch (Exception e) {
            logger.error("Error queuing approval emails for registration {}: {}", registrationId, e.getMessage(), e);
        }
    }

    /**
     * Send cancellation email asynchronously.
     * @param registrationId Registration ID
     */
    @Async("emailTaskExecutor")
    @Transactional(readOnly = true)
    public void sendCancellationEmailAsync(Long registrationId) {
        logger.info("Queuing cancellation email for registration: {}", registrationId);
        try {
            UserRegistration registration = registrationRepository.findByIdWithPlan(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));

            // Force initialization of the plan to avoid LazyInitializationException
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers the lazy loading within the transaction
            }

            if (registration.isTeamRegistration()) {
                // Get team members
                UserRegistration regWithMembers = registrationRepository.findByIdWithTeamMembers(registrationId)
                    .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));

                // For teams, only the leader gets the cancellation email
                TeamMember teamLeader = regWithMembers.getTeamMembers().stream()
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
                     logger.warn("Could not find team leader for cancellation email for registration {}", registrationId);
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
            logger.info("Successfully queued cancellation email for registration: {}", registrationId);
        } catch (Exception e) {
            logger.error("Error queuing cancellation email for registration {}: {}", registrationId, e.getMessage(), e);
        }
    }
}
