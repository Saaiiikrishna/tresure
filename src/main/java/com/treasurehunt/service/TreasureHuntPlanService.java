package com.treasurehunt.service;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.repository.TreasureHuntPlanRepository;
import com.treasurehunt.repository.UserRegistrationRepository;
import com.treasurehunt.service.interfaces.TreasureHuntPlanServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

/**
 * Service class for managing Treasure Hunt Plans
 * Handles business logic for CRUD operations on treasure hunt plans
 */
@Service
@Transactional
public class TreasureHuntPlanService implements TreasureHuntPlanServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntPlanService.class);

    private final TreasureHuntPlanRepository planRepository;
    private final UserRegistrationRepository registrationRepository;

    // In-memory cache for plans to reduce database calls
    private final ConcurrentHashMap<String, List<TreasureHuntPlan>> plansCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TreasureHuntPlan> planByIdCache = new ConcurrentHashMap<>();
    private volatile TreasureHuntPlan featuredPlanCache = null;
    private volatile long lastCacheRefresh = 0;
    private static final long CACHE_REFRESH_INTERVAL = 180000; // 3 minutes

    @Autowired
    public TreasureHuntPlanService(TreasureHuntPlanRepository planRepository,
                                   UserRegistrationRepository registrationRepository) {
        this.planRepository = planRepository;
        this.registrationRepository = registrationRepository;
    }

    /**
     * PERFORMANCE FIX: Load all plans into cache on startup with optimized queries
     */
    @PostConstruct
    public void loadPlansIntoCache() {
        try {
            logger.info("Loading all plans into cache...");
            long startTime = System.currentTimeMillis();

            // PERFORMANCE FIX: Use optimized query with timeout
            List<TreasureHuntPlan> activePlans;
            try {
                activePlans = planRepository.findByStatusOrderByCreatedDateDesc(TreasureHuntPlan.PlanStatus.ACTIVE);
            } catch (Exception e) {
                logger.warn("Database query for plans failed during startup, using empty cache: {}", e.getMessage());
                activePlans = new ArrayList<>();
            }

            plansCache.put("active", activePlans);

            // Load all plans by ID
            planByIdCache.clear();
            for (TreasureHuntPlan plan : activePlans) {
                planByIdCache.put(plan.getId(), plan);
            }

            // PERFORMANCE FIX: Load featured plan with fallback
            try {
                featuredPlanCache = planRepository.findByIsFeaturedTrueAndStatus(TreasureHuntPlan.PlanStatus.ACTIVE)
                        .orElse(activePlans.isEmpty() ? null : activePlans.get(0));
            } catch (Exception e) {
                logger.warn("Featured plan query failed, using first active plan as fallback: {}", e.getMessage());
                featuredPlanCache = activePlans.isEmpty() ? null : activePlans.get(0);
            }

            lastCacheRefresh = System.currentTimeMillis();
            long endTime = System.currentTimeMillis();
            logger.info("✅ Loaded {} active plans into cache in {}ms", activePlans.size(), (endTime - startTime));

        } catch (Exception e) {
            logger.error("Error loading plans into cache", e);
            // Initialize empty cache to prevent null pointer exceptions
            plansCache.put("active", new ArrayList<>());
            featuredPlanCache = null;
        }
    }

    /**
     * Refresh cache if it's older than the refresh interval
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheRefresh > CACHE_REFRESH_INTERVAL) {
            logger.debug("Cache refresh interval exceeded, refreshing plans cache");
            loadPlansIntoCache();
        }
    }

    /**
     * Get all active treasure hunt plans with in-memory caching (no database calls for cached values)
     * @return List of active plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getAllActivePlans() {
        refreshCacheIfNeeded();

        List<TreasureHuntPlan> cachedPlans = plansCache.get("active");
        if (cachedPlans != null && !cachedPlans.isEmpty()) {
            logger.debug("Returning {} active plans from cache", cachedPlans.size());
            return new ArrayList<>(cachedPlans); // Return copy to prevent modification
        }

        // Fallback to database if cache is empty
        logger.debug("Cache miss - fetching active plans from database");
        List<TreasureHuntPlan> plans = planRepository.findByStatusOrderByCreatedDateDesc(TreasureHuntPlan.PlanStatus.ACTIVE);
        plansCache.put("active", plans);
        return plans;
    }

    /**
     * Get all available plans (active with available spots) with caching
     * @return List of available plans
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "treasureHuntPlans", key = "'available'")
    public List<TreasureHuntPlan> getAvailablePlans() {
        logger.debug("Fetching available treasure hunt plans from database");
        List<TreasureHuntPlan> plans = planRepository.findAvailablePlans();

        // Set confirmed registrations count for each plan to avoid lazy loading issues
        for (TreasureHuntPlan plan : plans) {
            long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                plan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
            plan.setConfirmedRegistrationsCount(confirmedCount);
        }

        logger.debug("Loaded {} available plans into cache", plans.size());
        return plans;
    }

    /**
     * Get all active treasure hunt plans
     * @return List of active plans
     */
    @Transactional(readOnly = true)
    public List<TreasureHuntPlan> getActivePlans() {
        logger.debug("Fetching all active treasure hunt plans");
        try {
            List<TreasureHuntPlan> plans = planRepository.findByStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
            logger.debug("Found {} active plans", plans.size());
            return plans;
        } catch (Exception e) {
            logger.error("Error fetching active plans", e);
            return new ArrayList<>();
        }
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
    @CacheEvict(value = {"treasureHuntPlans", "featuredPlan"}, allEntries = true)
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
    @CacheEvict(value = {"treasureHuntPlans", "featuredPlan"}, allEntries = true)
    public TreasureHuntPlan updatePlan(Long id, TreasureHuntPlan updatedPlan) {
        logger.info("Updating treasure hunt plan with ID: {}", id);

        TreasureHuntPlan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));

        // Validate updated plan data
        validatePlan(updatedPlan);

        // Update all fields comprehensively
        updatePlanFields(existingPlan, updatedPlan);

        TreasureHuntPlan savedPlan = planRepository.save(existingPlan);
        logger.info("Successfully updated treasure hunt plan with ID: {}", savedPlan.getId());

        return savedPlan;
    }

    /**
     * Update an existing treasure hunt plan using entity with ID
     * @param plan Plan with updated data (must have ID set)
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found or ID is null
     */
    public TreasureHuntPlan updatePlan(TreasureHuntPlan plan) {
        if (plan.getId() == null) {
            throw new IllegalArgumentException("Plan ID must be set for update operation");
        }

        // Delegate to the ID-based method to avoid code duplication
        return updatePlan(plan.getId(), plan);
    }

    /**
     * Helper method to update plan fields - centralized field updating logic
     * @param existingPlan The plan to update
     * @param updatedPlan The plan with new values
     */
    private void updatePlanFields(TreasureHuntPlan existingPlan, TreasureHuntPlan updatedPlan) {
        // Basic plan information
        existingPlan.setName(updatedPlan.getName());
        existingPlan.setDescription(updatedPlan.getDescription());
        existingPlan.setDurationHours(updatedPlan.getDurationHours());
        existingPlan.setDifficultyLevel(updatedPlan.getDifficultyLevel());
        existingPlan.setMaxParticipants(updatedPlan.getMaxParticipants());
        existingPlan.setPriceInr(updatedPlan.getPriceInr());
        existingPlan.setPrizeMoney(updatedPlan.getPrizeMoney());
        existingPlan.setStatus(updatedPlan.getStatus());

        // Extended plan information (only update if not null)
        if (updatedPlan.getTeamSize() != null) {
            existingPlan.setTeamSize(updatedPlan.getTeamSize());
        }
        if (updatedPlan.getTeamType() != null) {
            existingPlan.setTeamType(updatedPlan.getTeamType());
        }
        if (updatedPlan.getBatchesCompleted() != null) {
            existingPlan.setBatchesCompleted(updatedPlan.getBatchesCompleted());
        }
        if (updatedPlan.getRating() != null) {
            existingPlan.setRating(updatedPlan.getRating());
        }
        if (updatedPlan.getAvailableSlots() != null) {
            existingPlan.setAvailableSlots(updatedPlan.getAvailableSlots());
        }
        if (updatedPlan.getPreviewVideoUrl() != null) {
            existingPlan.setPreviewVideoUrl(updatedPlan.getPreviewVideoUrl());
        }
    }

    /**
     * Get the featured plan for hero section with caching
     * Always returns a plan - either the explicitly featured one or a default fallback
     * @return Featured plan or default plan (never null)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "featuredPlan", key = "'featured'")
    public TreasureHuntPlan getFeaturedPlan() {
        refreshCacheIfNeeded();

        // Check cache first
        if (featuredPlanCache != null) {
            logger.debug("Returning featured plan from cache: {}", featuredPlanCache.getName());
            return featuredPlanCache;
        }

        logger.debug("Cache miss - fetching featured plan from database");
        try {
            // First, try to get the explicitly featured plan
            TreasureHuntPlan featuredPlan = planRepository.findByIsFeaturedTrueAndStatus(TreasureHuntPlan.PlanStatus.ACTIVE)
                    .orElse(null);

            if (featuredPlan != null) {
                // Set confirmed registrations count to avoid lazy loading issues
                try {
                    long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                        featuredPlan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
                    featuredPlan.setConfirmedRegistrationsCount(confirmedCount);
                    logger.debug("Featured plan {} has {} confirmed registrations", featuredPlan.getName(), confirmedCount);
                } catch (Exception e) {
                    logger.warn("Error loading registration count for featured plan {}", featuredPlan.getId(), e);
                    featuredPlan.setConfirmedRegistrationsCount(0L);
                }
                return featuredPlan;
            }

            // No explicitly featured plan found, get a default fallback
            logger.debug("No explicitly featured plan found, selecting default fallback");
            TreasureHuntPlan defaultPlan = getDefaultFeaturedPlan();

            if (defaultPlan != null) {
                logger.info("Using default featured plan: {} (ID: {})", defaultPlan.getName(), defaultPlan.getId());
                return defaultPlan;
            }

            // If still no plan found, create an emergency fallback
            logger.warn("No active plans available, creating emergency fallback plan");
            return createEmergencyFallbackPlan();

        } catch (Exception e) {
            logger.error("Error fetching featured plan, creating emergency fallback", e);
            return createEmergencyFallbackPlan();
        }
    }

    /**
     * Get a default featured plan when no plan is explicitly featured
     * Selects the most suitable active plan based on business criteria
     * @return Default plan or null if no active plans exist
     */
    private TreasureHuntPlan getDefaultFeaturedPlan() {
        try {
            // Strategy 1: Get the most popular plan (highest rating and most registrations)
            List<TreasureHuntPlan> activePlans = planRepository.findByStatusOrderByCreatedDateDesc(TreasureHuntPlan.PlanStatus.ACTIVE);

            if (activePlans.isEmpty()) {
                return null;
            }

            // Find the best plan based on rating and availability
            TreasureHuntPlan bestPlan = activePlans.stream()
                .filter(plan -> plan.getRating() != null && plan.getRating().compareTo(java.math.BigDecimal.ZERO) > 0)
                .max((p1, p2) -> {
                    // First compare by rating
                    int ratingCompare = p1.getRating().compareTo(p2.getRating());
                    if (ratingCompare != 0) return ratingCompare;

                    // Then by availability (more available slots is better)
                    if (p1.getAvailableSlots() != null && p2.getAvailableSlots() != null) {
                        return p1.getAvailableSlots().compareTo(p2.getAvailableSlots());
                    }

                    // Finally by creation date (newer is better)
                    return p1.getCreatedDate().compareTo(p2.getCreatedDate());
                })
                .orElse(activePlans.get(0)); // Fallback to first active plan

            // Set confirmed registrations count
            if (bestPlan != null) {
                try {
                    long confirmedCount = registrationRepository.countByPlanIdAndStatus(
                        bestPlan.getId(), UserRegistration.RegistrationStatus.CONFIRMED);
                    bestPlan.setConfirmedRegistrationsCount(confirmedCount);
                } catch (Exception e) {
                    logger.warn("Error loading registration count for default plan {}", bestPlan.getId(), e);
                    bestPlan.setConfirmedRegistrationsCount(0L);
                }
            }

            return bestPlan;

        } catch (Exception e) {
            logger.error("Error selecting default featured plan", e);
            return null;
        }
    }

    /**
     * Create an emergency fallback plan when no active plans exist
     * This ensures the application never fails due to missing featured plan
     * @return Emergency fallback plan
     */
    private TreasureHuntPlan createEmergencyFallbackPlan() {
        TreasureHuntPlan emergencyPlan = new TreasureHuntPlan();
        emergencyPlan.setId(-1L); // Special ID to indicate this is a fallback
        emergencyPlan.setName("Coming Soon - Exciting Adventures Await!");
        emergencyPlan.setDescription("We're preparing amazing treasure hunt experiences for you. Check back soon for new adventures!");
        emergencyPlan.setPriceInr(new java.math.BigDecimal("1500.00"));
        emergencyPlan.setDifficultyLevel(TreasureHuntPlan.DifficultyLevel.BEGINNER);
        emergencyPlan.setDurationHours(3);
        emergencyPlan.setMaxParticipants(6);
        emergencyPlan.setAvailableSlots(50);
        emergencyPlan.setTeamSize(4);
        emergencyPlan.setPrizeMoney(new java.math.BigDecimal("5000.00"));
        emergencyPlan.setRating(new java.math.BigDecimal("4.5"));
        emergencyPlan.setBatchesCompleted(0);
        emergencyPlan.setStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
        emergencyPlan.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        emergencyPlan.setIsFeatured(false);
        emergencyPlan.setDiscountEnabled(false);
        emergencyPlan.setCreatedDate(java.time.LocalDateTime.now());
        emergencyPlan.setConfirmedRegistrationsCount(0L);

        logger.info("Created emergency fallback featured plan");
        return emergencyPlan;
    }

    /**
     * Set a plan as featured (unsets any previously featured plan)
     * Uses atomic transaction to prevent race conditions
     * Updates in-memory cache immediately
     * @param planId Plan ID to set as featured
     * @return Updated plan
     * @throws IllegalArgumentException if plan not found
     */
    @Transactional
    @CacheEvict(value = {"treasureHuntPlans", "featuredPlan"}, allEntries = true)
    public TreasureHuntPlan setFeaturedPlan(Long planId) {
        logger.info("Setting plan {} as featured", planId);

        TreasureHuntPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));

        // Atomic operation: First unset all featured plans, then set the new one
        // This prevents temporary states where multiple plans are featured
        int unfeaturedCount = planRepository.updateAllFeaturedPlansToFalse();
        if (unfeaturedCount > 0) {
            logger.info("Unfeatured {} previous plan(s)", unfeaturedCount);
        }

        // Set the new featured plan
        plan.setIsFeatured(true);
        TreasureHuntPlan savedPlan = planRepository.save(plan);

        // Update cache immediately
        featuredPlanCache = savedPlan;
        loadPlansIntoCache(); // Refresh entire cache to ensure consistency

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

        // Comprehensive input validation
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        if (availableSlots == null) {
            throw new IllegalArgumentException("Available slots cannot be null");
        }
        if (availableSlots < 0) {
            throw new IllegalArgumentException("Available slots must be non-negative");
        }
        if (availableSlots > 10000) { // Reasonable upper limit
            throw new IllegalArgumentException("Available slots cannot exceed 10,000");
        }

        TreasureHuntPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));

        // Additional business logic validation
        if (plan.getMaxParticipants() != null && availableSlots > plan.getMaxParticipants()) {
            logger.warn("Available slots ({}) exceeds max participants ({}) for plan {}",
                       availableSlots, plan.getMaxParticipants(), planId);
        }

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
    @CacheEvict(value = {"treasureHuntPlans", "featuredPlan"}, allEntries = true)
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
     * Uses atomic check to prevent race conditions
     * @param id Plan ID
     * @throws IllegalArgumentException if plan not found or has existing registrations
     */
    @Transactional
    @CacheEvict(value = {"treasureHuntPlans", "featuredPlan"}, allEntries = true)
    public void deletePlan(Long id) {
        logger.info("Deleting treasure hunt plan with ID: {}", id);

        TreasureHuntPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));

        // Atomic check for registrations using repository query to avoid race conditions
        long registrationCount = registrationRepository.countByPlanId(id);
        if (registrationCount > 0) {
            throw new IllegalArgumentException("Cannot delete plan with existing registrations. " +
                "Plan has " + registrationCount + " registration(s). " +
                "Please cancel all registrations first or set plan status to INACTIVE.");
        }

        // Double-check within the same transaction before deletion
        registrationCount = registrationRepository.countByPlanId(id);
        if (registrationCount > 0) {
            throw new IllegalArgumentException("Cannot delete plan - registrations were added during operation");
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
    @Transactional(readOnly = true, timeout = 5)
    public long getActivePlanCount() {
        try {
            return planRepository.countByStatus(TreasureHuntPlan.PlanStatus.ACTIVE);
        } catch (Exception e) {
            logger.error("Error getting active plan count", e);
            return 0L; // Return 0 instead of throwing exception for health checks
        }
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

    // ===== INTERFACE IMPLEMENTATION METHODS =====

    @Override
    public PlanStatistics getPlanStatistics(Long planId) {
        logger.debug("Getting statistics for plan: {}", planId);

        TreasureHuntPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        List<UserRegistration> registrations = registrationRepository.findByPlanIdWithTeamMembers(planId);

        int totalRegistrations = registrations.size();
        int confirmedRegistrations = (int) registrations.stream()
            .filter(r -> r.getStatus() == UserRegistration.RegistrationStatus.CONFIRMED)
            .count();
        int pendingRegistrations = (int) registrations.stream()
            .filter(r -> r.getStatus() == UserRegistration.RegistrationStatus.PENDING)
            .count();

        int availableSlots = plan.getAvailableSlots() != null ?
            Math.max(0, plan.getAvailableSlots() - confirmedRegistrations) : -1;

        double occupancyRate = plan.getAvailableSlots() != null ?
            (double) confirmedRegistrations / plan.getAvailableSlots() * 100 : 0.0;

        return new PlanStatistics(planId, plan.getName(), totalRegistrations,
                                confirmedRegistrations, pendingRegistrations,
                                availableSlots, occupancyRate);
    }

    @Override
    public int getRemainingSlots(Long planId) {
        logger.debug("Getting remaining slots for plan: {}", planId);

        TreasureHuntPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if (plan.getAvailableSlots() == null) {
            return -1; // Unlimited slots
        }

        int confirmedRegistrations = (int) registrationRepository.findByPlanIdWithTeamMembers(planId)
            .stream()
            .filter(r -> r.getStatus() == UserRegistration.RegistrationStatus.CONFIRMED)
            .count();

        return Math.max(0, plan.getAvailableSlots() - confirmedRegistrations);
    }

    @Override
    public boolean hasAvailableSlots(Long planId) {
        logger.debug("Checking if plan has available slots: {}", planId);

        TreasureHuntPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if (plan.getAvailableSlots() == null) {
            return true; // Unlimited slots
        }

        int confirmedRegistrations = (int) registrationRepository.findByPlanIdWithTeamMembers(planId)
            .stream()
            .filter(r -> r.getStatus() == UserRegistration.RegistrationStatus.CONFIRMED)
            .count();

        return plan.getAvailableSlots() > confirmedRegistrations;
    }

    @Override
    public Page<TreasureHuntPlan> getPlans(Pageable pageable) {
        logger.debug("Getting paginated plans: {}", pageable);
        return planRepository.findAll(pageable);
    }
}
