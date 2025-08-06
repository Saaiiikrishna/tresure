package com.treasurehunt.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all application events
 * Provides common event functionality and metadata
 */
public abstract class ApplicationEvent {

    private final String eventId;
    private final LocalDateTime timestamp;
    private final String eventType;
    private final String source;
    private final Map<String, Object> metadata;

    protected ApplicationEvent(String eventType, String source) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.source = source;
        this.metadata = new HashMap<>();
    }

    // Getters
    public String getEventId() { return eventId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getSource() { return source; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

    // Metadata management
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', type='%s', source='%s', timestamp=%s}",
                           getClass().getSimpleName(), eventId, eventType, source, timestamp);
    }
}

/**
 * Registration-related events
 */
abstract class RegistrationEvent extends ApplicationEvent {
    private final Long registrationId;
    private final String userEmail;

    protected RegistrationEvent(String eventType, Long registrationId, String userEmail) {
        super(eventType, "RegistrationService");
        this.registrationId = registrationId;
        this.userEmail = userEmail;
        addMetadata("registrationId", registrationId);
        addMetadata("userEmail", userEmail);
    }

    public Long getRegistrationId() { return registrationId; }
    public String getUserEmail() { return userEmail; }
}

/**
 * Registration created event
 */
class RegistrationCreatedEvent extends RegistrationEvent {
    private final Long planId;
    private final boolean isTeamRegistration;

    public RegistrationCreatedEvent(Long registrationId, String userEmail, Long planId, boolean isTeamRegistration) {
        super("REGISTRATION_CREATED", registrationId, userEmail);
        this.planId = planId;
        this.isTeamRegistration = isTeamRegistration;
        addMetadata("planId", planId);
        addMetadata("isTeamRegistration", isTeamRegistration);
    }

    public Long getPlanId() { return planId; }
    public boolean isTeamRegistration() { return isTeamRegistration; }
}

/**
 * Registration confirmed event
 */
class RegistrationConfirmedEvent extends RegistrationEvent {
    private final String applicationId;

    public RegistrationConfirmedEvent(Long registrationId, String userEmail, String applicationId) {
        super("REGISTRATION_CONFIRMED", registrationId, userEmail);
        this.applicationId = applicationId;
        addMetadata("applicationId", applicationId);
    }

    public String getApplicationId() { return applicationId; }
}

/**
 * Registration cancelled event
 */
class RegistrationCancelledEvent extends RegistrationEvent {
    private final String reason;

    public RegistrationCancelledEvent(Long registrationId, String userEmail, String reason) {
        super("REGISTRATION_CANCELLED", registrationId, userEmail);
        this.reason = reason;
        addMetadata("reason", reason);
    }

    public String getReason() { return reason; }
}

/**
 * Plan-related events
 */
abstract class PlanEvent extends ApplicationEvent {
    private final Long planId;
    private final String planName;

    protected PlanEvent(String eventType, Long planId, String planName) {
        super(eventType, "TreasureHuntPlanService");
        this.planId = planId;
        this.planName = planName;
        addMetadata("planId", planId);
        addMetadata("planName", planName);
    }

    public Long getPlanId() { return planId; }
    public String getPlanName() { return planName; }
}

/**
 * Plan created event
 */
class PlanCreatedEvent extends PlanEvent {
    public PlanCreatedEvent(Long planId, String planName) {
        super("PLAN_CREATED", planId, planName);
    }
}

/**
 * Plan updated event
 */
class PlanUpdatedEvent extends PlanEvent {
    private final String updatedBy;

    public PlanUpdatedEvent(Long planId, String planName, String updatedBy) {
        super("PLAN_UPDATED", planId, planName);
        this.updatedBy = updatedBy;
        addMetadata("updatedBy", updatedBy);
    }

    public String getUpdatedBy() { return updatedBy; }
}

/**
 * Plan featured event
 */
class PlanFeaturedEvent extends PlanEvent {
    private final Long previousFeaturedPlanId;

    public PlanFeaturedEvent(Long planId, String planName, Long previousFeaturedPlanId) {
        super("PLAN_FEATURED", planId, planName);
        this.previousFeaturedPlanId = previousFeaturedPlanId;
        addMetadata("previousFeaturedPlanId", previousFeaturedPlanId);
    }

    public Long getPreviousFeaturedPlanId() { return previousFeaturedPlanId; }
}

/**
 * File-related events
 */
abstract class FileEvent extends ApplicationEvent {
    private final String fileName;
    private final String filePath;
    private final long fileSize;

    protected FileEvent(String eventType, String fileName, String filePath, long fileSize) {
        super(eventType, "FileStorageService");
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        addMetadata("fileName", fileName);
        addMetadata("filePath", filePath);
        addMetadata("fileSize", fileSize);
    }

    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
}

/**
 * File uploaded event
 */
class FileUploadedEvent extends FileEvent {
    private final Long registrationId;
    private final String documentType;

    public FileUploadedEvent(String fileName, String filePath, long fileSize, 
                           Long registrationId, String documentType) {
        super("FILE_UPLOADED", fileName, filePath, fileSize);
        this.registrationId = registrationId;
        this.documentType = documentType;
        addMetadata("registrationId", registrationId);
        addMetadata("documentType", documentType);
    }

    public Long getRegistrationId() { return registrationId; }
    public String getDocumentType() { return documentType; }
}

/**
 * File deleted event
 */
class FileDeletedEvent extends FileEvent {
    private final String reason;

    public FileDeletedEvent(String fileName, String filePath, long fileSize, String reason) {
        super("FILE_DELETED", fileName, filePath, fileSize);
        this.reason = reason;
        addMetadata("reason", reason);
    }

    public String getReason() { return reason; }
}

/**
 * Email-related events
 */
abstract class EmailEvent extends ApplicationEvent {
    private final String recipientEmail;
    private final String subject;

    protected EmailEvent(String eventType, String recipientEmail, String subject) {
        super(eventType, "EmailService");
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        addMetadata("recipientEmail", recipientEmail);
        addMetadata("subject", subject);
    }

    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
}

/**
 * Email sent event
 */
class EmailSentEvent extends EmailEvent {
    private final Long emailQueueId;

    public EmailSentEvent(String recipientEmail, String subject, Long emailQueueId) {
        super("EMAIL_SENT", recipientEmail, subject);
        this.emailQueueId = emailQueueId;
        addMetadata("emailQueueId", emailQueueId);
    }

    public Long getEmailQueueId() { return emailQueueId; }
}

/**
 * Email failed event
 */
class EmailFailedEvent extends EmailEvent {
    private final String errorMessage;
    private final int attemptCount;

    public EmailFailedEvent(String recipientEmail, String subject, String errorMessage, int attemptCount) {
        super("EMAIL_FAILED", recipientEmail, subject);
        this.errorMessage = errorMessage;
        this.attemptCount = attemptCount;
        addMetadata("errorMessage", errorMessage);
        addMetadata("attemptCount", attemptCount);
    }

    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
}

/**
 * System-related events
 */
abstract class SystemEvent extends ApplicationEvent {
    protected SystemEvent(String eventType, String source) {
        super(eventType, source);
    }
}

/**
 * Application started event
 */
class ApplicationStartedEvent extends SystemEvent {
    private final String version;
    private final String profile;

    public ApplicationStartedEvent(String version, String profile) {
        super("APPLICATION_STARTED", "TreasureHuntApplication");
        this.version = version;
        this.profile = profile;
        addMetadata("version", version);
        addMetadata("profile", profile);
    }

    public String getVersion() { return version; }
    public String getProfile() { return profile; }
}

/**
 * Performance threshold exceeded event
 */
class PerformanceThresholdExceededEvent extends SystemEvent {
    private final String metricName;
    private final double currentValue;
    private final double threshold;

    public PerformanceThresholdExceededEvent(String metricName, double currentValue, double threshold) {
        super("PERFORMANCE_THRESHOLD_EXCEEDED", "PerformanceMonitoringService");
        this.metricName = metricName;
        this.currentValue = currentValue;
        this.threshold = threshold;
        addMetadata("metricName", metricName);
        addMetadata("currentValue", currentValue);
        addMetadata("threshold", threshold);
    }

    public String getMetricName() { return metricName; }
    public double getCurrentValue() { return currentValue; }
    public double getThreshold() { return threshold; }
}
