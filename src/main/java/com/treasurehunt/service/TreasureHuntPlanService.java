package com.treasurehunt.service;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.TreasureHuntPlanRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
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
    private final UserRegistrationRepository registrationRepository;

    @Autowired
    public TreasureHuntPlanService(TreasureHuntPlanRepository planRepository,
                                   UserRegistrationRepository registrationRepository) {
        this.planRepository = planRepository;
        this.registrationRepository = registrationRepository;
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
        List<TreasureHuntPlan> plans = planRepository.findAvailablePlans();

        // Set confirmed registrations count for each plan to avoid lazy loading issues
        for (TreasureHuntPlan plan : plans) {
            long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                plan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
            plan.setConfirmedRegistrationsCount(confirmedCount);
        }

        return plans;
    }

    /**
     * Get all active treasure hunt plans
     * @return List of active plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getActivePlans() {
        logger.debug("Fetching all active treasure hunt plans");
        return planRepository.findByStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
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
        List<TreasureHuntPlan> plans = planRepository.findAll();

        // Add confirmed registrations count to each plan
        for (TreasureHuntPlan plan : plans) {
            long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                plan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
            plan.setConfirmedRegistrationsCount(confirmedCount);
        }

        return plans;
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
        existingPlan.setPriceInr(updatedPlan.getPriceInr());
        existingPlan.setPrizeMoney(updatedPlan.getPrizeMoney());
        existingPlan.setStatus(updatedPlan.getStatus());
        
        TreasureHuntPlan savedPlan = planRepository.save(existingPlan);
        logger.info("Successfully updated treasure hunt plan with ID: {}", savedPlan.getId());
        
        return savedPlan;
    }

    /**
     * Update an existing treasure hunt plan
     * @param plan Plan with updated data (must have ID set)
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found
     */
    public TreasureHuntPlan updatePlan(TreasureHuntPlan plan) {
        if (plan.getId() == null) {
            throw new IllegalArgumentException("Plan ID must be set for update operation");
        }

        logger.info("Updating treasure hunt plan with ID: {}", plan.getId());

        TreasureHuntPlan existingPlan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + plan.getId()));

        // Validate updated plan data
        validatePlan(plan);

        // Update fields
        existingPlan.setName(plan.getName());
        existingPlan.setDescription(plan.getDescription());
        existingPlan.setDurationHours(plan.getDurationHours());
        existingPlan.setDifficultyLevel(plan.getDifficultyLevel());
        existingPlan.setMaxParticipants(plan.getMaxParticipants());
        existingPlan.setTeamSize(plan.getTeamSize());
        existingPlan.setTeamType(plan.getTeamType());
        existingPlan.setPriceInr(plan.getPriceInr());
        existingPlan.setPrizeMoney(plan.getPrizeMoney());
        existingPlan.setBatchesCompleted(plan.getBatchesCompleted());
        existingPlan.setRating(plan.getRating());
        existingPlan.setAvailableSlots(plan.getAvailableSlots());
        existingPlan.setPreviewVideoUrl(plan.getPreviewVideoUrl());

        TreasureHuntPlan savedPlan = planRepository.save(existingPlan);
        logger.info("Successfully updated treasure hunt plan with ID: {}", savedPlan.getId());

        return savedPlan;
    }

    /**
     * Get the featured plan for hero section
     * @return Featured plan or null if none is featured
     */
    @Transactional(readOnly = true)
    public TreasureHuntPlan getFeaturedPlan() {
        logger.debug("Fetching featured plan");
        TreasureHuntPlan featuredPlan = planRepository.findByIsFeaturedTrueAndStatus(TreasureHuntPlan.PlanStatus.ACTIVE)
                .orElse(null);

        if (featuredPlan != null) {
            // Set confirmed registrations count to avoid lazy loading issues
            long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                featuredPlan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
            featuredPlan.setConfirmedRegistrationsCount(confirmedCount);
        }

        return featuredPlan;
    }

    /**
     * Set a plan as featured (unsets any previously featured plan)
     * @param planId Plan ID to set as featured
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found
     */
    public TreasureHuntPlan setFeaturedPlan(Long planId) {
        logger.info("Setting plan {} as featured", planId);

        TreasureHuntPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));

        // Unset any previously featured plan
        planRepository.findByIsFeaturedTrue().ifPresent(currentFeatured -> {
            currentFeatured.setIsFeatured(false);
            planRepository.save(currentFeatured);
            logger.info("Unfeatured previous plan with ID: {}", currentFeatured.getId());
        });

        // Set the new featured plan
        plan.setIsFeatured(true);
        TreasureHuntPlan savedPlan = planRepository.save(plan);

        logger.info("Successfully set plan {} as featured", planId);
        return savedPlan;
    }

    /**
     * Update available slots for a plan
     * @param planId Plan ID
     * @param availableSlots New available slots value
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found or invalid slots value
     */
    public TreasureHuntPlan updateAvailableSlots(Long planId, Integer availableSlots) {
        logger.info("Updating available slots for plan {} to {}", planId, availableSlots);

        if (availableSlots < 0) {
            throw new IllegalArgumentException("Available slots must be non-negative");
        }

        TreasureHuntPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));

        plan.setAvailableSlots(availableSlots);
        TreasureHuntPlan savedPlan = planRepository.save(plan);

        logger.info("Successfully updated available slots for plan {} to {}", planId, availableSlots);
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
     * @throws IllegalArgumentException if plan not found or has existing registrations
     */
    public void deletePlan(Long id) {
        logger.info("Deleting treasure hunt plan with ID: {}", id);

        TreasureHuntPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));

        // Check if plan has any registrations
        if (!plan.getRegistrations().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete plan with existing registrations. " +
                "Plan has " + plan.getRegistrations().size() + " registration(s). " +
                "Please cancel all registrations first or set plan status to INACTIVE.");
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
        return planRepository.findByDifficultyLevelAndStatusOrderByPriceInrAsc(
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
        
        if (plan.getDurationHours() == null || plan.getDurationHours() < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 hour");
        }
        
        if (plan.getMaxParticipants() == null || plan.getMaxParticipants() < 1 || plan.getMaxParticipants() > 100) {
            throw new IllegalArgumentException("Maximum participants must be between 1 and 100");
        }
        
        if (plan.getPriceInr() == null || plan.getPriceInr().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        
        if (plan.getDifficultyLevel() == null) {
            throw new IllegalArgumentException("Difficulty level is required");
        }
    }
}
