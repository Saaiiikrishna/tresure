package com.treasurehunt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an email in the queue system
 * Maps to email_queue table in database
 */
@Entity
@Table(name = "email_queue")
public class EmailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @NotBlank(message = "Recipient name is required")
    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @NotBlank(message = "Subject is required")
    @Column(name = "subject", nullable = false)
    private String subject;

    @NotBlank(message = "Email body is required")
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @NotNull(message = "Email type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    private EmailType emailType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5; // 1 = highest, 10 = lowest

    @Column(name = "max_retry_attempts", nullable = false)
    private Integer maxRetryAttempts = 3;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Reference to registration if applicable
    @Column(name = "registration_id")
    private Long registrationId;

    // Campaign information
    @Column(name = "campaign_name")
    private String campaignName;

    @Column(name = "campaign_id")
    private String campaignId;

    // Email template information
    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables; // JSON string of template variables

    // Enums
    public enum EmailType {
        REGISTRATION_CONFIRMATION,
        APPLICATION_APPROVAL,
        ADMIN_NOTIFICATION,
        CAMPAIGN_EMAIL,
        REMINDER_EMAIL,
        CANCELLATION_EMAIL,
        WELCOME_EMAIL,
        EVENT_UPDATE
    }

    public enum EmailStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        CANCELLED,
        SCHEDULED
    }

    // Constructors
    public EmailQueue() {}

    public EmailQueue(String recipientEmail, String recipientName, String subject, 
                     String body, EmailType emailType) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.body = body;
        this.emailType = emailType;
        this.status = EmailStatus.PENDING;
        this.scheduledDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public EmailType getEmailType() { return emailType; }
    public void setEmailType(EmailType emailType) { this.emailType = emailType; }

    public EmailStatus getStatus() { return status; }
    public void setStatus(EmailStatus status) { this.status = status; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(String templateVariables) { this.templateVariables = templateVariables; }

    // Helper methods
    public boolean canRetry() {
        return retryCount < maxRetryAttempts && status == EmailStatus.FAILED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean isReadyToSend() {
        return status == EmailStatus.PENDING || status == EmailStatus.SCHEDULED;
    }

    public boolean isScheduled() {
        return scheduledDate != null && scheduledDate.isAfter(LocalDateTime.now());
    }
}
