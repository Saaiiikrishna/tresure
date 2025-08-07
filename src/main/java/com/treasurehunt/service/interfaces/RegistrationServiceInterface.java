package com.treasurehunt.service.interfaces;

import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.UserRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for registration operations
 * Provides abstraction layer for registration business logic
 */
public interface RegistrationServiceInterface {

    /**
     * Register individual participant
     * @param registration Registration data
     * @param planId Plan ID
     * @return Created registration
     */
    UserRegistration registerIndividual(UserRegistration registration, Long planId);

    /**
     * Register team
     * @param teamLeader Team leader registration
     * @param teamMembers List of team members
     * @param planId Plan ID
     * @return Created team registration
     */
    UserRegistration registerTeam(UserRegistration teamLeader, List<TeamMember> teamMembers, Long planId);

    /**
     * Get registration by ID
     * @param id Registration ID
     * @return Optional registration
     */
    Optional<UserRegistration> getRegistrationById(Long id);

    /**
     * Get registration by email
     * @param email Email address
     * @return Registration if found
     */
    UserRegistration getRegistrationByEmail(String email);

    /**
     * Get registrations by plan
     * @param planId Plan ID
     * @return List of registrations for the plan
     */
    List<UserRegistration> getRegistrationsByPlan(Long planId);

    /**
     * Get paginated registrations
     * @param pageable Pagination parameters
     * @return Page of registrations
     */
    Page<UserRegistration> getRegistrations(Pageable pageable);

    /**
     * Get registrations by status
     * @param status Registration status
     * @return List of registrations with specified status
     */
    List<UserRegistration> getRegistrationsByStatus(UserRegistration.RegistrationStatus status);

    /**
     * Confirm registration
     * @param registrationId Registration ID
     * @return Updated registration
     */
    UserRegistration confirmRegistration(Long registrationId);

    /**
     * Cancel registration
     * @param registrationId Registration ID
     * @return Updated registration
     */
    UserRegistration cancelRegistration(Long registrationId);

    /**
     * Update registration status
     * @param registrationId Registration ID
     * @param status New status
     * @return Updated registration
     */
    UserRegistration updateRegistrationStatus(Long registrationId, UserRegistration.RegistrationStatus status);

    /**
     * Upload document for registration
     * @param registrationId Registration ID
     * @param file Document file
     * @param documentType Document type
     * @return Upload result
     */
    DocumentUploadResult uploadDocument(Long registrationId, MultipartFile file, String documentType);

    /**
     * Get registration statistics
     * @return Registration statistics
     */
    RegistrationStatistics getRegistrationStatistics();

    /**
     * Send confirmation emails
     * @param registration Registration to send confirmations for
     */
    void sendConfirmationEmails(UserRegistration registration);

    /**
     * Send cancellation email
     * @param registration Registration to send cancellation for
     */
    void sendCancellationEmail(UserRegistration registration);

    /**
     * Document upload result
     */
    class DocumentUploadResult {
        private final boolean success;
        private final String message;
        private final String documentId;
        private final String filePath;

        public DocumentUploadResult(boolean success, String message, String documentId, String filePath) {
            this.success = success;
            this.message = message;
            this.documentId = documentId;
            this.filePath = filePath;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDocumentId() { return documentId; }
        public String getFilePath() { return filePath; }

        @Override
        public String toString() {
            return String.format("DocumentUploadResult{success=%s, message='%s', documentId='%s'}",
                               success, message, documentId);
        }
    }

    /**
     * Registration statistics
     */
    class RegistrationStatistics {
        private final long totalRegistrations;
        private final long pendingRegistrations;
        private final long confirmedRegistrations;
        private final long cancelledRegistrations;
        private final long individualRegistrations;
        private final long teamRegistrations;
        private final double confirmationRate;

        public RegistrationStatistics(long totalRegistrations, long pendingRegistrations, 
                                    long confirmedRegistrations, long cancelledRegistrations,
                                    long individualRegistrations, long teamRegistrations,
                                    double confirmationRate) {
            this.totalRegistrations = totalRegistrations;
            this.pendingRegistrations = pendingRegistrations;
            this.confirmedRegistrations = confirmedRegistrations;
            this.cancelledRegistrations = cancelledRegistrations;
            this.individualRegistrations = individualRegistrations;
            this.teamRegistrations = teamRegistrations;
            this.confirmationRate = confirmationRate;
        }

        // Getters
        public long getTotalRegistrations() { return totalRegistrations; }
        public long getPendingRegistrations() { return pendingRegistrations; }
        public long getConfirmedRegistrations() { return confirmedRegistrations; }
        public long getCancelledRegistrations() { return cancelledRegistrations; }
        public long getIndividualRegistrations() { return individualRegistrations; }
        public long getTeamRegistrations() { return teamRegistrations; }
        public double getConfirmationRate() { return confirmationRate; }

        @Override
        public String toString() {
            return String.format("RegistrationStatistics{total=%d, pending=%d, confirmed=%d, cancelled=%d, individual=%d, team=%d, confirmationRate=%.1f%%}",
                               totalRegistrations, pendingRegistrations, confirmedRegistrations, cancelledRegistrations,
                               individualRegistrations, teamRegistrations, confirmationRate);
        }
    }
}
