package com.treasurehunt.service;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UploadedDocument;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.UserRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    private final TreasureHuntPlanService planService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @Autowired
    public RegistrationService(UserRegistrationRepository registrationRepository,
                              TreasureHuntPlanService planService,
                              FileStorageService fileStorageService,
                              EmailService emailService) {
        this.registrationRepository = registrationRepository;
        this.planService = planService;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
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

            // Send confirmation email
            emailService.sendRegistrationConfirmation(savedRegistration);
            
            // Send admin notification
            emailService.sendAdminNotification(savedRegistration);

            logger.info("Successfully created registration with ID: {}", savedRegistration.getId());
            return savedRegistration;

        } catch (Exception e) {
            // Rollback: delete registration if file processing fails
            logger.error("Failed to process files for registration, rolling back", e);
            registrationRepository.delete(savedRegistration);
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
     * Get all registrations
     * @return List of all registrations
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getAllRegistrations() {
        logger.debug("Fetching all registrations");
        return registrationRepository.findAll();
    }

    /**
     * Get registrations by plan
     * @param plan Treasure hunt plan
     * @return List of registrations for the plan
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByPlan(TreasureHuntPlan plan) {
        logger.debug("Fetching registrations for plan ID: {}", plan.getId());
        return registrationRepository.findByPlanOrderByRegistrationDateDesc(plan);
    }

    /**
     * Get registrations by status
     * @param status Registration status
     * @return List of registrations with specified status
     */
    @Transactional(readOnly = true)
    public List<UserRegistration> getRegistrationsByStatus(UserRegistration.RegistrationStatus status) {
        logger.debug("Fetching registrations with status: {}", status);
        return registrationRepository.findByStatusOrderByRegistrationDateDesc(status);
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

        // Send status update email if status changed
        if (oldStatus != newStatus) {
            emailService.sendStatusUpdateEmail(savedRegistration, newStatus);
        }

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
}
