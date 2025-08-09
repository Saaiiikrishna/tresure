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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final EmailNotificationService emailNotificationService;
    private final ApplicationIdService applicationIdService;
    private final InputSanitizationService inputSanitizationService;

    @Value("${app.email.support}")
    private String supportEmail;

    @Autowired
    public RegistrationService(UserRegistrationRepository registrationRepository,
                              TeamMemberRepository teamMemberRepository,
                              TreasureHuntPlanService planService,
                              FileStorageService fileStorageService,
                              EmailService emailService,
                              EmailQueueService emailQueueService,
                              EmailNotificationService emailNotificationService,
                              ApplicationIdService applicationIdService,
                              InputSanitizationService inputSanitizationService) {
        this.registrationRepository = registrationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.planService = planService;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.emailQueueService = emailQueueService;
        this.emailNotificationService = emailNotificationService;
        this.applicationIdService = applicationIdService;
        this.inputSanitizationService = inputSanitizationService;
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

        // Generate application ID before persisting
        String applicationId = registration.isTeamRegistration()
                ? applicationIdService.generateTeamApplicationId(plan.getId())
                : applicationIdService.generateIndividualApplicationId(plan.getId());
        registration.setApplicationId(applicationId);

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

            // Queue confirmation emails using the new unified approach
            queueConfirmationEmails(savedRegistration);

            // Queue admin notification
            queueAdminNotificationEmail(savedRegistration);

            logger.info("Successfully created registration with ID: {}", savedRegistration.getId());
            return savedRegistration;

        } catch (IOException e) {
            // Rollback: delete registration if file processing fails
            logger.error("Failed to process files for registration ID {}, rolling back: {}",
                        savedRegistration.getId(), e.getMessage());
            performRegistrationRollback(savedRegistration);
            throw new RuntimeException("Registration failed due to file processing error", e);
        } catch (Exception e) {
            // Rollback: delete registration for any other unexpected errors
            logger.error("Unexpected error during registration creation for ID {}, rolling back: {}",
                        savedRegistration.getId(), e.getMessage());
            performRegistrationRollback(savedRegistration);
            throw new RuntimeException("Registration failed due to unexpected error", e);
        }
    }

    private void queueConfirmationEmails(UserRegistration registration) {
        try {
            if (registration.isTeamRegistration()) {
                logger.debug("Queueing team member confirmation emails for registration ID: {}", registration.getId());
                for (TeamMember member : registration.getTeamMembers()) {
                    try {
                        String body = emailService.createTeamMemberConfirmationBody(registration, member);
                        emailQueueService.queueEmail(member.getEmail(), member.getFullName(), "Registration Confirmed - " + registration.getPlan().getName(), body, EmailQueue.EmailType.REGISTRATION_CONFIRMATION);
                        logger.debug("Queued team member confirmation email for: {}", member.getEmail());
                    } catch (Exception e) {
                        logger.error("Failed to queue team member confirmation email for {}: {}", member.getEmail(), e.getMessage(), e);
                        throw e;
                    }
                }
            } else {
                logger.debug("Queueing individual confirmation email for registration ID: {}", registration.getId());
                try {
                    String body = emailService.createRegistrationConfirmationBody(registration);
                    emailQueueService.queueEmail(registration.getEmail(), registration.getFullName(), "Registration Received for " + registration.getPlan().getName(), body, EmailQueue.EmailType.REGISTRATION_CONFIRMATION);
                    logger.debug("Queued individual confirmation email for: {}", registration.getEmail());
                } catch (Exception e) {
                    logger.error("Failed to create or queue individual confirmation email for {}: {}", registration.getEmail(), e.getMessage(), e);
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to queue confirmation emails for registration ID {}: {}", registration.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void queueAdminNotificationEmail(UserRegistration registration) {
        try {
            logger.debug("Queueing admin notification email for registration ID: {}", registration.getId());
            String body = emailService.createAdminNotificationBody(registration);
            String subject = "📋 New Registration: " + (registration.isTeamRegistration() ? registration.getTeamName() : registration.getFullName());
            emailQueueService.queueEmail(supportEmail, "Treasure Hunt Admin", subject, body, EmailQueue.EmailType.ADMIN_NOTIFICATION);
            logger.debug("Queued admin notification email for registration ID: {}", registration.getId());
        } catch (Exception e) {
            logger.error("Failed to queue admin notification email for registration ID {}: {}", registration.getId(), e.getMessage(), e);
            throw e;
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

                // Force load plan data to avoid lazy loading issues in templates
                if (registration.getPlan() != null) {
                    registration.getPlan().getName(); // This triggers lazy loading
                    registration.getPlan().getId(); // Ensure ID is also loaded
                }

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
     * Get all registrations with optimized query
     * @return List of all registrations with related data loaded
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getAllRegistrations() {
        logger.debug("Fetching all registrations with optimized query");
        List<UserRegistration> registrations = registrationRepository.findAllWithAllData();

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
            }
        }

        return registrations;
    }

    /**
     * Get all registrations with pagination and optimized query
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of registrations with related data loaded
     */
    @Transactional(readOnly = true)
    public Page<UserRegistration> getAllRegistrations(int page, int size) {
        logger.debug("Fetching registrations page {} with size {} using optimized query", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));
        return registrationRepository.findAllWithPlanData(pageable);
    }

    /**
     * Get registrations by plan with optimized query
     * @param plan Treasure hunt plan
     * @return List of registrations for the plan with related data loaded
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByPlan(TreasureHuntPlan plan) {
        logger.debug("Fetching registrations for plan ID: {} with optimized query", plan.getId());
        List<UserRegistration> registrations = registrationRepository.findByPlanWithAllDataOrderByRegistrationDateDesc(plan);

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
            }
        }

        return registrations;
    }

    /**
     * Get registrations by status with optimized query
     * @param status Registration status
     * @return List of registrations with specified status with related data loaded
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByStatus(UserRegistration.RegistrationStatus status) {
        logger.debug("Fetching registrations with status: {} with optimized query", status);
        List<UserRegistration> registrations = registrationRepository.findByStatusWithAllDataOrderByRegistrationDateDesc(status);

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
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

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
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

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
            }
        }

        return registrations;
    }

    /**
     * PERFORMANCE FIX: Update registration status with cache eviction
     * @param id Registration ID
     * @param newStatus New status
     * @return Updated registration
     * @throws IllegalArgumentException if registration not found
     */
    @CacheEvict(value = "registrationStatistics", allEntries = true)
    public UserRegistration updateRegistrationStatus(Long id, UserRegistration.RegistrationStatus newStatus) {
        logger.info("Updating registration status for ID: {} to {}", id, newStatus);

        UserRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found with ID: " + id));

        registration.setStatus(newStatus);
        return registrationRepository.save(registration);
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
        List<UserRegistration> registrations = registrationRepository.findRecentRegistrations(cutoffDate);

        // Force load documents and team members to avoid LazyInitializationException in templates
        for (UserRegistration registration : registrations) {
            try {
                registration.getDocuments().size(); // Trigger lazy loading
                registration.getTeamMembers().size(); // Trigger lazy loading
            } catch (Exception e) {
                logger.debug("Could not load collections for registration {}: {}", registration.getId(), e.getMessage());
            }
        }

        return registrations;
    }

    /**
     * PERFORMANCE FIX: Get registration statistics with caching
     * @return Registration statistics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "registrationStatistics", key = "'stats'")
    public RegistrationStatistics getRegistrationStatistics() {
        logger.debug("Calculating registration statistics (will be cached)");

        long totalRegistrations = registrationRepository.countTotalRegistrations();
        long pendingCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.PENDING);
        long confirmedCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.CONFIRMED);
        long cancelledCount = registrationRepository.countByStatus(UserRegistration.RegistrationStatus.CANCELLED);

        return new RegistrationStatistics(totalRegistrations, pendingCount, confirmedCount, cancelledCount);
    }

    /**
     * Send approval emails to all team members or individual participant
     * Uses proper transaction boundaries to avoid lazy loading issues
     * @param registration Registration to send approval emails for
     */
    public void sendConfirmationEmails(UserRegistration registration) {
        logger.info("Initiating approval emails for registration ID: {}", registration.getId());
        try {
            emailNotificationService.sendApprovalEmailsAsync(registration.getId());
            logger.info("Successfully queued approval emails for registration ID: {}", registration.getId());
        } catch (Exception e) {
            logger.error("Failed to queue approval emails for registration ID {}: {}", registration.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send cancellation email to team leader or individual participant
     * Uses proper transaction boundaries to avoid lazy loading issues
     * @param registration Registration to send cancellation for
     */
    public void sendCancellationEmail(UserRegistration registration) {
        logger.info("Initiating cancellation email for registration ID: {}", registration.getId());
        try {
            emailNotificationService.sendCancellationEmailAsync(registration.getId());
            logger.info("Successfully queued cancellation email for registration ID: {}", registration.getId());
        } catch (Exception e) {
            logger.error("Failed to queue cancellation email for registration ID {}: {}", registration.getId(), e.getMessage(), e);
        }
    }

    /**
     * Validate and sanitize registration data
     * @param registration Registration to validate and sanitize
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRegistration(UserRegistration registration) {
        // Sanitize and validate full name
        String sanitizedFullName = inputSanitizationService.sanitizeName(registration.getFullName());
        if (sanitizedFullName == null) {
            throw new IllegalArgumentException("Full name is required and must contain only valid characters");
        }
        registration.setFullName(sanitizedFullName);

        // Validate age
        if (registration.getAge() == null || registration.getAge() < 18 || registration.getAge() > 65) {
            throw new IllegalArgumentException("Age must be between 18 and 65");
        }

        // Sanitize and validate email
        String sanitizedEmail = inputSanitizationService.sanitizeEmail(registration.getEmail());
        if (sanitizedEmail == null) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        registration.setEmail(sanitizedEmail);

        // Sanitize and validate phone number
        String sanitizedPhone = inputSanitizationService.sanitizePhone(registration.getPhoneNumber());
        if (sanitizedPhone == null) {
            throw new IllegalArgumentException("Valid phone number is required");
        }
        registration.setPhoneNumber(sanitizedPhone);

        // Sanitize and validate emergency contact name
        String sanitizedEmergencyName = inputSanitizationService.sanitizeName(registration.getEmergencyContactName());
        if (sanitizedEmergencyName == null) {
            throw new IllegalArgumentException("Emergency contact name is required and must contain only valid characters");
        }
        registration.setEmergencyContactName(sanitizedEmergencyName);

        // Sanitize and validate emergency contact phone
        String sanitizedEmergencyPhone = inputSanitizationService.sanitizePhone(registration.getEmergencyContactPhone());
        if (sanitizedEmergencyPhone == null) {
            throw new IllegalArgumentException("Valid emergency contact phone number is required");
        }
        registration.setEmergencyContactPhone(sanitizedEmergencyPhone);

        // Sanitize optional fields
        if (registration.getBio() != null) {
            registration.setBio(inputSanitizationService.sanitizeText(registration.getBio()));
        }
        if (registration.getTeamName() != null) {
            String sanitizedTeamName = inputSanitizationService.sanitizeName(registration.getTeamName());
            if (sanitizedTeamName == null) {
                throw new IllegalArgumentException("Team name must contain only valid characters");
            }
            registration.setTeamName(sanitizedTeamName);
        }

        // Validate required consents
        if (registration.getMedicalConsentGiven() == null || !registration.getMedicalConsentGiven()) {
            throw new IllegalArgumentException("Medical consent must be given");
        }

        // Validate plan selection
        if (registration.getPlan() == null || registration.getPlan().getId() == null) {
            throw new IllegalArgumentException("Plan selection is required");
        }

        // Validate and sanitize team members if present
        if (registration.getTeamMembers() != null && !registration.getTeamMembers().isEmpty()) {
            validateAndSanitizeTeamMembers(registration.getTeamMembers());
        }
    }

    /**
     * Validate and sanitize team members
     * @param teamMembers List of team members to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAndSanitizeTeamMembers(List<TeamMember> teamMembers) {
        for (TeamMember member : teamMembers) {
            // Sanitize and validate name
            String sanitizedName = inputSanitizationService.sanitizeName(member.getFullName());
            if (sanitizedName == null) {
                throw new IllegalArgumentException("Team member name is required and must contain only valid characters");
            }
            member.setFullName(sanitizedName);

            // Validate age
            if (member.getAge() == null || member.getAge() < 18 || member.getAge() > 65) {
                throw new IllegalArgumentException("Team member age must be between 18 and 65");
            }

            // Sanitize and validate email if provided
            if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                String sanitizedEmail = inputSanitizationService.sanitizeEmail(member.getEmail());
                if (sanitizedEmail == null) {
                    throw new IllegalArgumentException("Team member email must be valid if provided");
                }
                member.setEmail(sanitizedEmail);
            }

            // Sanitize and validate phone if provided
            if (member.getPhoneNumber() != null && !member.getPhoneNumber().trim().isEmpty()) {
                String sanitizedPhone = inputSanitizationService.sanitizePhone(member.getPhoneNumber());
                if (sanitizedPhone == null) {
                    throw new IllegalArgumentException("Team member phone number must be valid if provided");
                }
                member.setPhoneNumber(sanitizedPhone);
            }

            // Sanitize optional fields
            if (member.getBio() != null) {
                member.setBio(inputSanitizationService.sanitizeText(member.getBio()));
            }
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
     * Perform registration rollback in separate transaction
     * This ensures rollback completes even if main transaction fails
     * @param registration Registration to rollback
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performRegistrationRollback(UserRegistration registration) {
        try {
            // First, try to delete any uploaded files
            try {
                fileStorageService.deleteAllFilesForRegistration(registration);
                logger.debug("Deleted files for registration rollback: {}", registration.getId());
            } catch (Exception fileException) {
                logger.warn("Could not delete files during rollback for registration {}: {}",
                           registration.getId(), fileException.getMessage());
                // Continue with registration deletion even if file deletion fails
            }

            // Delete the registration record
            registrationRepository.delete(registration);
            logger.info("Successfully rolled back registration ID: {}", registration.getId());

        } catch (Exception rollbackException) {
            logger.error("CRITICAL: Failed to rollback registration ID {}: {}",
                        registration.getId(), rollbackException.getMessage());
            // Re-throw to ensure the caller knows rollback failed
            throw new RuntimeException("Rollback failed for registration " + registration.getId(), rollbackException);
        }
    }

    /**
     * Get configured support email
     */
    private String getSupportEmail() {
        return supportEmail;
    }
}
