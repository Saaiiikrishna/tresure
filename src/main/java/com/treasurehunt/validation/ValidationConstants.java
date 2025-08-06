package com.treasurehunt.validation;

/**
 * Centralized validation constants to ensure consistency across the application
 * This class contains all validation rules, limits, and constraints used throughout the system
 */
public final class ValidationConstants {

    // Private constructor to prevent instantiation
    private ValidationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Age validation constants
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 65;
    public static final String AGE_VALIDATION_MESSAGE = "Age must be between " + MIN_AGE + " and " + MAX_AGE;

    // Name validation constants
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;
    public static final String NAME_PATTERN = "^[a-zA-Z\\s'-]+$";
    public static final String NAME_VALIDATION_MESSAGE = "Name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters and contain only letters, spaces, hyphens, and apostrophes";

    // Email validation constants
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String EMAIL_VALIDATION_MESSAGE = "Please provide a valid email address";

    // Phone validation constants
    public static final int MIN_PHONE_LENGTH = 10;
    public static final int MAX_PHONE_LENGTH = 15;
    public static final String PHONE_PATTERN = "^[+]?[0-9\\s()-]+$";
    public static final String PHONE_VALIDATION_MESSAGE = "Phone number must be between " + MIN_PHONE_LENGTH + " and " + MAX_PHONE_LENGTH + " digits";

    // Plan validation constants
    public static final int MIN_PLAN_DURATION_HOURS = 1;
    public static final int MAX_PLAN_DURATION_HOURS = 168; // 1 week
    public static final int MIN_PARTICIPANTS = 1;
    public static final int MAX_PARTICIPANTS = 100;
    public static final int MAX_AVAILABLE_SLOTS = 10000;
    public static final String PLAN_NAME_MAX_LENGTH_MESSAGE = "Plan name must not exceed 255 characters";
    public static final String PLAN_DESCRIPTION_MAX_LENGTH_MESSAGE = "Plan description must not exceed 2000 characters";

    // File validation constants
    public static final long MAX_PHOTO_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    public static final long MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    public static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    public static final long MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024; // 50MB

    // Allowed file types
    public static final String[] ALLOWED_PHOTO_TYPES = {"image/jpeg", "image/jpg", "image/png"};
    public static final String[] ALLOWED_DOCUMENT_TYPES = {"application/pdf", "image/jpeg", "image/jpg"};
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"};
    public static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/webm", "video/ogg"};

    // Team validation constants
    public static final int MIN_TEAM_SIZE = 2;
    public static final int MAX_TEAM_SIZE = 10;
    public static final int MIN_TEAM_NAME_LENGTH = 3;
    public static final int MAX_TEAM_NAME_LENGTH = 100;
    public static final String TEAM_NAME_PATTERN = "^[a-zA-Z0-9\\s'-]+$";
    public static final String TEAM_NAME_VALIDATION_MESSAGE = "Team name must be between " + MIN_TEAM_NAME_LENGTH + " and " + MAX_TEAM_NAME_LENGTH + " characters";

    // Address validation constants
    public static final int MAX_ADDRESS_LENGTH = 500;
    public static final int MAX_CITY_LENGTH = 100;
    public static final int MAX_STATE_LENGTH = 100;
    public static final int MAX_POSTAL_CODE_LENGTH = 20;
    public static final String POSTAL_CODE_PATTERN = "^[0-9A-Za-z\\s-]+$";

    // Emergency contact validation constants
    public static final int MAX_EMERGENCY_CONTACT_NAME_LENGTH = 100;
    public static final int MAX_RELATIONSHIP_LENGTH = 50;

    // Application ID validation constants
    public static final String APPLICATION_ID_PREFIX_INDIVIDUAL = "TH";
    public static final String APPLICATION_ID_PREFIX_TEAM = "THT";
    public static final int APPLICATION_ID_SEQUENCE_LENGTH = 6;

    // Password validation constants (for admin)
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$";
    public static final String PASSWORD_VALIDATION_MESSAGE = "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";

    // Email campaign validation constants
    public static final int MAX_EMAIL_SUBJECT_LENGTH = 255;
    public static final int MAX_EMAIL_BODY_LENGTH = 10000;
    public static final int MAX_CAMPAIGN_NAME_LENGTH = 255;

    // Settings validation constants
    public static final int MAX_SETTING_KEY_LENGTH = 100;
    public static final int MAX_SETTING_VALUE_LENGTH = 2000;
    public static final int MAX_SETTING_DESCRIPTION_LENGTH = 500;

    // Validation helper methods
    public static boolean isValidAge(Integer age) {
        return age != null && age >= MIN_AGE && age <= MAX_AGE;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= MAX_EMAIL_LENGTH && email.matches(EMAIL_PATTERN);
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("[\\s()-+]", "");
        return cleanPhone.length() >= MIN_PHONE_LENGTH && cleanPhone.length() <= MAX_PHONE_LENGTH;
    }

    public static boolean isValidName(String name) {
        return name != null && name.length() >= MIN_NAME_LENGTH && 
               name.length() <= MAX_NAME_LENGTH && name.matches(NAME_PATTERN);
    }

    public static boolean isValidTeamName(String teamName) {
        return teamName != null && teamName.length() >= MIN_TEAM_NAME_LENGTH && 
               teamName.length() <= MAX_TEAM_NAME_LENGTH && teamName.matches(TEAM_NAME_PATTERN);
    }

    public static boolean isValidPlanDuration(Integer hours) {
        return hours != null && hours >= MIN_PLAN_DURATION_HOURS && hours <= MAX_PLAN_DURATION_HOURS;
    }

    public static boolean isValidParticipantCount(Integer count) {
        return count != null && count >= MIN_PARTICIPANTS && count <= MAX_PARTICIPANTS;
    }

    public static boolean isValidAvailableSlots(Integer slots) {
        return slots != null && slots >= 0 && slots <= MAX_AVAILABLE_SLOTS;
    }

    public static String getFileSizeValidationMessage(long actualSize, long maxSize) {
        return String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", actualSize, maxSize);
    }

    public static String getFileTypeValidationMessage(String actualType, String[] allowedTypes) {
        return String.format("File type '%s' is not allowed. Allowed types: %s", actualType, String.join(", ", allowedTypes));
    }
}
