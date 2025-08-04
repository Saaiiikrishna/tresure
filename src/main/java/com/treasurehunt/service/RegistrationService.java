package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.TeamMemberRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing user registrations
 * Handles registration business logic, validation, and file processing
 */
@Service
@Transactional
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRegistrationRepository registrationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TreasureHuntPlanService planService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final EmailQueueService emailQueueService;
    private final ApplicationIdService applicationIdService;

    @Value("${app.email.support}")
    private String supportEmail;

    @Autowired
    public RegistrationService(UserRegistrationRepository registrationRepository,
                              TeamMemberRepository teamMemberRepository,
                              TreasureHuntPlanService planService,
                              FileStorageService fileStorageService,
                              EmailService emailService,
                              EmailQueueService emailQueueService,
                              ApplicationIdService applicationIdService) {
        this.registrationRepository = registrationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.planService = planService;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.emailQueueService = emailQueueService;
        this.applicationIdService = applicationIdService;
    }

    /**
     * Create a new registration with file uploads
     * @param registration Registration data
     * @param photoFile Passport photo file
     * @param idFile Government ID file
     * @param medicalFile Medical certificate file
     * @return Created registration
     * @throws IOException if file processing fails
     */
    public UserRegistration createRegistration(UserRegistration registration,
                                             MultipartFile photoFile,
                                             MultipartFile idFile,
                                             MultipartFile medicalFile) throws IOException {
        
        logger.info("Creating new registration for plan ID: {} and email: {}", 
                   registration.getPlan().getId(), registration.getEmail());

        // Validate registration data
        validateRegistration(registration);

        // Check if plan is available
        TreasureHuntPlan plan = planService.getAvailablePlanById(registration.getPlan().getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan is not available for registration"));

        // Check for duplicate registration
        Optional<UserRegistration> existingRegistration = registrationRepository
                .findByEmailAndPlan(registration.getEmail(), plan);
        if (existingRegistration.isPresent()) {
            throw new IllegalArgumentException("Email is already registered for this plan");
        }

        // Set plan and save registration
        registration.setPlan(plan);
        registration.setStatus(UserRegistration.RegistrationStatus.PENDING);
        UserRegistration savedRegistration = registrationRepository.save(registration);

        try {
            // Store uploaded files
            if (photoFile != null && !photoFile.isEmpty()) {
                fileStorageService.storeFile(photoFile, savedRegistration, UploadedDocument.DocumentType.PHOTO);
            }
            
            if (idFile != null && !idFile.isEmpty()) {
                fileStorageService.storeFile(idFile, savedRegistration, UploadedDocument.DocumentType.ID_DOCUMENT);
            }
            
            if (medicalFile != null && !medicalFile.isEmpty()) {
                fileStorageService.storeFile(medicalFile, savedRegistration, UploadedDocument.DocumentType.MEDICAL_CERTIFICATE);
            }

            // Queue confirmation emails using the new email queue system
            queueRegistrationConfirmationEmails(savedRegistration);

            // Queue admin notification
            queueAdminNotificationEmail(savedRegistration);

            logger.info("Successfully created registration with ID: {}", savedRegistration.getId());
            return savedRegistration;

        } catch (IOException e) {
            // Rollback: delete registration if file processing fails
            logger.error("Failed to process files for registration ID {}, rolling back: {}",
                        savedRegistration.getId(), e.getMessage());
            try {
                registrationRepository.delete(savedRegistration);
                logger.info("Successfully rolled back registration ID: {}", savedRegistration.getId());
            } catch (Exception rollbackException) {
                logger.error("Failed to rollback registration ID {}: {}",
                           savedRegistration.getId(), rollbackException.getMessage());
            }
            throw new RuntimeException("Registration failed due to file processing error", e);
        } catch (Exception e) {
            // Rollback: delete registration for any other unexpected errors
            logger.error("Unexpected error during registration creation for ID {}, rolling back: {}",
                        savedRegistration.getId(), e.getMessage());
            try {
                registrationRepository.delete(savedRegistration);
                logger.info("Successfully rolled back registration ID: {}", savedRegistration.getId());
            } catch (Exception rollbackException) {
                logger.error("Failed to rollback registration ID {}: {}",
                           savedRegistration.getId(), rollbackException.getMessage());
            }
            throw new RuntimeException("Registration failed due to unexpected error", e);
        }
    }

    /**
     * Get registration by ID
     * @param id Registration ID
     * @return Optional registration
     */
    @Transactional(readOnly = true)
    public Optional<UserRegistration> getRegistrationById(Long id) {
        logger.debug("Fetching registration with ID: {}", id);
        return registrationRepository.findById(id);
    }

    /**
     * Get registration by ID with all related data loaded for admin view
     * @param id Registration ID
     * @return Optional registration with team members and documents
     */
    @Transactional(readOnly = true)
    public Optional<UserRegistration> getRegistrationByIdWithDetails(Long id) {
        logger.debug("Fetching registration with details for ID: {}", id);

        try {
            // First, get the registration with team members
            Optional<UserRegistration> registrationOpt = registrationRepository.findByIdWithTeamMembers(id);

            if (registrationOpt.isPresent()) {
                UserRegistration registration = registrationOpt.get();

                // Force load documents separately to avoid MultipleBagFetchException
                registration.getDocuments().size(); // This triggers lazy loading

                // Sort team members by position if it's a team registration
                if (registration.isTeamRegistration() && registration.getTeamMembers() != null) {
                    registration.getTeamMembers().sort((m1, m2) -> {
                        Integer pos1 = m1.getMemberPosition();
                        Integer pos2 = m2.getMemberPosition();
                        if (pos1 == null) pos1 = Integer.MAX_VALUE;
                        if (pos2 == null) pos2 = Integer.MAX_VALUE;
                        return pos1.compareTo(pos2);
                    });
                }

                logger.debug("Loaded registration {} with {} team members and {} documents",
                            id, registration.getTeamMembers().size(), registration.getDocuments().size());

                return Optional.of(registration);
            }

            logger.warn("Registration not found for ID: {}", id);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error loading registration details for ID: {}", id, e);
            throw new RuntimeException("Failed to load registration details", e);
        }
    }

    /**
     * Get all registrations
     * @return List of all registrations
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getAllRegistrations() {
        logger.debug("Fetching all registrations");
        List<UserRegistration> registrations = registrationRepository.findAll();

        // Force load plan data to avoid lazy loading issues in templates
        for (UserRegistration registration : registrations) {
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers lazy loading
            }
        }

        return registrations;
    }

    /**
     * Get registrations by plan
     * @param plan Treasure hunt plan
     * @return List of registrations for the plan
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByPlan(TreasureHuntPlan plan) {
        logger.debug("Fetching registrations for plan ID: {}", plan.getId());
        List<UserRegistration> registrations = registrationRepository.findByPlanOrderByRegistrationDateDesc(plan);

        // Force load plan data to avoid lazy loading issues in templates
        for (UserRegistration registration : registrations) {
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers lazy loading
            }
        }

        return registrations;
    }

    /**
     * Get registrations by status
     * @param status Registration status
     * @return List of registrations with specified status
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByStatus(UserRegistration.RegistrationStatus status) {
        logger.debug("Fetching registrations with status: {}", status);
        List<UserRegistration> registrations = registrationRepository.findByStatusOrderByRegistrationDateDesc(status);

        // Force load plan data to avoid lazy loading issues in templates
        for (UserRegistration registration : registrations) {
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers lazy loading
            }
        }

        return registrations;
    }

    /**
     * Get registrations by plan ID
     * @param planId Plan ID
     * @return List of registrations for the plan
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByPlan(Long planId) {
        logger.debug("Fetching registrations for plan ID: {}", planId);
        List<UserRegistration> registrations = registrationRepository.findByPlanIdOrderByRegistrationDateDesc(planId);

        // Force load plan data to avoid lazy loading issues in templates
        for (UserRegistration registration : registrations) {
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers lazy loading
            }
        }

        return registrations;
    }

    /**
     * Get registrations by plan ID and status
     * @param planId Plan ID
     * @param status Registration status
     * @return List of registrations matching criteria
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByPlanAndStatus(Long planId, UserRegistration.RegistrationStatus status) {
        logger.debug("Fetching registrations for plan ID: {} with status: {}", planId, status);
        List<UserRegistration> registrations = registrationRepository.findByPlanIdAndStatusOrderByRegistrationDateDesc(planId, status);

        // Force load plan data to avoid lazy loading issues in templates
        for (UserRegistration registration : registrations) {
            if (registration.getPlan() != null) {
                registration.getPlan().getName(); // This triggers lazy loading
            }
        }

        return registrations;
    }

    /**
     * Update registration status
     * @param id Registration ID
     * @param newStatus New status
     * @return Updated registration
     * @throws IllegalArgumentException if registration not found
     */
    public UserRegistration updateRegistrationStatus(Long id, UserRegistration.RegistrationStatus newStatus) {
        logger.info("Updating registration status for ID: {} to {}", id, newStatus);

        UserRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found with ID: " + id));

        UserRegistration.RegistrationStatus oldStatus = registration.getStatus();
        registration.setStatus(newStatus);
        UserRegistration savedRegistration = registrationRepository.save(registration);

        logger.info("Successfully updated registration status for ID: {}", id);
        return savedRegistration;
    }

    /**
     * Cancel registration
     * @param id Registration ID
     * @return Updated registration
     */
    public UserRegistration cancelRegistration(Long id) {
        return updateRegistrationStatus(id, UserRegistration.RegistrationStatus.CANCELLED);
    }

    /**
     * Confirm registration
     * @param id Registration ID
     * @return Updated registration
     */
    public UserRegistration confirmRegistration(Long id) {
        return updateRegistrationStatus(id, UserRegistration.RegistrationStatus.CONFIRMED);
    }

    /**
     * Delete registration and associated files
     * @param id Registration ID
     * @throws IOException if file deletion fails
     */
    public void deleteRegistration(Long id) throws IOException {
        logger.info("Deleting registration with ID: {}", id);
        
        UserRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found with ID: " + id));

        // Delete associated files
        fileStorageService.deleteAllFilesForRegistration(registration);

        // Delete registration
        registrationRepository.delete(registration);
        
        logger.info("Successfully deleted registration with ID: {}", id);
    }

    /**
     * Search registrations by email
     * @param email Email to search for
     * @return List of registrations for the email
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> searchByEmail(String email) {
        logger.debug("Searching registrations by email: {}", email);
        return registrationRepository.findByEmailIgnoreCaseOrderByRegistrationDateDesc(email);
    }

    /**
     * Search registrations by name
     * @param name Name to search for
     * @return List of matching registrations
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> searchByName(String name) {
        logger.debug("Searching registrations by name: {}", name);
        return registrationRepository.findByFullNameContainingIgnoreCaseOrderByRegistrationDateDesc(name);
    }

    /**
     * Get recent registrations (last 30 days)
     * @return List of recent registrations
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRecentRegistrations() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return registrationRepository.findRecentRegistrations(cutoffDate);
    }

    /**
     * Get registration statistics
     * @return Registration statistics
     */
    @Transactional(readOnly = true)
    public RegistrationStatistics getRegistrationStatistics() {
        long totalRegistrations = registrationRepository.countTotalRegistrations();
        long pendingCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.PENDING);
        long confirmedCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.CONFIRMED);
        long cancelledCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.CANCELLED);

        return new RegistrationStatistics(totalRegistrations, pendingCount, confirmedCount, cancelledCount);
    }

    /**
     * Send confirmation emails to all team members or individual participant
     * @param registration Registration to send confirmations for
     */
    @Async
    public void sendConfirmationEmails(UserRegistration registration) {
        logger.info("Sending approval emails for registration ID: {}", registration.getId());

        try {
            // Fetch the registration with team members to avoid lazy loading issues
            UserRegistration fullRegistration = registrationRepository.findById(registration.getId())
                .orElseThrow(() -> new RuntimeException("Registration not found"));

            // Use the approval email queue system when admin confirms registration
            queueApplicationApprovalEmails(fullRegistration);
            logger.info("Queued approval emails for registration ID: {}", registration.getId());

        } catch (Exception e) {
            logger.error("Error sending approval emails for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Send cancellation email to team leader or individual participant
     * @param registration Registration to send cancellation for
     */
    @Async
    @Transactional
    public void sendCancellationEmail(UserRegistration registration) {
        logger.info("Sending cancellation email for registration ID: {}", registration.getId());

        try {
            // Fetch the registration with plan to avoid lazy loading issues
            UserRegistration fullRegistration = registrationRepository.findById(registration.getId())
                .orElseThrow(() -> new RuntimeException("Registration not found"));

            // Force load the plan to avoid lazy initialization
            fullRegistration.getPlan().getName(); // This triggers the lazy loading

            if (fullRegistration.isTeamRegistration()) {
                // For team registrations, queue cancellation email for team leader
                List<TeamMember> teamMembers = teamMemberRepository.findByRegistrationOrderByMemberPosition(fullRegistration);

                TeamMember teamLeader = teamMembers.stream()
                    .filter(TeamMember::isTeamLeader)
                    .findFirst()
                    .orElse(null);

                if (teamLeader != null) {
                    logger.info("Sending team cancellation email to team leader: {}", teamLeader.getEmail());
                    queueTeamCancellationEmail(fullRegistration, teamLeader);
                } else {
                    logger.warn("No team leader found for team registration ID: {}", fullRegistration.getId());
                }
            } else {
                // Queue cancellation email for individual participant
                logger.info("Queuing individual cancellation email to: {}", fullRegistration.getEmail());
                queueIndividualCancellationEmail(fullRegistration);
            }

            logger.info("Successfully queued cancellation email for registration ID: {}", fullRegistration.getId());
        } catch (Exception e) {
            logger.error("Error queuing cancellation email for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Validate registration data
     * @param registration Registration to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRegistration(UserRegistration registration) {
        if (registration.getFullName() == null || registration.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (registration.getAge() == null || registration.getAge() < 18 || registration.getAge() > 65) {
            throw new IllegalArgumentException("Age must be between 18 and 65");
        }

        if (registration.getEmail() == null || registration.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (registration.getPhoneNumber() == null || registration.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (registration.getEmergencyContactName() == null || registration.getEmergencyContactName().trim().isEmpty()) {
            throw new IllegalArgumentException("Emergency contact name is required");
        }

        if (registration.getEmergencyContactPhone() == null || registration.getEmergencyContactPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Emergency contact phone is required");
        }

        if (registration.getMedicalConsentGiven() == null || !registration.getMedicalConsentGiven()) {
            throw new IllegalArgumentException("Medical consent must be given");
        }

        if (registration.getPlan() == null || registration.getPlan().getId() == null) {
            throw new IllegalArgumentException("Plan selection is required");
        }
    }

    /**
     * Inner class for registration statistics
     */
    public static class RegistrationStatistics {
        private final long totalRegistrations;
        private final long pendingCount;
        private final long confirmedCount;
        private final long cancelledCount;

        public RegistrationStatistics(long totalRegistrations, long pendingCount, 
                                    long confirmedCount, long cancelledCount) {
            this.totalRegistrations = totalRegistrations;
            this.pendingCount = pendingCount;
            this.confirmedCount = confirmedCount;
            this.cancelledCount = cancelledCount;
        }

        // Getters
        public long getTotalRegistrations() { return totalRegistrations; }
        public long getPendingCount() { return pendingCount; }
        public long getConfirmedCount() { return confirmedCount; }
        public long getCancelledCount() { return cancelledCount; }
    }

    /**
     * Queue registration confirmation emails using the email queue system
     */
    private void queueRegistrationConfirmationEmails(UserRegistration registration) {
        logger.info("Queuing registration confirmation emails for registration ID: {}", registration.getId());

        try {
            if (registration.isTeamRegistration()) {
                // Queue registration received emails for all team members
                for (TeamMember member : registration.getTeamMembers()) {
                    String subject = "Registration Received for " + registration.getPlan().getName();
                    String body = buildTeamMemberConfirmationEmail(registration, member);

                    emailQueueService.queueRegistrationEmail(
                        createUserRegistrationFromTeamMember(registration, member),
                        subject,
                        body,
                        EmailQueue.EmailType.REGISTRATION_CONFIRMATION
                    );
                }

                logger.info("Queued registration confirmation emails for {} team members", registration.getTeamMembers().size());
            } else {
                // Queue registration received email for individual participant
                String subject = "Registration Received for " + registration.getPlan().getName();
                String body = buildIndividualConfirmationEmail(registration);

                emailQueueService.queueRegistrationEmail(
                    registration,
                    subject,
                    body,
                    EmailQueue.EmailType.REGISTRATION_CONFIRMATION
                );

                logger.info("Queued registration confirmation email for individual registration");
            }
        } catch (Exception e) {
            logger.error("Error queuing confirmation emails for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Queue application approval emails when admin approves registration
     */
    private void queueApplicationApprovalEmails(UserRegistration registration) {
        logger.info("Queuing application approval emails for registration ID: {}", registration.getId());

        try {
            if (registration.isTeamRegistration()) {
                // Queue approval emails for all team members
                for (TeamMember member : registration.getTeamMembers()) {
                    String subject = "Application Approved - " + registration.getPlan().getName() + " Team Participation Confirmed";
                    String body = buildTeamMemberApprovalEmail(registration, member);

                    emailQueueService.queueRegistrationEmail(
                        createUserRegistrationFromTeamMember(registration, member),
                        subject,
                        body,
                        EmailQueue.EmailType.APPLICATION_APPROVAL
                    );
                }

                logger.info("Queued approval emails for {} team members", registration.getTeamMembers().size());
            } else {
                // Queue approval email for individual participant
                String subject = "Application Approved - " + registration.getPlan().getName() + " Participation Confirmed";
                String body = buildApplicationApprovalEmail(registration);

                emailQueueService.queueRegistrationEmail(
                    registration,
                    subject,
                    body,
                    EmailQueue.EmailType.APPLICATION_APPROVAL
                );

                logger.info("Queued application approval email for individual registration");
            }
        } catch (Exception e) {
            logger.error("Error queuing approval emails for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Queue admin notification email
     */
    private void queueAdminNotificationEmail(UserRegistration registration) {
        logger.info("Queuing admin notification email for registration ID: {}", registration.getId());

        try {
            String subject = "📋 New Registration: " +
                (registration.isTeamRegistration() ? registration.getTeamName() : registration.getFullName());
            String body = buildAdminNotificationEmail(registration);

            // Queue admin notification email using configured support email
            emailQueueService.queueEmail(
                getSupportEmail(), // Use configured admin email
                "Treasure Hunt Admin",
                subject,
                body,
                EmailQueue.EmailType.ADMIN_NOTIFICATION
            );

            logger.info("Queued admin notification email");
        } catch (Exception e) {
            logger.error("Error queuing admin notification email for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Queue team cancellation email for team leader
     */
    private void queueTeamCancellationEmail(UserRegistration registration, TeamMember teamLeader) {
        logger.info("Queuing team cancellation email for registration ID: {} to team leader: {}",
                   registration.getId(), teamLeader.getEmail());

        try {
            String subject = "Registration Cancelled - " + registration.getPlan().getName();
            String body = buildTeamCancellationEmail(registration, teamLeader);

            emailQueueService.queueEmail(
                teamLeader.getEmail(),
                teamLeader.getFullName(),
                subject,
                body,
                EmailQueue.EmailType.CANCELLATION_EMAIL
            );

            logger.info("Queued team cancellation email for team leader: {}", teamLeader.getEmail());
        } catch (Exception e) {
            logger.error("Error queuing team cancellation email for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Queue individual cancellation email
     */
    private void queueIndividualCancellationEmail(UserRegistration registration) {
        logger.info("Queuing individual cancellation email for registration ID: {} to: {}",
                   registration.getId(), registration.getEmail());

        try {
            String subject = "Registration Cancelled - " + registration.getPlan().getName();
            String body = buildIndividualCancellationEmail(registration);

            emailQueueService.queueEmail(
                registration.getEmail(),
                registration.getFullName(),
                subject,
                body,
                EmailQueue.EmailType.CANCELLATION_EMAIL
            );

            logger.info("Queued individual cancellation email for: {}", registration.getEmail());
        } catch (Exception e) {
            logger.error("Error queuing individual cancellation email for registration ID: {}", registration.getId(), e);
        }
    }

    /**
     * Build admin notification email content
     */
    private String buildAdminNotificationEmail(UserRegistration registration) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>New Registration</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background: #343a40; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 24px;'>📋 New Registration Alert</h1>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px;'>");
        html.append("<p>A new registration has been submitted:</p>");

        // Registration Summary
        html.append("<div style='background: white; padding: 15px; border-radius: 6px; margin: 15px 0;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>Registration Summary</h3>");
        html.append("<p><strong>Type:</strong> ").append(registration.isTeamRegistration() ? "Team Registration" : "Individual Registration").append("</p>");

        if (registration.isTeamRegistration()) {
            html.append("<p><strong>Team Name:</strong> ").append(registration.getTeamName()).append("</p>");
            html.append("<p><strong>Team Size:</strong> ").append(registration.getTeamMembers().size()).append(" members</p>");
        } else {
            html.append("<p><strong>Participant:</strong> ").append(registration.getFullName()).append("</p>");
            html.append("<p><strong>Email:</strong> ").append(registration.getEmail()).append("</p>");
        }

        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Registration ID:</strong> ").append(registration.getId()).append("</p>");
        html.append("<p><strong>Registration Date:</strong> ").append(registration.getRegistrationDate()).append("</p>");
        html.append("</div>");

        // Action Links
        html.append("<div style='text-align: center; margin: 20px 0;'>");
        html.append("<a href='http://localhost:8080/admin/registrations/").append(registration.getId()).append("' ");
        html.append("style='background: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>");
        html.append("View Registration Details</a>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Build team member approval email content
     */
    private String buildTeamMemberApprovalEmail(UserRegistration registration, TeamMember member) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Team Application Approved</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🎉 Team Application Approved!</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Your Team Participation is Confirmed!</p>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;'>");
        html.append("<h2 style='color: #28a745; margin-top: 0;'>Hello ").append(member.getFullName()).append("!</h2>");
        html.append("<p>Congratulations! Your team application has been approved. You and your team <strong>").append(registration.getTeamName()).append("</strong> are all set for an amazing adventure!</p>");

        // Approval Notice
        html.append("<div style='background: #d4edda; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #155724;'>✅ Team Application Approved!</h3>");
        html.append("<p style='color: #155724; margin: 0;'>Your entire team is confirmed for the treasure hunt!</p>");
        html.append("</div>");

        // Team Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>👥 Team Details</h3>");
        html.append("<p><strong>Team Name:</strong> ").append(registration.getTeamName()).append("</p>");
        html.append("<p><strong>Registration Number:</strong> <span style='background: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-family: monospace;'>")
                   .append(generateRegistrationNumber(registration)).append("</span></p>");
        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Your Role:</strong> ").append(member.isTeamLeader() ? "👑 Team Leader" : "👤 Team Member").append("</p>");
        html.append("<p><strong>Team Size:</strong> ").append(registration.getTeamMembers().size()).append(" members</p>");
        html.append("</div>");

        // Payment Instructions (for team leader)
        if (member.isTeamLeader()) {
            html.append("<div style='background: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ffc107;'>");
            html.append("<h3 style='margin-top: 0; color: #856404;'>💰 Payment Instructions (Team Leader)</h3>");
            html.append("<p style='text-align: center; font-size: 24px; font-weight: bold; color: #856404; margin: 15px 0;'>");
            html.append("₹").append(registration.getPlan().getPriceInr()).append(" per person</p>");
            html.append("<p style='text-align: center; font-size: 18px; font-weight: bold; color: #856404; margin: 15px 0;'>");
            html.append("Total for team: ₹").append(registration.getPlan().getPriceInr().multiply(BigDecimal.valueOf(registration.getTeamMembers().size()))).append("</p>");
            html.append("<div style='background: #f8d7da; padding: 15px; border-radius: 8px; margin: 15px 0; color: #721c24; text-align: center; font-weight: 600;'>");
            html.append("⚠️ Important: This payment is non-refundable once processed.");
            html.append("</div>");
            html.append("<p><strong>Payment Deadline:</strong> Please complete payment within 7 days.</p>");
            html.append("<p><strong>Payment Methods:</strong> UPI, Bank Transfer, or Online Payment (details will be shared via WhatsApp)</p>");
            html.append("</div>");
        } else {
            html.append("<div style='background: #d1ecf1; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #17a2b8;'>");
            html.append("<h3 style='margin-top: 0; color: #0c5460;'>💰 Payment Information</h3>");
            html.append("<p>Your team leader will handle the payment for the entire team.</p>");
            html.append("<p><strong>Cost per person:</strong> ₹").append(registration.getPlan().getPriceInr()).append("</p>");
            html.append("</div>");
        }

        // Communication Guidelines
        html.append("<div style='background: #d1ecf1; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #17a2b8;'>");
        html.append("<h3 style='margin-top: 0; color: #0c5460;'>📱 Communication Guidelines</h3>");
        html.append("<p><strong>WhatsApp Group:</strong> All team members will be added for informal communication</p>");
        html.append("<p><strong>Email:</strong> Official matters will be communicated via email</p>");
        html.append("</div>");

        // Contact Info
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📞 Contact Information</h3>");
        html.append("<p><strong>WhatsApp:</strong> +91 98765 43210</p>");
        html.append("<p><strong>Email:</strong> treasurehunting@gmail.com</p>");
        html.append("</div>");

        html.append("<p>Get ready for an unforgettable team adventure! 🏴‍☠️</p>");
        html.append("<p>Best regards,<br><strong>The Treasure Hunt Adventures Team</strong></p>");
        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Build team member confirmation email content
     */
    private String buildTeamMemberConfirmationEmail(UserRegistration registration, TeamMember member) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Registration Confirmed</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🎉 Registration Confirmed!</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Welcome to the Adventure!</p>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;'>");
        html.append("<h2 style='color: #667eea; margin-top: 0;'>Hello ").append(member.getFullName()).append("!</h2>");
        html.append("<p>Congratulations! Your team registration has been confirmed. Here are your details:</p>");

        // Registration Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📋 Registration Details</h3>");
        html.append("<p><strong>Team Name:</strong> ").append(registration.getTeamName()).append("</p>");
        html.append("<p><strong>Registration Number:</strong> <span style='background: #667eea; color: white; padding: 4px 8px; border-radius: 4px; font-family: monospace;'>")
                   .append(generateRegistrationNumber(registration)).append("</span></p>");
        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Team Size:</strong> ").append(registration.getTeamMembers().size()).append(" members</p>");
        html.append("<p><strong>Registration Date:</strong> ").append(registration.getRegistrationDate().toLocalDate()).append("</p>");
        html.append("</div>");

        // Member Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>👤 Your Details</h3>");
        html.append("<p><strong>Name:</strong> ").append(member.getFullName()).append("</p>");
        html.append("<p><strong>Email:</strong> ").append(member.getEmail()).append("</p>");
        html.append("<p><strong>Phone:</strong> +91 ").append(member.getPhoneNumber()).append("</p>");
        html.append("<p><strong>Position:</strong> ").append(member.isTeamLeader() ? "Team Leader" : "Team Member").append("</p>");
        html.append("</div>");

        // What's Next
        html.append("<div style='background: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ffc107;'>");
        html.append("<h3 style='margin-top: 0; color: #856404;'>📅 What's Next?</h3>");
        html.append("<ul style='margin: 0; padding-left: 20px;'>");
        html.append("<li>All team members will receive this confirmation email</li>");
        html.append("<li>We'll send event details and instructions closer to the date</li>");
        html.append("<li>Please arrive 15 minutes before the scheduled start time</li>");
        html.append("<li>Bring a valid ID and comfortable walking shoes</li>");
        html.append("<li>All team members must be present at the start</li>");
        html.append("</ul>");
        html.append("</div>");

        // Footer
        html.append("<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; margin: 0;'>Questions? Contact us at <a href='mailto:support@treasurehunt.com' style='color: #667eea;'>support@treasurehunt.com</a></p>");
        html.append("<p style='color: #6c757d; margin: 5px 0 0 0; font-size: 14px;'>© 2024 Treasure Hunt Adventures. All rights reserved.</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Build application approval email content
     */
    private String buildApplicationApprovalEmail(UserRegistration registration) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Application Approved</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🎉 Application Approved!</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Your Participation is Confirmed!</p>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;'>");
        html.append("<h2 style='color: #28a745; margin-top: 0;'>Congratulations ").append(registration.getFullName()).append("!</h2>");
        html.append("<p>We're excited to confirm your participation in our treasure hunt adventure! Your application has been reviewed and approved.</p>");

        // Approval Notice
        html.append("<div style='background: #d4edda; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #155724;'>✅ Your Application Has Been Approved!</h3>");
        html.append("<p style='color: #155724; margin: 0;'>You're all set for an amazing experience!</p>");
        html.append("</div>");

        // Plan Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📋 Adventure Details</h3>");
        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Registration Number:</strong> <span style='background: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-family: monospace;'>")
                   .append(generateRegistrationNumber(registration)).append("</span></p>");
        html.append("<p><strong>Duration:</strong> ").append(registration.getPlan().getDurationHours()).append(" hours</p>");
        html.append("<p><strong>Difficulty:</strong> ").append(registration.getPlan().getDifficultyLevel()).append("</p>");
        html.append("</div>");

        // Payment Instructions
        html.append("<div style='background: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ffc107;'>");
        html.append("<h3 style='margin-top: 0; color: #856404;'>💰 Payment Instructions</h3>");
        html.append("<p style='text-align: center; font-size: 24px; font-weight: bold; color: #856404; margin: 15px 0;'>");
        html.append("₹").append(registration.getPlan().getPriceInr()).append(" per person</p>");
        html.append("<div style='background: #f8d7da; padding: 15px; border-radius: 8px; margin: 15px 0; color: #721c24; text-align: center; font-weight: 600;'>");
        html.append("⚠️ Important: This payment is non-refundable once processed.");
        html.append("</div>");
        html.append("<p><strong>Payment Deadline:</strong> Please complete payment within 7 days.</p>");
        html.append("<p><strong>Payment Methods:</strong> UPI, Bank Transfer, or Online Payment (details will be shared via WhatsApp)</p>");
        html.append("</div>");

        // Communication Guidelines
        html.append("<div style='background: #d1ecf1; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #17a2b8;'>");
        html.append("<h3 style='margin-top: 0; color: #0c5460;'>📱 Communication Guidelines</h3>");
        html.append("<p><strong>WhatsApp Group:</strong> You'll be added for informal communication and quick updates</p>");
        html.append("<p><strong>Email:</strong> Official matters will be communicated via email</p>");
        html.append("</div>");

        // Contact Info
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📞 Contact Information</h3>");
        html.append("<p><strong>WhatsApp:</strong> +91 98765 43210</p>");
        html.append("<p><strong>Email:</strong> treasurehunting@gmail.com</p>");
        html.append("</div>");

        html.append("<p>Get ready for an unforgettable adventure! 🏴‍☠️</p>");
        html.append("<p>Best regards,<br><strong>The Treasure Hunt Adventures Team</strong></p>");
        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Build individual confirmation email content
     */
    private String buildIndividualConfirmationEmail(UserRegistration registration) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Registration Confirmed</title></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🎉 Registration Confirmed!</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Welcome to the Adventure!</p>");
        html.append("</div>");

        // Content
        html.append("<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;'>");
        html.append("<h2 style='color: #667eea; margin-top: 0;'>Hello ").append(registration.getFullName()).append("!</h2>");
        html.append("<p>Congratulations! Your registration has been confirmed. Here are your details:</p>");

        // Registration Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>📋 Registration Details</h3>");
        html.append("<p><strong>Registration Number:</strong> <span style='background: #667eea; color: white; padding: 4px 8px; border-radius: 4px; font-family: monospace;'>")
                   .append(generateRegistrationNumber(registration)).append("</span></p>");
        html.append("<p><strong>Plan:</strong> ").append(registration.getPlan().getName()).append("</p>");
        html.append("<p><strong>Registration Date:</strong> ").append(registration.getRegistrationDate().toLocalDate()).append("</p>");
        html.append("</div>");

        // Participant Details
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;'>");
        html.append("<h3 style='margin-top: 0; color: #333;'>👤 Your Details</h3>");
        html.append("<p><strong>Name:</strong> ").append(registration.getFullName()).append("</p>");
        html.append("<p><strong>Email:</strong> ").append(registration.getEmail()).append("</p>");
        html.append("<p><strong>Phone:</strong> +91 ").append(registration.getPhoneNumber()).append("</p>");
        html.append("<p><strong>Age:</strong> ").append(registration.getAge()).append(" years</p>");
        html.append("</div>");

        // What's Next
        html.append("<div style='background: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ffc107;'>");
        html.append("<h3 style='margin-top: 0; color: #856404;'>📅 What's Next?</h3>");
        html.append("<ul style='margin: 0; padding-left: 20px;'>");
        html.append("<li>You will receive event details and instructions closer to the date</li>");
        html.append("<li>Please arrive 15 minutes before the scheduled start time</li>");
        html.append("<li>Bring a valid ID and comfortable walking shoes</li>");
        html.append("<li>Mobile phones will be required for the treasure hunt</li>");
        html.append("</ul>");
        html.append("</div>");

        // Footer
        html.append("<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; margin: 0;'>Questions? Contact us at <a href='mailto:support@treasurehunt.com' style='color: #667eea;'>support@treasurehunt.com</a></p>");
        html.append("<p style='color: #6c757d; margin: 5px 0 0 0; font-size: 14px;'>© 2024 Treasure Hunt Adventures. All rights reserved.</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * Create a UserRegistration object from TeamMember for email purposes
     */
    private UserRegistration createUserRegistrationFromTeamMember(UserRegistration teamRegistration, TeamMember member) {
        UserRegistration memberRegistration = new UserRegistration();
        memberRegistration.setId(teamRegistration.getId());
        memberRegistration.setFullName(member.getFullName());
        memberRegistration.setEmail(member.getEmail());
        memberRegistration.setPhoneNumber(member.getPhoneNumber());
        memberRegistration.setAge(member.getAge());
        // Convert string gender to enum, with fallback to OTHER if invalid
        try {
            memberRegistration.setGender(UserRegistration.Gender.valueOf(member.getGender().toUpperCase()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid gender value '{}' for team member, defaulting to OTHER", member.getGender());
            memberRegistration.setGender(UserRegistration.Gender.OTHER);
        }
        memberRegistration.setTeamName(teamRegistration.getTeamName());
        memberRegistration.setPlan(teamRegistration.getPlan());
        memberRegistration.setRegistrationDate(teamRegistration.getRegistrationDate());
        memberRegistration.setStatus(teamRegistration.getStatus());
        return memberRegistration;
    }

    /**
     * Build team cancellation email body
     */
    private String buildTeamCancellationEmail(UserRegistration registration, TeamMember teamLeader) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(teamLeader.getFullName()).append(",\n\n");
        body.append("We regret to inform you that your team registration for \"")
            .append(registration.getPlan().getName()).append("\" has been cancelled.\n\n");
        body.append("Registration Details:\n");
        body.append("- Team Name: ").append(registration.getTeamName()).append("\n");
        body.append("- Application ID: ").append(generateRegistrationNumber(registration)).append("\n");
        body.append("- Plan: ").append(registration.getPlan().getName()).append("\n");
        body.append("- Registration Date: ").append(registration.getRegistrationDate()).append("\n\n");
        body.append("If you have any questions or concerns, please contact us at ").append(getSupportEmail()).append(".\n\n");
        body.append("Thank you for your interest in Treasure Hunt Adventures.\n\n");
        body.append("Best regards,\n");
        body.append("Treasure Hunt Adventures Team");
        return body.toString();
    }

    /**
     * Build individual cancellation email body
     */
    private String buildIndividualCancellationEmail(UserRegistration registration) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(registration.getFullName()).append(",\n\n");
        body.append("We regret to inform you that your registration for \"")
            .append(registration.getPlan().getName()).append("\" has been cancelled.\n\n");
        body.append("Registration Details:\n");
        body.append("- Application ID: ").append(generateRegistrationNumber(registration)).append("\n");
        body.append("- Plan: ").append(registration.getPlan().getName()).append("\n");
        body.append("- Registration Date: ").append(registration.getRegistrationDate()).append("\n\n");
        body.append("If you have any questions or concerns, please contact us at ").append(getSupportEmail()).append(".\n\n");
        body.append("Thank you for your interest in Treasure Hunt Adventures.\n\n");
        body.append("Best regards,\n");
        body.append("Treasure Hunt Adventures Team");
        return body.toString();
    }

    /**
     * Generate registration number for display purposes
     * Uses the new ApplicationIdService for consistent ID generation
     */
    private String generateRegistrationNumber(UserRegistration registration) {
        try {
            Long planId = registration.getPlan().getId();
            if (registration.isTeamRegistration()) {
                return applicationIdService.generateTeamApplicationId(planId);
            } else {
                return applicationIdService.generateIndividualApplicationId(planId);
            }
        } catch (Exception e) {
            logger.warn("Failed to generate application ID using service, falling back to entity method", e);
            // Fallback to entity's own method
            return registration.getRegistrationNumber();
        }
    }

    /**
     * Get configured support email
     */
    private String getSupportEmail() {
        return supportEmail;
    }
}
