package com.treasurehunt.repository;

import com.treasurehunt.entity.TreasureHuntPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TreasureHuntPlan entity
 * Provides CRUD operations and custom queries for treasure hunt plans
 */
@Repository
public interface TreasureHuntPlanRepository extends JpaRepository<TreasureHuntPlan, Long> {

    /**
     * Find all active treasure hunt plans
     * @return List of active plans
     */
    List<TreasureHuntPlan> findByStatusOrderByCreatedDateDesc(TreasureHuntPlan.PlanStatus status);

    /**
     * Find active plans with available spots
     * @return List of available active plans
     */
    @Query("SELECT p FROM TreasureHuntPlan p WHERE p.status = 'ACTIVE' " +
           "AND (SELECT COUNT(r) FROM UserRegistration r WHERE r.plan = p AND r.status = 'CONFIRMED') < p.maxParticipants " +
           "ORDER BY " +
           "CASE p.difficultyLevel " +
           "  WHEN 'BEGINNER' THEN 1 " +
           "  WHEN 'INTERMEDIATE' THEN 2 " +
           "  WHEN 'ADVANCED' THEN 3 " +
           "  ELSE 4 " +
           "END, p.createdDate ASC")
    List<TreasureHuntPlan> findAvailablePlans();

    /**
     * Find plan by ID if it's active and available
     * @param id Plan ID
     * @return Optional plan if available
     */
    @Query("SELECT p FROM TreasureHuntPlan p WHERE p.id = :id AND p.status = 'ACTIVE' " +
           "AND (SELECT COUNT(r) FROM UserRegistration r WHERE r.plan = p AND r.status = 'CONFIRMED') < p.maxParticipants")
    Optional<TreasureHuntPlan> findAvailablePlanById(@Param("id") Long id);

    /**
     * Find plans by difficulty level
     * @param difficultyLevel Difficulty level
     * @return List of plans with specified difficulty
     */
    List<TreasureHuntPlan> findByDifficultyLevelAndStatusOrderByPriceInrAsc(
            TreasureHuntPlan.DifficultyLevel difficultyLevel,
            TreasureHuntPlan.PlanStatus status);

    /**
     * Count total active plans
     * @return Number of active plans
     */
    long countByStatus(TreasureHuntPlan.PlanStatus status);

    /**
     * Get plan with registration count
     * @param planId Plan ID
     * @return Plan with registration statistics
     */
    @Query("SELECT p, COUNT(r) as registrationCount FROM TreasureHuntPlan p " +
           "LEFT JOIN p.registrations r ON r.status = 'CONFIRMED' " +
           "WHERE p.id = :planId GROUP BY p")
    Object[] findPlanWithRegistrationCount(@Param("planId") Long planId);

    /**
     * Find plans by name containing (case insensitive)
     * @param name Name to search for
     * @return List of matching plans
     */
    List<TreasureHuntPlan> findByNameContainingIgnoreCaseAndStatusOrderByNameAsc(
            String name, TreasureHuntPlan.PlanStatus status);

    /**
     * Find the featured plan with active status
     * @param status Plan status
     * @return Optional featured plan
     */
    Optional<TreasureHuntPlan> findByIsFeaturedTrueAndStatus(TreasureHuntPlan.PlanStatus status);

    /**
     * Find any featured plan (regardless of status)
     * @return Optional featured plan
     */
    Optional<TreasureHuntPlan> findByIsFeaturedTrue();

    /**
     * Find all plans with a specific status
     * @param status Plan status
     * @return List of plans with the specified status
     */
    List<TreasureHuntPlan> findByStatus(TreasureHuntPlan.PlanStatus status);
}
