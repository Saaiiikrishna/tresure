package com.treasurehunt.repository;

import com.treasurehunt.entity.EmailCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for EmailCampaign entity
 */
@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    /**
     * Find campaigns by status
     */
    List<EmailCampaign> findByStatusOrderByCreatedDateDesc(EmailCampaign.CampaignStatus status);

    /**
     * Find campaigns by status with pagination
     */
    Page<EmailCampaign> findByStatusOrderByCreatedDateDesc(EmailCampaign.CampaignStatus status, Pageable pageable);

    /**
     * Find campaigns by type
     */
    List<EmailCampaign> findByCampaignTypeOrderByCreatedDateDesc(EmailCampaign.CampaignType campaignType);

    /**
     * Find campaigns by creator
     */
    List<EmailCampaign> findByCreatedByOrderByCreatedDateDesc(String createdBy);

    /**
     * Find campaigns created between dates
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.createdDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.createdDate DESC")
    List<EmailCampaign> findCampaignsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find campaigns sent between dates
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.sentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.sentDate DESC")
    List<EmailCampaign> findCampaignsSentBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find campaigns scheduled for future
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledDate > :now " +
           "ORDER BY c.scheduledDate ASC")
    List<EmailCampaign> findScheduledCampaigns(@Param("now") LocalDateTime now);

    /**
     * Find campaigns ready to be sent
     */
    @Query("SELECT c FROM EmailCampaign c WHERE " +
           "(c.status = 'SCHEDULED' AND c.scheduledDate <= :now) " +
           "ORDER BY c.priority ASC, c.scheduledDate ASC")
    List<EmailCampaign> findCampaignsReadyToSend(@Param("now") LocalDateTime now);

    /**
     * Find active campaigns (sending or scheduled)
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.status IN ('SENDING', 'SCHEDULED') " +
           "ORDER BY c.createdDate DESC")
    List<EmailCampaign> findActiveCampaigns();

    /**
     * Count campaigns by status
     */
    long countByStatus(EmailCampaign.CampaignStatus status);

    /**
     * Count campaigns by type
     */
    long countByCampaignType(EmailCampaign.CampaignType campaignType);

    /**
     * Count campaigns by creator
     */
    long countByCreatedBy(String createdBy);

    /**
     * Get campaign statistics by status
     */
    @Query("SELECT c.status, COUNT(c) FROM EmailCampaign c GROUP BY c.status")
    List<Object[]> getCampaignStatisticsByStatus();

    /**
     * Get campaign statistics by type
     */
    @Query("SELECT c.campaignType, COUNT(c) FROM EmailCampaign c GROUP BY c.campaignType")
    List<Object[]> getCampaignStatisticsByType();

    /**
     * Get monthly campaign statistics
     */
    @Query("SELECT YEAR(c.createdDate), MONTH(c.createdDate), COUNT(c) FROM EmailCampaign c " +
           "WHERE c.createdDate >= :startDate " +
           "GROUP BY YEAR(c.createdDate), MONTH(c.createdDate) " +
           "ORDER BY YEAR(c.createdDate) DESC, MONTH(c.createdDate) DESC")
    List<Object[]> getMonthlyCampaignStatistics(@Param("startDate") LocalDateTime startDate);

    /**
     * Find campaigns by name (case insensitive)
     */
    @Query("SELECT c FROM EmailCampaign c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY c.createdDate DESC")
    List<EmailCampaign> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find campaigns by target audience
     */
    List<EmailCampaign> findByTargetAudienceOrderByCreatedDateDesc(String targetAudience);

    /**
     * Find campaigns with high success rate
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.status = 'SENT' AND " +
           "(CAST(c.emailsSent AS double) / CAST(c.totalRecipients AS double)) >= :minSuccessRate " +
           "ORDER BY (CAST(c.emailsSent AS double) / CAST(c.totalRecipients AS double)) DESC")
    List<EmailCampaign> findCampaignsWithHighSuccessRate(@Param("minSuccessRate") double minSuccessRate);

    /**
     * Find recent campaigns (last N days)
     */
    @Query("SELECT c FROM EmailCampaign c WHERE c.createdDate >= :cutoffDate " +
           "ORDER BY c.createdDate DESC")
    List<EmailCampaign> findRecentCampaigns(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find campaigns by template name
     */
    List<EmailCampaign> findByTemplateNameOrderByCreatedDateDesc(String templateName);
}
