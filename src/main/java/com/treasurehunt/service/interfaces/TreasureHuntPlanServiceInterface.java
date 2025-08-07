package com.treasurehunt.service.interfaces;

import com.treasurehunt.entity.TreasureHuntPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for treasure hunt plan operations
 * Provides abstraction layer for business logic operations
 */
public interface TreasureHuntPlanServiceInterface {

    /**
     * Get all active treasure hunt plans
     * @return List of active plans
     */
    List<TreasureHuntPlan> getAllActivePlans();

    /**
     * Get treasure hunt plan by ID
     * @param id Plan ID
     * @return Optional plan
     */
    Optional<TreasureHuntPlan> getPlanById(Long id);

    /**
     * Get featured treasure hunt plan
     * @return Featured plan or null if none
     */
    TreasureHuntPlan getFeaturedPlan();

    /**
     * Get plans by difficulty level
     * @param difficulty Difficulty level
     * @return List of plans with specified difficulty
     */
    List<TreasureHuntPlan> getPlansByDifficulty(TreasureHuntPlan.DifficultyLevel difficulty);

    /**
     * Get paginated plans
     * @param pageable Pagination parameters
     * @return Page of plans
     */
    Page<TreasureHuntPlan> getPlans(Pageable pageable);

    /**
     * Create new treasure hunt plan
     * @param plan Plan to create
     * @return Created plan
     */
    TreasureHuntPlan createPlan(TreasureHuntPlan plan);

    /**
     * Update existing treasure hunt plan
     * @param id Plan ID
     * @param plan Updated plan data
     * @return Updated plan
     */
    TreasureHuntPlan updatePlan(Long id, TreasureHuntPlan plan);

    /**
     * Set plan as featured
     * @param planId Plan ID to set as featured
     * @return Updated plan
     */
    TreasureHuntPlan setFeaturedPlan(Long planId);

    /**
     * Delete treasure hunt plan
     * @param id Plan ID to delete
     */
    void deletePlan(Long id);

    /**
     * Check if plan has available slots
     * @param planId Plan ID
     * @return true if slots are available
     */
    boolean hasAvailableSlots(Long planId);

    /**
     * Get remaining slots for plan
     * @param planId Plan ID
     * @return Number of remaining slots
     */
    int getRemainingSlots(Long planId);

    /**
     * Get plan statistics
     * @param planId Plan ID
     * @return Plan statistics
     */
    PlanStatistics getPlanStatistics(Long planId);

    /**
     * Plan statistics data class
     */
    class PlanStatistics {
        private final Long planId;
        private final String planName;
        private final int totalRegistrations;
        private final int confirmedRegistrations;
        private final int pendingRegistrations;
        private final int availableSlots;
        private final double occupancyRate;

        public PlanStatistics(Long planId, String planName, int totalRegistrations, 
                            int confirmedRegistrations, int pendingRegistrations, 
                            int availableSlots, double occupancyRate) {
            this.planId = planId;
            this.planName = planName;
            this.totalRegistrations = totalRegistrations;
            this.confirmedRegistrations = confirmedRegistrations;
            this.pendingRegistrations = pendingRegistrations;
            this.availableSlots = availableSlots;
            this.occupancyRate = occupancyRate;
        }

        // Getters
        public Long getPlanId() { return planId; }
        public String getPlanName() { return planName; }
        public int getTotalRegistrations() { return totalRegistrations; }
        public int getConfirmedRegistrations() { return confirmedRegistrations; }
        public int getPendingRegistrations() { return pendingRegistrations; }
        public int getAvailableSlots() { return availableSlots; }
        public double getOccupancyRate() { return occupancyRate; }

        @Override
        public String toString() {
            return String.format("PlanStatistics{planId=%d, name='%s', total=%d, confirmed=%d, pending=%d, available=%d, occupancy=%.1f%%}",
                               planId, planName, totalRegistrations, confirmedRegistrations, pendingRegistrations, availableSlots, occupancyRate);
        }
    }
}
