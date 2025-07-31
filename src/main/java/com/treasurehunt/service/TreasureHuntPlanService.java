package com.treasurehunt.service;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.repository.TreasureHuntPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing Treasure Hunt Plans
 * Handles business logic for CRUD operations on treasure hunt plans
 */
@Service
@Transactional
public class TreasureHuntPlanService {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntPlanService.class);

    private final TreasureHuntPlanRepository planRepository;

    @Autowired
    public TreasureHuntPlanService(TreasureHuntPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Get all active treasure hunt plans
     * @return List of active plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getAllActivePlans() {
        logger.debug("Fetching all active treasure hunt plans");
        return planRepository.findByStatusOrderByCreatedDateDesc(TreasureHuntPlan.PlanStatus.ACTIVE);
    }

    /**
     * Get all available plans (active with available spots)
     * @return List of available plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getAvailablePlans() {
        logger.debug("Fetching available treasure hunt plans");
        return planRepository.findAvailablePlans();
    }

    /**
     * Get plan by ID
     * @param id Plan ID
     * @return Optional plan
     */
    @Transactional(readOnly = true)
    public Optional<TreasureHuntPlan> getPlanById(Long id) {
        logger.debug("Fetching treasure hunt plan with ID: {}", id);
        return planRepository.findById(id);
    }

    /**
     * Get available plan by ID (active and has spots)
     * @param id Plan ID
     * @return Optional available plan
     */
    @Transactional(readOnly = true)
    public Optional<TreasureHuntPlan> getAvailablePlanById(Long id) {
        logger.debug("Fetching available treasure hunt plan with ID: {}", id);
        return planRepository.findAvailablePlanById(id);
    }

    /**
     * Get all plans (for admin)
     * @return List of all plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getAllPlans() {
        logger.debug("Fetching all treasure hunt plans for admin");
        return planRepository.findAll();
    }

    /**
     * Create a new treasure hunt plan
     * @param plan Plan to create
     * @return Created plan
     */
    public TreasureHuntPlan createPlan(TreasureHuntPlan plan) {
        logger.info("Creating new treasure hunt plan: {}", plan.getName());
        
        // Validate plan data
        validatePlan(plan);
        
        // Set default status if not provided
        if (plan.getStatus() == null) {
            plan.setStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
        }
        
        TreasureHuntPlan savedPlan = planRepository.save(plan);
        logger.info("Successfully created treasure hunt plan with ID: {}", savedPlan.getId());
        
        return savedPlan;
    }

    /**
     * Update an existing treasure hunt plan
     * @param id Plan ID
     * @param updatedPlan Updated plan data
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found
     */
    public TreasureHuntPlan updatePlan(Long id, TreasureHuntPlan updatedPlan) {
        logger.info("Updating treasure hunt plan with ID: {}", id);
        
        TreasureHuntPlan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));
        
        // Validate updated plan data
        validatePlan(updatedPlan);
        
        // Update fields
        existingPlan.setName(updatedPlan.getName());
        existingPlan.setDescription(updatedPlan.getDescription());
        existingPlan.setDurationHours(updatedPlan.getDurationHours());
        existingPlan.setDifficultyLevel(updatedPlan.getDifficultyLevel());
        existingPlan.setMaxParticipants(updatedPlan.getMaxParticipants());
        existingPlan.setPriceUsd(updatedPlan.getPriceUsd());
        existingPlan.setStatus(updatedPlan.getStatus());
        
        TreasureHuntPlan savedPlan = planRepository.save(existingPlan);
        logger.info("Successfully updated treasure hunt plan with ID: {}", savedPlan.getId());
        
        return savedPlan;
    }

    /**
     * Toggle plan status (active/inactive)
     * @param id Plan ID
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found
     */
    public TreasureHuntPlan togglePlanStatus(Long id) {
        logger.info("Toggling status for treasure hunt plan with ID: {}", id);
        
        TreasureHuntPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));
        
        TreasureHuntPlan.PlanStatus newStatus = plan.getStatus() == TreasureHuntPlan.PlanStatus.ACTIVE
                ? TreasureHuntPlan.PlanStatus.INACTIVE
                : TreasureHuntPlan.PlanStatus.ACTIVE;
        
        plan.setStatus(newStatus);
        TreasureHuntPlan savedPlan = planRepository.save(plan);
        
        logger.info("Successfully toggled status to {} for plan ID: {}", newStatus, id);
        return savedPlan;
    }

    /**
     * Delete a treasure hunt plan
     * @param id Plan ID
     * @throws IllegalArgumentException if plan not found
     */
    public void deletePlan(Long id) {
        logger.info("Deleting treasure hunt plan with ID: {}", id);
        
        if (!planRepository.existsById(id)) {
            throw new IllegalArgumentException("Plan not found with ID: " + id);
        }
        
        planRepository.deleteById(id);
        logger.info("Successfully deleted treasure hunt plan with ID: {}", id);
    }

    /**
     * Get plans by difficulty level
     * @param difficultyLevel Difficulty level
     * @return List of plans with specified difficulty
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getPlansByDifficulty(TreasureHuntPlan.DifficultyLevel difficultyLevel) {
        logger.debug("Fetching plans with difficulty level: {}", difficultyLevel);
        return planRepository.findByDifficultyLevelAndStatusOrderByPriceUsdAsc(
                difficultyLevel, TreasureHuntPlan.PlanStatus.ACTIVE);
    }

    /**
     * Search plans by name
     * @param name Name to search for
     * @return List of matching plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> searchPlansByName(String name) {
        logger.debug("Searching plans by name: {}", name);
        return planRepository.findByNameContainingIgnoreCaseAndStatusOrderByNameAsc(
                name, TreasureHuntPlan.PlanStatus.ACTIVE);
    }

    /**
     * Get total count of active plans
     * @return Number of active plans
     */
    @Transactional(readOnly = true)
    public long getActivePlanCount() {
        return planRepository.countByStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
    }

    /**
     * Validate plan data
     * @param plan Plan to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePlan(TreasureHuntPlan plan) {
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        
        if (plan.getDurationHours() == null || plan.getDurationHours() < 1 || plan.getDurationHours() > 24) {
            throw new IllegalArgumentException("Duration must be between 1 and 24 hours");
        }
        
        if (plan.getMaxParticipants() == null || plan.getMaxParticipants() < 1 || plan.getMaxParticipants() > 100) {
            throw new IllegalArgumentException("Maximum participants must be between 1 and 100");
        }
        
        if (plan.getPriceUsd() == null || plan.getPriceUsd().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        
        if (plan.getDifficultyLevel() == null) {
            throw new IllegalArgumentException("Difficulty level is required");
        }
    }
}
