package com.treasurehunt.event;

import com.treasurehunt.service.PerformanceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Application event listener that handles all application events
 * Provides centralized event handling and processing
 */
@Component
public class TreasureHuntEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntEventListener.class);

    private final PerformanceMonitoringService performanceMonitoringService;
    
    // Event counters
    private final AtomicLong registrationEventCount = new AtomicLong(0);
    private final AtomicLong planEventCount = new AtomicLong(0);
    private final AtomicLong fileEventCount = new AtomicLong(0);
    private final AtomicLong emailEventCount = new AtomicLong(0);
    private final AtomicLong systemEventCount = new AtomicLong(0);

    @Autowired
    public TreasureHuntEventListener(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Handle registration created events
     */
    @EventListener
    @Async
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        logger.info("📝 Registration created: ID={}, Email={}, Plan={}, Team={}", 
                   event.getRegistrationId(), event.getUserEmail(), event.getPlanId(), event.isTeamRegistration());
        
        registrationEventCount.incrementAndGet();
    }

    /**
     * Handle registration confirmed events
     */
    @EventListener
    @Async
    public void handleRegistrationConfirmed(RegistrationConfirmedEvent event) {
        logger.info("✅ Registration confirmed: ID={}, Email={}, ApplicationID={}", 
                   event.getRegistrationId(), event.getUserEmail(), event.getApplicationId());
        
        registrationEventCount.incrementAndGet();
    }

    /**
     * Handle registration cancelled events
     */
    @EventListener
    @Async
    public void handleRegistrationCancelled(RegistrationCancelledEvent event) {
        logger.info("❌ Registration cancelled: ID={}, Email={}, Reason={}", 
                   event.getRegistrationId(), event.getUserEmail(), event.getReason());
        
        registrationEventCount.incrementAndGet();
    }

    /**
     * Handle plan created events
     */
    @EventListener
    @Async
    public void handlePlanCreated(PlanCreatedEvent event) {
        logger.info("🎯 Plan created: ID={}, Name={}", event.getPlanId(), event.getPlanName());
        
        planEventCount.incrementAndGet();
    }

    /**
     * Handle plan updated events
     */
    @EventListener
    @Async
    public void handlePlanUpdated(PlanUpdatedEvent event) {
        logger.info("📝 Plan updated: ID={}, Name={}, UpdatedBy={}", 
                   event.getPlanId(), event.getPlanName(), event.getUpdatedBy());
        
        planEventCount.incrementAndGet();
    }

    /**
     * Handle plan featured events
     */
    @EventListener
    @Async
    public void handlePlanFeatured(PlanFeaturedEvent event) {
        logger.info("⭐ Plan featured: ID={}, Name={}, Previous={}", 
                   event.getPlanId(), event.getPlanName(), event.getPreviousFeaturedPlanId());
        
        planEventCount.incrementAndGet();
    }

    /**
     * Handle file uploaded events
     */
    @EventListener
    @Async
    public void handleFileUploaded(FileUploadedEvent event) {
        logger.info("📁 File uploaded: Name={}, Size={}bytes, Registration={}, Type={}", 
                   event.getFileName(), event.getFileSize(), event.getRegistrationId(), event.getDocumentType());
        
        fileEventCount.incrementAndGet();
    }

    /**
     * Handle file deleted events
     */
    @EventListener
    @Async
    public void handleFileDeleted(FileDeletedEvent event) {
        logger.info("🗑️ File deleted: Name={}, Size={}bytes, Reason={}", 
                   event.getFileName(), event.getFileSize(), event.getReason());
        
        fileEventCount.incrementAndGet();
    }

    /**
     * Handle email sent events
     */
    @EventListener
    @Async
    public void handleEmailSent(EmailSentEvent event) {
        logger.info("📧 Email sent: To={}, Subject={}, QueueID={}", 
                   event.getRecipientEmail(), event.getSubject(), event.getEmailQueueId());
        
        emailEventCount.incrementAndGet();
    }

    /**
     * Handle email failed events
     */
    @EventListener
    @Async
    public void handleEmailFailed(EmailFailedEvent event) {
        logger.warn("📧❌ Email failed: To={}, Subject={}, Error={}, Attempts={}", 
                   event.getRecipientEmail(), event.getSubject(), event.getErrorMessage(), event.getAttemptCount());
        
        emailEventCount.incrementAndGet();
    }

    /**
     * Handle application started events
     */
    @EventListener
    public void handleApplicationStarted(ApplicationStartedEvent event) {
        logger.info("🚀 Application started: Version={}, Profile={}", event.getVersion(), event.getProfile());
        
        systemEventCount.incrementAndGet();
    }

    /**
     * Handle performance threshold exceeded events
     */
    @EventListener
    @Async
    public void handlePerformanceThresholdExceeded(PerformanceThresholdExceededEvent event) {
        logger.warn("⚠️ Performance threshold exceeded: Metric={}, Current={}, Threshold={}", 
                   event.getMetricName(), event.getCurrentValue(), event.getThreshold());
        
        systemEventCount.incrementAndGet();
    }

    /**
     * Handle all application events for general logging
     */
    @EventListener
    @Async
    public void handleAllEvents(ApplicationEvent event) {
        logger.debug("🔔 Event processed: {}", event);
    }

    /**
     * Get event handling statistics
     */
    public EventHandlingStats getStats() {
        return new EventHandlingStats(
            registrationEventCount.get(),
            planEventCount.get(),
            fileEventCount.get(),
            emailEventCount.get(),
            systemEventCount.get()
        );
    }

    /**
     * Reset event counters
     */
    public void resetCounters() {
        registrationEventCount.set(0);
        planEventCount.set(0);
        fileEventCount.set(0);
        emailEventCount.set(0);
        systemEventCount.set(0);
        logger.info("Event handling counters reset");
    }

    /**
     * Event handling statistics
     */
    public static class EventHandlingStats {
        private final long registrationEvents;
        private final long planEvents;
        private final long fileEvents;
        private final long emailEvents;
        private final long systemEvents;
        private final long totalEvents;

        public EventHandlingStats(long registrationEvents, long planEvents, long fileEvents, 
                                long emailEvents, long systemEvents) {
            this.registrationEvents = registrationEvents;
            this.planEvents = planEvents;
            this.fileEvents = fileEvents;
            this.emailEvents = emailEvents;
            this.systemEvents = systemEvents;
            this.totalEvents = registrationEvents + planEvents + fileEvents + emailEvents + systemEvents;
        }

        // Getters
        public long getRegistrationEvents() { return registrationEvents; }
        public long getPlanEvents() { return planEvents; }
        public long getFileEvents() { return fileEvents; }
        public long getEmailEvents() { return emailEvents; }
        public long getSystemEvents() { return systemEvents; }
        public long getTotalEvents() { return totalEvents; }

        @Override
        public String toString() {
            return String.format("EventHandlingStats{registration=%d, plan=%d, file=%d, email=%d, system=%d, total=%d}",
                               registrationEvents, planEvents, fileEvents, emailEvents, systemEvents, totalEvents);
        }
    }
}
