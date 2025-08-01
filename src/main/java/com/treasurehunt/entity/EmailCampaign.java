package com.treasurehunt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an email campaign
 * Maps to email_campaigns table in database
 */
@Entity
@Table(name = "email_campaigns")
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Subject is required")
    @Column(name = "subject", nullable = false)
    private String subject;

    @NotBlank(message = "Email body is required")
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @NotNull(message = "Campaign type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false, length = 50)
    private CampaignType campaignType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "target_audience", length = 100)
    private String targetAudience; // ALL, INDIVIDUAL_REGISTRATIONS, TEAM_REGISTRATIONS, etc.

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    // Statistics
    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients = 0;

    @Column(name = "emails_sent", nullable = false)
    private Integer emailsSent = 0;

    @Column(name = "emails_failed", nullable = false)
    private Integer emailsFailed = 0;

    @Column(name = "emails_pending", nullable = false)
    private Integer emailsPending = 0;

    // Template information
    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables; // JSON string of template variables

    // Campaign settings
    @Column(name = "priority", nullable = false)
    private Integer priority = 5; // 1 = highest, 10 = lowest

    @Column(name = "max_retry_attempts", nullable = false)
    private Integer maxRetryAttempts = 3;

    // Enums
    public enum CampaignType {
        PROMOTIONAL,
        INFORMATIONAL,
        REMINDER,
        ANNOUNCEMENT,
        NEWSLETTER,
        EVENT_UPDATE,
        REGISTRATION_FOLLOWUP
    }

    public enum CampaignStatus {
        DRAFT,
        SCHEDULED,
        SENDING,
        SENT,
        PAUSED,
        CANCELLED,
        FAILED
    }

    // Constructors
    public EmailCampaign() {}

    public EmailCampaign(String name, String subject, String body, 
                        CampaignType campaignType, String createdBy) {
        this.name = name;
        this.subject = subject;
        this.body = body;
        this.campaignType = campaignType;
        this.createdBy = createdBy;
        this.status = CampaignStatus.DRAFT;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public CampaignType getCampaignType() { return campaignType; }
    public void setCampaignType(CampaignType campaignType) { this.campaignType = campaignType; }

    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Integer getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(Integer totalRecipients) { this.totalRecipients = totalRecipients; }

    public Integer getEmailsSent() { return emailsSent; }
    public void setEmailsSent(Integer emailsSent) { this.emailsSent = emailsSent; }

    public Integer getEmailsFailed() { return emailsFailed; }
    public void setEmailsFailed(Integer emailsFailed) { this.emailsFailed = emailsFailed; }

    public Integer getEmailsPending() { return emailsPending; }
    public void setEmailsPending(Integer emailsPending) { this.emailsPending = emailsPending; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(String templateVariables) { this.templateVariables = templateVariables; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

    // Helper methods
    public boolean isActive() {
        return status == CampaignStatus.SENDING || status == CampaignStatus.SCHEDULED;
    }

    public boolean canBeSent() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.SCHEDULED;
    }

    public double getSuccessRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) emailsSent / totalRecipients * 100;
    }

    public double getFailureRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) emailsFailed / totalRecipients * 100;
    }

    public void updateStatistics(int sent, int failed, int pending) {
        this.emailsSent = sent;
        this.emailsFailed = failed;
        this.emailsPending = pending;
        this.totalRecipients = sent + failed + pending;
    }
}
