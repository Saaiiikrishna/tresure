package com.treasurehunt.validation;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Centralized validation service to ensure consistent validation across the application
 * This service provides comprehensive validation methods for all entities and operations
 */
@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    /**
     * Validate user registration data
     * @param registration User registration to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateUserRegistration(UserRegistration registration) {
        List<String> errors = new ArrayList<>();

        if (registration == null) {
            errors.add("Registration data is required");
            return errors;
        }

        // Validate basic personal information
        if (!ValidationConstants.isValidName(registration.getFullName())) {
            errors.add(ValidationConstants.NAME_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidEmail(registration.getEmail())) {
            errors.add(ValidationConstants.EMAIL_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidPhone(registration.getPhoneNumber())) {
            errors.add(ValidationConstants.PHONE_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidAge(registration.getAge())) {
            errors.add(ValidationConstants.AGE_VALIDATION_MESSAGE);
        }

        // Validate team information if it's a team registration
        if (registration.isTeamRegistration()) {
            if (!ValidationConstants.isValidTeamName(registration.getTeamName())) {
                errors.add(ValidationConstants.TEAM_NAME_VALIDATION_MESSAGE);
            }
        }

        return errors;
    }

    /**
     * Validate team member data
     * @param teamMember Team member to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateTeamMember(TeamMember teamMember) {
        List<String> errors = new ArrayList<>();

        if (teamMember == null) {
            errors.add("Team member data is required");
            return errors;
        }

        if (!ValidationConstants.isValidName(teamMember.getFullName())) {
            errors.add("Team member " + ValidationConstants.NAME_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidEmail(teamMember.getEmail())) {
            errors.add("Team member " + ValidationConstants.EMAIL_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidPhone(teamMember.getPhoneNumber())) {
            errors.add("Team member " + ValidationConstants.PHONE_VALIDATION_MESSAGE);
        }

        if (!ValidationConstants.isValidAge(teamMember.getAge())) {
            errors.add("Team member " + ValidationConstants.AGE_VALIDATION_MESSAGE);
        }

        return errors;
    }

    /**
     * Validate treasure hunt plan data
     * @param plan Treasure hunt plan to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateTreasureHuntPlan(TreasureHuntPlan plan) {
        List<String> errors = new ArrayList<>();

        if (plan == null) {
            errors.add("Plan data is required");
            return errors;
        }

        // Validate plan name
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            errors.add("Plan name is required");
        } else if (plan.getName().length() > 255) {
            errors.add(ValidationConstants.PLAN_NAME_MAX_LENGTH_MESSAGE);
        }

        // Validate duration
        if (!ValidationConstants.isValidPlanDuration(plan.getDurationHours())) {
            errors.add("Duration must be between " + ValidationConstants.MIN_PLAN_DURATION_HOURS + 
                      " and " + ValidationConstants.MAX_PLAN_DURATION_HOURS + " hours");
        }

        // Validate participants
        if (!ValidationConstants.isValidParticipantCount(plan.getMaxParticipants())) {
            errors.add("Maximum participants must be between " + ValidationConstants.MIN_PARTICIPANTS + 
                      " and " + ValidationConstants.MAX_PARTICIPANTS);
        }

        // Validate available slots
        if (!ValidationConstants.isValidAvailableSlots(plan.getAvailableSlots())) {
            errors.add("Available slots must be between 0 and " + ValidationConstants.MAX_AVAILABLE_SLOTS);
        }

        // Validate price
        if (plan.getPriceInr() == null || plan.getPriceInr().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Price must be non-negative");
        }

        // Validate difficulty level
        if (plan.getDifficultyLevel() == null) {
            errors.add("Difficulty level is required");
        }

        return errors;
    }

    /**
     * Validate uploaded file
     * @param file Uploaded file
     * @param allowedTypes Array of allowed MIME types
     * @param maxSize Maximum file size in bytes
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateUploadedFile(MultipartFile file, String[] allowedTypes, long maxSize) {
        List<String> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add("File is required");
            return errors;
        }

        // Validate file size
        if (file.getSize() > maxSize) {
            errors.add(ValidationConstants.getFileSizeValidationMessage(file.getSize(), maxSize));
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType.toLowerCase())) {
            errors.add(ValidationConstants.getFileTypeValidationMessage(contentType, allowedTypes));
        }

        return errors;
    }

    /**
     * Validate photo file
     * @param file Photo file to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validatePhotoFile(MultipartFile file) {
        return validateUploadedFile(file, ValidationConstants.ALLOWED_PHOTO_TYPES, ValidationConstants.MAX_PHOTO_SIZE_BYTES);
    }

    /**
     * Validate document file
     * @param file Document file to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateDocumentFile(MultipartFile file) {
        return validateUploadedFile(file, ValidationConstants.ALLOWED_DOCUMENT_TYPES, ValidationConstants.MAX_DOCUMENT_SIZE_BYTES);
    }

    /**
     * Validate image file
     * @param file Image file to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateImageFile(MultipartFile file) {
        return validateUploadedFile(file, ValidationConstants.ALLOWED_IMAGE_TYPES, ValidationConstants.MAX_IMAGE_SIZE_BYTES);
    }

    /**
     * Log validation errors
     * @param errors List of validation errors
     * @param context Context for logging (e.g., "User Registration", "Plan Creation")
     */
    public void logValidationErrors(List<String> errors, String context) {
        if (!errors.isEmpty()) {
            logger.warn("Validation errors in {}: {}", context, String.join(", ", errors));
        }
    }

    /**
     * Check if validation passed (no errors)
     * @param errors List of validation errors
     * @return true if no errors, false otherwise
     */
    public boolean isValid(List<String> errors) {
        return errors == null || errors.isEmpty();
    }

    /**
     * Validate and throw exception if validation fails
     * @param errors List of validation errors
     * @param context Validation context for error message
     * @throws ValidationException if validation fails
     */
    public void validateAndThrow(List<String> errors, String context) throws ValidationException {
        if (!isValid(errors)) {
            String errorMessage = String.format("Validation failed for %s: %s", context, String.join(", ", errors));
            logValidationErrors(errors, context);
            throw new ValidationException(errorMessage, "VALIDATION_FAILED");
        }
    }

    /**
     * Validate user registration and throw exception if invalid
     * @param registration User registration to validate
     * @throws ValidationException if validation fails
     */
    public void validateUserRegistrationOrThrow(UserRegistration registration) throws ValidationException {
        List<String> errors = validateUserRegistration(registration);
        validateAndThrow(errors, "User Registration");
    }

    /**
     * Validate team member and throw exception if invalid
     * @param teamMember Team member to validate
     * @throws ValidationException if validation fails
     */
    public void validateTeamMemberOrThrow(TeamMember teamMember) throws ValidationException {
        List<String> errors = validateTeamMember(teamMember);
        validateAndThrow(errors, "Team Member");
    }

    /**
     * Validate treasure hunt plan and throw exception if invalid
     * @param plan Treasure hunt plan to validate
     * @throws ValidationException if validation fails
     */
    public void validateTreasureHuntPlanOrThrow(TreasureHuntPlan plan) throws ValidationException {
        List<String> errors = validateTreasureHuntPlan(plan);
        validateAndThrow(errors, "Treasure Hunt Plan");
    }

    /**
     * Validate photo file and throw exception if invalid
     * @param file Photo file to validate
     * @throws ValidationException if validation fails
     */
    public void validatePhotoFileOrThrow(MultipartFile file) throws ValidationException {
        List<String> errors = validatePhotoFile(file);
        validateAndThrow(errors, "Photo File");
    }

    /**
     * Validate document file and throw exception if invalid
     * @param file Document file to validate
     * @throws ValidationException if validation fails
     */
    public void validateDocumentFileOrThrow(MultipartFile file) throws ValidationException {
        List<String> errors = validateDocumentFile(file);
        validateAndThrow(errors, "Document File");
    }

    /**
     * Validate image file and throw exception if invalid
     * @param file Image file to validate
     * @throws ValidationException if validation fails
     */
    public void validateImageFileOrThrow(MultipartFile file) throws ValidationException {
        List<String> errors = validateImageFile(file);
        validateAndThrow(errors, "Image File");
    }

    /**
     * Batch validate multiple objects
     * @param validations Map of validation contexts to error lists
     * @throws ValidationException if any validation fails
     */
    public void batchValidateAndThrow(Map<String, List<String>> validations) throws ValidationException {
        List<String> allErrors = new ArrayList<>();
        List<String> failedContexts = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : validations.entrySet()) {
            String context = entry.getKey();
            List<String> errors = entry.getValue();

            if (!isValid(errors)) {
                failedContexts.add(context);
                errors.forEach(error -> allErrors.add(context + ": " + error));
            }
        }

        if (!allErrors.isEmpty()) {
            String errorMessage = String.format("Batch validation failed for contexts [%s]: %s",
                                               String.join(", ", failedContexts), String.join("; ", allErrors));
            logger.error("Batch validation failed: {}", errorMessage);
            throw new ValidationException(errorMessage, "BATCH_VALIDATION_FAILED");
        }
    }

    /**
     * Create validation summary
     * @param errors List of validation errors
     * @param context Validation context
     * @return Validation summary
     */
    public ValidationSummary createValidationSummary(List<String> errors, String context) {
        return new ValidationSummary(context, errors, isValid(errors));
    }

    /**
     * Validation summary data class
     */
    public static class ValidationSummary {
        private final String context;
        private final List<String> errors;
        private final boolean valid;
        private final int errorCount;

        public ValidationSummary(String context, List<String> errors, boolean valid) {
            this.context = context;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.valid = valid;
            this.errorCount = this.errors.size();
        }

        // Getters
        public String getContext() { return context; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public boolean isValid() { return valid; }
        public int getErrorCount() { return errorCount; }

        @Override
        public String toString() {
            return String.format("ValidationSummary{context='%s', valid=%s, errors=%d}",
                               context, valid, errorCount);
        }
    }
}
