package com.treasurehunt.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Treasure Hunt Plan
 * Maps to treasure_hunt_plans table in PostgreSQL database
 */
@Entity
@Table(name = "treasure_hunt_plans")
public class TreasureHuntPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 24, message = "Duration must not exceed 24 hours")
    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    @NotNull(message = "Difficulty level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 50)
    private DifficultyLevel difficultyLevel;

    @NotNull(message = "Maximum participants is required")
    @Min(value = 1, message = "Must allow at least 1 participant")
    @Max(value = 100, message = "Maximum participants cannot exceed 100")
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @NotNull(message = "Team size is required")
    @Min(value = 1, message = "Team size must be at least 1")
    @Max(value = 10, message = "Team size cannot exceed 10")
    @Column(name = "team_size", nullable = false)
    private Integer teamSize = 1;

    @NotNull(message = "Team type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false, length = 20)
    private TeamType teamType = TeamType.INDIVIDUAL;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @DecimalMax(value = "9999.99", message = "Price must not exceed $9999.99 (₹829,999)")
    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanStatus status = PlanStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserRegistration> registrations = new ArrayList<>();

    // Enums
    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum PlanStatus {
        ACTIVE, INACTIVE
    }

    public enum TeamType {
        INDIVIDUAL, TEAM
    }

    // Constructors
    public TreasureHuntPlan() {}

    public TreasureHuntPlan(String name, String description, Integer durationHours, 
                           DifficultyLevel difficultyLevel, Integer maxParticipants, 
                           BigDecimal priceUsd) {
        this.name = name;
        this.description = description;
        this.durationHours = durationHours;
        this.difficultyLevel = difficultyLevel;
        this.maxParticipants = maxParticipants;
        this.priceUsd = priceUsd;
        this.status = PlanStatus.ACTIVE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }

    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public List<UserRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<UserRegistration> registrations) { this.registrations = registrations; }

    // Helper methods
    public String getTruncatedDescription(int maxLength) {
        if (description == null || description.length() <= maxLength) {
            return description;
        }
        return description.substring(0, maxLength) + "...";
    }

    public long getRegistrationCount() {
        return registrations.stream()
                .filter(reg -> reg.getStatus() == UserRegistration.RegistrationStatus.CONFIRMED)
                .count();
    }

    public boolean isAvailable() {
        return status == PlanStatus.ACTIVE && getRegistrationCount() < maxParticipants;
    }

    /**
     * Get price in Indian Rupees (converted from USD)
     * Using approximate conversion rate of 1 USD = 83 INR
     */
    public BigDecimal getPriceInr() {
        if (priceUsd == null) {
            return BigDecimal.ZERO;
        }
        return priceUsd.multiply(new BigDecimal("83")).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    // Getters and setters for new fields
    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public TeamType getTeamType() {
        return teamType;
    }

    public void setTeamType(TeamType teamType) {
        this.teamType = teamType;
    }

    public boolean isTeamBased() {
        return teamType == TeamType.TEAM && teamSize > 1;
    }

    public String getTeamDescription() {
        if (teamType == TeamType.INDIVIDUAL || teamSize == 1) {
            return "Individual players";
        } else {
            return "Teams of " + teamSize + " players";
        }
    }
}
