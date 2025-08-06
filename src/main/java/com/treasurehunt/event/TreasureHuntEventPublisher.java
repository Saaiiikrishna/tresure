package com.treasurehunt.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application event publisher that provides centralized event publishing
 * Handles both synchronous and asynchronous event publishing
 */
@Component
public class TreasureHuntEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntEventPublisher.class);

    private final ApplicationEventPublisher springEventPublisher;
    private final AtomicLong eventCounter = new AtomicLong(0);

    @Autowired
    public TreasureHuntEventPublisher(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
    }

    /**
     * Publish event synchronously
     * @param event Event to publish
     */
    public void publishEvent(ApplicationEvent event) {
        try {
            logger.debug("Publishing event: {}", event);
            springEventPublisher.publishEvent(event);
            eventCounter.incrementAndGet();
            logger.debug("Successfully published event: {}", event.getEventId());
        } catch (Exception e) {
            logger.error("Error publishing event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Publish event asynchronously
     * @param event Event to publish
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<Void> publishEventAsync(ApplicationEvent event) {
        return CompletableFuture.runAsync(() -> publishEvent(event));
    }

    /**
     * Publish registration created event
     */
    public void publishRegistrationCreated(Long registrationId, String userEmail, Long planId, boolean isTeamRegistration) {
        RegistrationCreatedEvent event = new RegistrationCreatedEvent(registrationId, userEmail, planId, isTeamRegistration);
        publishEvent(event);
    }

    /**
     * Publish registration confirmed event
     */
    public void publishRegistrationConfirmed(Long registrationId, String userEmail, String applicationId) {
        RegistrationConfirmedEvent event = new RegistrationConfirmedEvent(registrationId, userEmail, applicationId);
        publishEvent(event);
    }

    /**
     * Publish registration cancelled event
     */
    public void publishRegistrationCancelled(Long registrationId, String userEmail, String reason) {
        RegistrationCancelledEvent event = new RegistrationCancelledEvent(registrationId, userEmail, reason);
        publishEvent(event);
    }

    /**
     * Publish plan created event
     */
    public void publishPlanCreated(Long planId, String planName) {
        PlanCreatedEvent event = new PlanCreatedEvent(planId, planName);
        publishEvent(event);
    }

    /**
     * Publish plan updated event
     */
    public void publishPlanUpdated(Long planId, String planName, String updatedBy) {
        PlanUpdatedEvent event = new PlanUpdatedEvent(planId, planName, updatedBy);
        publishEvent(event);
    }

    /**
     * Publish plan featured event
     */
    public void publishPlanFeatured(Long planId, String planName, Long previousFeaturedPlanId) {
        PlanFeaturedEvent event = new PlanFeaturedEvent(planId, planName, previousFeaturedPlanId);
        publishEvent(event);
    }

    /**
     * Publish file uploaded event
     */
    public void publishFileUploaded(String fileName, String filePath, long fileSize, 
                                  Long registrationId, String documentType) {
        FileUploadedEvent event = new FileUploadedEvent(fileName, filePath, fileSize, registrationId, documentType);
        publishEvent(event);
    }

    /**
     * Publish file deleted event
     */
    public void publishFileDeleted(String fileName, String filePath, long fileSize, String reason) {
        FileDeletedEvent event = new FileDeletedEvent(fileName, filePath, fileSize, reason);
        publishEvent(event);
    }

    /**
     * Publish email sent event
     */
    public void publishEmailSent(String recipientEmail, String subject, Long emailQueueId) {
        EmailSentEvent event = new EmailSentEvent(recipientEmail, subject, emailQueueId);
        publishEvent(event);
    }

    /**
     * Publish email failed event
     */
    public void publishEmailFailed(String recipientEmail, String subject, String errorMessage, int attemptCount) {
        EmailFailedEvent event = new EmailFailedEvent(recipientEmail, subject, errorMessage, attemptCount);
        publishEvent(event);
    }

    /**
     * Publish application started event
     */
    public void publishApplicationStarted(String version, String profile) {
        ApplicationStartedEvent event = new ApplicationStartedEvent(version, profile);
        publishEvent(event);
    }

    /**
     * Publish performance threshold exceeded event
     */
    public void publishPerformanceThresholdExceeded(String metricName, double currentValue, double threshold) {
        PerformanceThresholdExceededEvent event = new PerformanceThresholdExceededEvent(metricName, currentValue, threshold);
        publishEvent(event);
    }

    /**
     * Get total number of events published
     */
    public long getEventCount() {
        return eventCounter.get();
    }

    /**
     * Reset event counter
     */
    public void resetEventCounter() {
        eventCounter.set(0);
        logger.info("Event counter reset");
    }

    /**
     * Get event publishing statistics
     */
    public EventPublishingStats getStats() {
        return new EventPublishingStats(eventCounter.get());
    }

    /**
     * Event publishing statistics
     */
    public static class EventPublishingStats {
        private final long totalEventsPublished;

        public EventPublishingStats(long totalEventsPublished) {
            this.totalEventsPublished = totalEventsPublished;
        }

        public long getTotalEventsPublished() {
            return totalEventsPublished;
        }

        @Override
        public String toString() {
            return String.format("EventPublishingStats{totalEventsPublished=%d}", totalEventsPublished);
        }
    }
}
