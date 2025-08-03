package com.treasurehunt.repository;

import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.entity.TreasureHuntPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRegistration entity
 * Provides CRUD operations and custom queries for user registrations
 */
@Repository
public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {

    /**
     * Find registrations by plan
     * @param plan Treasure hunt plan
     * @return List of registrations for the plan
     */
    List<UserRegistration> findByPlanOrderByRegistrationDateDesc(TreasureHuntPlan plan);

    /**
     * Find registrations by plan and status
     * @param plan Treasure hunt plan
     * @param status Registration status
     * @return List of registrations matching criteria
     */
    List<UserRegistration> findByPlanAndStatusOrderByRegistrationDateDesc(
            TreasureHuntPlan plan, UserRegistration.RegistrationStatus status);

    /**
     * Find registration by email and plan (to prevent duplicate registrations)
     * @param email User email
     * @param plan Treasure hunt plan
     * @return Optional registration if exists
     */
    Optional<UserRegistration> findByEmailAndPlan(String email, TreasureHuntPlan plan);

    /**
     * Count confirmed registrations for a plan
     * @param plan Treasure hunt plan
     * @return Number of confirmed registrations
     */
    long countByPlanAndStatus(TreasureHuntPlan plan, UserRegistration.RegistrationStatus status);

    /**
     * Find registrations by status
     * @param status Registration status
     * @return List of registrations with specified status
     */
    List<UserRegistration> findByStatusOrderByRegistrationDateDesc(UserRegistration.RegistrationStatus status);

    /**
     * Find registrations within date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of registrations within date range
     */
    List<UserRegistration> findByRegistrationDateBetweenOrderByRegistrationDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find registrations by email (case insensitive)
     * @param email Email to search for
     * @return List of registrations for the email
     */
    List<UserRegistration> findByEmailIgnoreCaseOrderByRegistrationDateDesc(String email);

    /**
     * Find registrations by full name containing (case insensitive)
     * @param name Name to search for
     * @return List of matching registrations
     */
    List<UserRegistration> findByFullNameContainingIgnoreCaseOrderByRegistrationDateDesc(String name);

    /**
     * Count total registrations
     * @return Total number of registrations
     */
    @Query("SELECT COUNT(r) FROM UserRegistration r")
    long countTotalRegistrations();

    /**
     * Count registrations by status
     * @param status Registration status
     * @return Number of registrations with specified status
     */
    long countByStatus(UserRegistration.RegistrationStatus status);

    /**
     * Find old completed registrations for cleanup
     * @param cutoffDate Date before which registrations are considered old
     * @return List of old registrations for completed events
     */
    @Query("SELECT r FROM UserRegistration r JOIN r.plan p WHERE p.eventDate < :cutoffDate AND r.status IN ('CONFIRMED', 'CANCELLED')")
    List<UserRegistration> findOldCompletedRegistrations(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Get registration statistics by plan
     * @return List of plan statistics
     */
    @Query("SELECT r.plan.name, r.plan.id, COUNT(r), r.status " +
           "FROM UserRegistration r " +
           "GROUP BY r.plan.id, r.plan.name, r.status " +
           "ORDER BY r.plan.name, r.status")
    List<Object[]> getRegistrationStatisticsByPlan();

    /**
     * Find recent registrations (last N days)
     * @param daysAgo Number of days ago
     * @return List of recent registrations
     */
    @Query("SELECT r FROM UserRegistration r " +
           "WHERE r.registrationDate >= :cutoffDate " +
           "ORDER BY r.registrationDate DESC")
    List<UserRegistration> findRecentRegistrations(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Check if email is already registered for a specific plan
     */
    Optional<UserRegistration> findByEmailAndPlanId(String email, Long planId);

    /**
     * Find individual registrations (no team name)
     */
    List<UserRegistration> findByTeamNameIsNull();

    /**
     * Find team registrations (has team name)
     */
    List<UserRegistration> findByTeamNameIsNotNull();

    /**
     * Find registrations after a specific date
     */
    List<UserRegistration> findByRegistrationDateAfter(LocalDateTime date);

    /**
     * Find registrations by plan ID
     * @param planId Plan ID
     * @return List of registrations for the plan
     */
    List<UserRegistration> findByPlanIdOrderByRegistrationDateDesc(Long planId);

    /**
     * Find registrations by plan ID and status
     * @param planId Plan ID
     * @param status Registration status
     * @return List of registrations matching criteria
     */
    List<UserRegistration> findByPlanIdAndStatusOrderByRegistrationDateDesc(Long planId, UserRegistration.RegistrationStatus status);

    /**
     * Find registration by ID with team members eagerly loaded
     * @param id Registration ID
     * @return Optional registration with team members
     */
    @Query("SELECT DISTINCT r FROM UserRegistration r LEFT JOIN FETCH r.teamMembers WHERE r.id = :id")
    Optional<UserRegistration> findByIdWithTeamMembers(@Param("id") Long id);

    /**
     * Find registration by ID with documents eagerly loaded
     * @param id Registration ID
     * @return Optional registration with documents
     */
    @Query("SELECT r FROM UserRegistration r LEFT JOIN FETCH r.documents WHERE r.id = :id")
    Optional<UserRegistration> findByIdWithDocuments(@Param("id") Long id);

    /**
     * Count registrations by plan ID and status
     * @param planId Plan ID
     * @param status Registration status
     * @return Count of registrations matching criteria
     */
    long countByPlanIdAndStatus(Long planId, UserRegistration.RegistrationStatus status);

    /**
     * Count all registrations by plan ID (for application ID sequence generation)
     * @param planId Plan ID
     * @return Total count of registrations for the plan
     */
    long countByPlanId(Long planId);
}
