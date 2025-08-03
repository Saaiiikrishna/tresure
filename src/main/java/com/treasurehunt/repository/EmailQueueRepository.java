package com.treasurehunt.repository;

import com.treasurehunt.entity.EmailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for EmailQueue entity
 */
@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {

    /**
     * Find emails ready to be sent (pending or scheduled for now/past)
     */
    @Query("SELECT e FROM EmailQueue e WHERE " +
           "(e.status = 'PENDING' OR (e.status = 'SCHEDULED' AND e.scheduledDate <= :now)) " +
           "ORDER BY e.priority ASC, e.createdDate ASC")
    List<EmailQueue> findEmailsReadyToSend(@Param("now") LocalDateTime now);

    /**
     * Find emails by status
     */
    List<EmailQueue> findByStatusOrderByCreatedDateDesc(EmailQueue.EmailStatus status);

    /**
     * Find emails by status with pagination
     */
    Page<EmailQueue> findByStatusOrderByCreatedDateDesc(EmailQueue.EmailStatus status, Pageable pageable);

    /**
     * Find emails by email type
     */
    List<EmailQueue> findByEmailTypeOrderByCreatedDateDesc(EmailQueue.EmailType emailType);

    /**
     * Find emails by registration ID
     */
    List<EmailQueue> findByRegistrationIdOrderByCreatedDateDesc(Long registrationId);

    /**
     * Find emails by campaign ID
     */
    List<EmailQueue> findByCampaignIdOrderByCreatedDateDesc(String campaignId);

    /**
     * Find emails by campaign ID starting with prefix
     */
    List<EmailQueue> findByCampaignIdStartingWithOrderByCreatedDateDesc(String campaignIdPrefix);

    /**
     * Find failed emails that can be retried
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.status = 'FAILED' AND e.retryCount < e.maxRetryAttempts")
    List<EmailQueue> findFailedEmailsForRetry();

    /**
     * Count emails by status
     */
    long countByStatus(EmailQueue.EmailStatus status);

    /**
     * Count emails by email type
     */
    long countByEmailType(EmailQueue.EmailType emailType);

    /**
     * Count emails by campaign ID
     */
    long countByCampaignId(String campaignId);

    /**
     * Find emails created between dates
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.createdDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdDate DESC")
    List<EmailQueue> findEmailsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find emails sent between dates
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.sentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.sentDate DESC")
    List<EmailQueue> findEmailsSentBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get email statistics by status
     */
    @Query("SELECT e.status, COUNT(e) FROM EmailQueue e GROUP BY e.status")
    List<Object[]> getEmailStatisticsByStatus();

    /**
     * Get email statistics by type
     */
    @Query("SELECT e.emailType, COUNT(e) FROM EmailQueue e GROUP BY e.emailType")
    List<Object[]> getEmailStatisticsByType();

    /**
     * Get daily email statistics for the last N days
     */
    @Query("SELECT DATE(e.createdDate), e.status, COUNT(e) FROM EmailQueue e " +
           "WHERE e.createdDate >= :startDate " +
           "GROUP BY DATE(e.createdDate), e.status " +
           "ORDER BY DATE(e.createdDate) DESC")
    List<Object[]> getDailyEmailStatistics(@Param("startDate") LocalDateTime startDate);

    /**
     * Find emails by recipient email
     */
    List<EmailQueue> findByRecipientEmailOrderByCreatedDateDesc(String recipientEmail);

    /**
     * Find emails by priority
     */
    List<EmailQueue> findByPriorityOrderByCreatedDateDesc(Integer priority);

    /**
     * Find emails scheduled for future
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.status = 'SCHEDULED' AND e.scheduledDate > :now " +
           "ORDER BY e.scheduledDate ASC")
    List<EmailQueue> findScheduledEmails(@Param("now") LocalDateTime now);

    /**
     * Find emails with errors
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.errorMessage IS NOT NULL " +
           "ORDER BY e.createdDate DESC")
    List<EmailQueue> findEmailsWithErrors();

    /**
     * Delete old emails (cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailQueue e WHERE e.createdDate < :cutoffDate AND e.status = 'SENT'")
    void deleteOldSentEmails(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find emails by template name
     */
    List<EmailQueue> findByTemplateNameOrderByCreatedDateDesc(String templateName);
}
