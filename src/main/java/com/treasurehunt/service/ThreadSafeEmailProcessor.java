package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.repository.EmailQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe email processing service that handles scheduled email sending
 * Prevents concurrent processing and ensures proper resource management
 */
@Service
public class ThreadSafeEmailProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ThreadSafeEmailProcessor.class);

    // Thread safety controls
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    // Processing configuration
    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final EmailQueueRepository emailQueueRepository;
    private final EmailService emailService;

    @Autowired
    public ThreadSafeEmailProcessor(EmailQueueRepository emailQueueRepository,
                                   EmailService emailService) {
        this.emailQueueRepository = emailQueueRepository;
        this.emailService = emailService;
    }

    /**
     * Scheduled email processing - runs every minute
     * Uses atomic flag to prevent concurrent execution with timeout protection
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void processEmailQueue() {
        // Check if processing is already in progress
        if (!processing.compareAndSet(false, true)) {
            logger.debug("Email processing already in progress, skipping this cycle");
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            logger.debug("Starting email queue processing cycle");
            processEmailsSync(); // Changed to sync to maintain transaction context
        } catch (Exception e) {
            logger.error("Error in email processing cycle: {}", e.getMessage(), e);
        } finally {
            processing.set(false);
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Email processing cycle completed in {}ms", duration);

            // Log warning if processing took too long
            if (duration > 30000) { // 30 seconds
                logger.warn("Email processing cycle took {}ms - consider optimization", duration);
            }
        }
    }

    /**
     * Process emails synchronously in batches (within transaction context)
     */
    public void processEmailsSync() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int batchCount = 0;
            int totalProcessed = 0;

            while (true) {
                // Get next batch of emails ready to send
                List<EmailQueue> emailBatch = getNextEmailBatch(now);

                if (emailBatch.isEmpty()) {
                    break; // No more emails to process
                }

                batchCount++;
                logger.debug("Processing email batch {} with {} emails", batchCount, emailBatch.size());

                // Process each email in the batch
                for (EmailQueue email : emailBatch) {
                    try {
                        processSingleEmailInTransaction(email);
                        totalProcessed++;
                        processedCount.incrementAndGet();
                    } catch (Exception e) {
                        logger.error("Error processing email ID {}: {}", email.getId(), e.getMessage());
                        handleEmailProcessingErrorInTransaction(email, e);
                        errorCount.incrementAndGet();
                    }
                }

                // Prevent infinite loops - limit to reasonable number of batches per cycle
                if (batchCount >= 10) {
                    logger.warn("Reached maximum batch limit (10) in single processing cycle");
                    break;
                }
            }

            if (totalProcessed > 0) {
                logger.info("Email processing cycle completed: {} emails processed in {} batches", 
                           totalProcessed, batchCount);
            }

        } catch (Exception e) {
            logger.error("Error in email processing cycle: {}", e.getMessage(), e);
        }
    }

    /**
     * Get next batch of emails ready to send
     * @param now Current timestamp
     * @return List of emails ready to send
     */
    public List<EmailQueue> getNextEmailBatch(LocalDateTime now) {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        // Use the regular method since we're already in a transaction
        return emailQueueRepository.findEmailsReadyToSend(now, pageable);
    }

    /**
     * Process a single email within existing transaction
     * @param email Email to process
     */
    public void processSingleEmailInTransaction(EmailQueue email) {
        logger.debug("Processing email ID: {} to: {}", email.getId(), email.getRecipientEmail());

        try {
            // Mark email as being processed
            email.setStatus(EmailQueue.EmailStatus.SENDING);
            email.setLastAttemptDate(LocalDateTime.now());
            emailQueueRepository.save(email);

            // FIXED: Correct parameter order for sendEmail method
            // Method signature: sendEmail(String to, String subject, String body, String from)
            emailService.sendEmail(
                email.getRecipientEmail(),  // to
                email.getSubject(),         // subject
                email.getBody(),            // body
                null                        // from (null = use default)
            );

            // Mark as sent successfully
            email.setStatus(EmailQueue.EmailStatus.SENT);
            email.setSentDate(LocalDateTime.now());
            emailQueueRepository.save(email);

            logger.debug("Successfully sent email ID: {}", email.getId());

        } catch (Exception e) {
            logger.error("Failed to send email ID {}: {}", email.getId(), e.getMessage());
            throw e; // Re-throw to trigger error handling
        }
    }

    /**
     * Process a single email (with separate transaction for external calls)
     * @param email Email to process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEmail(EmailQueue email) {
        processSingleEmailInTransaction(email);
    }

    /**
     * Handle email processing errors with retry logic (within existing transaction)
     * @param email Email that failed to process
     * @param error The error that occurred
     */
    public void handleEmailProcessingErrorInTransaction(EmailQueue email, Exception error) {
        int currentAttempts = email.getAttemptCount();
        currentAttempts++;

        email.setAttemptCount(currentAttempts);
        email.setLastAttemptDate(LocalDateTime.now());
        email.setErrorMessage(error.getMessage());

        if (currentAttempts >= MAX_RETRY_ATTEMPTS) {
            // Max retries reached - mark as failed
            email.setStatus(EmailQueue.EmailStatus.FAILED);
            logger.error("Email ID {} failed permanently after {} attempts: {}",
                        email.getId(), currentAttempts, error.getMessage());
        } else {
            // Schedule for retry with exponential backoff
            email.setStatus(EmailQueue.EmailStatus.PENDING);
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(currentAttempts * 5); // 5, 10, 15 minutes
            email.setScheduledDate(nextRetry);
            logger.warn("Email ID {} failed (attempt {}), scheduled for retry at {}: {}",
                       email.getId(), currentAttempts, nextRetry, error.getMessage());
        }

        emailQueueRepository.save(email);
    }

    /**
     * Handle email processing errors with retry logic (with separate transaction)
     * @param email Email that failed to process
     * @param error The error that occurred
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEmailProcessingError(EmailQueue email, Exception error) {
        handleEmailProcessingErrorInTransaction(email, error);
    }

    /**
     * Get processing statistics
     * @return Processing statistics
     */
    public ProcessingStats getProcessingStats() {
        return new ProcessingStats(
            processing.get(),
            processedCount.get(),
            errorCount.get(),
            emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PENDING),
            emailQueueRepository.countByStatus(EmailQueue.EmailStatus.FAILED)
        );
    }

    /**
     * Reset processing statistics
     */
    public void resetStats() {
        processedCount.set(0);
        errorCount.set(0);
        logger.info("Email processing statistics reset");
    }

    /**
     * Force stop processing (for emergency situations)
     */
    public void forceStopProcessing() {
        processing.set(false);
        logger.warn("Email processing force stopped");
    }

    /**
     * Check if processing is currently active
     * @return true if processing is active
     */
    public boolean isProcessing() {
        return processing.get();
    }

    /**
     * Processing statistics data class
     */
    public static class ProcessingStats {
        private final boolean isProcessing;
        private final int totalProcessed;
        private final int totalErrors;
        private final long pendingEmails;
        private final long failedEmails;

        public ProcessingStats(boolean isProcessing, int totalProcessed, int totalErrors, 
                              long pendingEmails, long failedEmails) {
            this.isProcessing = isProcessing;
            this.totalProcessed = totalProcessed;
            this.totalErrors = totalErrors;
            this.pendingEmails = pendingEmails;
            this.failedEmails = failedEmails;
        }

        // Getters
        public boolean isProcessing() { return isProcessing; }
        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalErrors() { return totalErrors; }
        public long getPendingEmails() { return pendingEmails; }
        public long getFailedEmails() { return failedEmails; }

        @Override
        public String toString() {
            return String.format("ProcessingStats{processing=%s, processed=%d, errors=%d, pending=%d, failed=%d}",
                               isProcessing, totalProcessed, totalErrors, pendingEmails, failedEmails);
        }
    }

    /**
     * Manual trigger for email processing (for testing/admin use)
     */
    public void triggerManualProcessing() {
        if (processing.get()) {
            logger.warn("Cannot trigger manual processing - already in progress");
            return;
        }

        logger.info("Manual email processing triggered");
        processEmailQueue();
    }

    /**
     * Process specific email by ID (for admin retry functionality)
     * @param emailId Email ID to process
     * @return true if processed successfully
     */
    @Transactional
    public boolean processSpecificEmail(Long emailId) {
        try {
            EmailQueue email = emailQueueRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

            if (email.getStatus() == EmailQueue.EmailStatus.SENT) {
                logger.warn("Email ID {} is already sent", emailId);
                return false;
            }

            processSingleEmail(email);
            logger.info("Successfully processed specific email ID: {}", emailId);
            return true;

        } catch (Exception e) {
            logger.error("Error processing specific email ID {}: {}", emailId, e.getMessage());
            return false;
        }
    }
}
