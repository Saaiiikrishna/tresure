package com.treasurehunt.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Min(value = 1, message = "Duration must be at least 1 second")
    @Column(name = "duration_hours", nullable = true)
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
    @DecimalMax(value = "999999.99", message = "Price must not exceed ₹999,999")
    @Column(name = "price_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceInr;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Size(max = 500, message = "Preview video URL must not exceed 500 characters")
    @Column(name = "preview_video_url", length = 500)
    private String previewVideoUrl;

    @Min(value = 0, message = "Batches completed must be non-negative")
    @Column(name = "batches_completed", nullable = false)
    private Integer batchesCompleted = 0;

    @DecimalMin(value = "0.0", message = "Rating must be between 0.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Rating must be between 0.0 and 5.0")
    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Prize money must be non-negative")
    @Column(name = "prize_money", nullable = false, precision = 10, scale = 2)
    private BigDecimal prizeMoney = BigDecimal.ZERO;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Min(value = 0, message = "Available slots must be non-negative")
    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots = 0;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Event scheduling fields
    @NotNull(message = "Start date is required")
    @Column(name = "event_date")
    private LocalDate eventDate;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time")
    private LocalTime startTime;

    @NotNull(message = "End date is required")
    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull(message = "End time is required")
    @Column(name = "end_time")
    private LocalTime endTime;

    @OneToMany(mappedBy = "plan", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserRegistration> registrations = new ArrayList<>();

    // Transient field for confirmed registrations count
    @Transient
    private Long confirmedRegistrationsCount = 0L;

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
                           BigDecimal priceInr) {
        this.name = name;
        this.description = description;
        this.durationHours = durationHours;
        this.difficultyLevel = difficultyLevel;
        this.maxParticipants = maxParticipants;
        this.availableSlots = maxParticipants; // Initialize available slots to max participants
        this.priceInr = priceInr;
        this.prizeMoney = BigDecimal.ZERO; // Initialize prize money
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

    public BigDecimal getPriceInr() { return priceInr; }
    public void setPriceInr(BigDecimal priceInr) { this.priceInr = priceInr; }

    // Legacy method for backward compatibility - deprecated
    @Deprecated
    public BigDecimal getPriceUsd() {
        if (priceInr == null) return BigDecimal.ZERO;
        return priceInr.divide(new BigDecimal("83"), 2, BigDecimal.ROUND_HALF_UP);
    }

    @Deprecated
    public void setPriceUsd(BigDecimal priceUsd) {
        if (priceUsd != null) {
            this.priceInr = priceUsd.multiply(new BigDecimal("83")).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

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
        // Use pre-loaded count to avoid lazy loading issues
        if (confirmedRegistrationsCount != null) {
            return confirmedRegistrationsCount;
        }

        // Fallback to collection count if available (when session is active)
        try {
            return registrations.stream()
                    .filter(reg -> reg.getStatus() == UserRegistration.RegistrationStatus.CONFIRMED)
                    .count();
        } catch (Exception e) {
            // If lazy loading fails, return 0 as fallback
            return 0;
        }
    }

    public boolean isAvailable() {
        return status == PlanStatus.ACTIVE && getRegistrationCount() < maxParticipants;
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

    public String getPreviewVideoUrl() {
        return previewVideoUrl;
    }

    public void setPreviewVideoUrl(String previewVideoUrl) {
        this.previewVideoUrl = previewVideoUrl;
    }

    public Integer getBatchesCompleted() {
        return batchesCompleted;
    }

    public void setBatchesCompleted(Integer batchesCompleted) {
        this.batchesCompleted = batchesCompleted;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public BigDecimal getPrizeMoney() {
        return prizeMoney;
    }

    public void setPrizeMoney(BigDecimal prizeMoney) {
        this.prizeMoney = prizeMoney;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Long getConfirmedRegistrationsCount() {
        return confirmedRegistrationsCount;
    }

    public void setConfirmedRegistrationsCount(Long confirmedRegistrationsCount) {
        this.confirmedRegistrationsCount = confirmedRegistrationsCount;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isTeamBased() {
        return teamType == TeamType.TEAM && teamSize > 1;
    }

    /**
     * Calculate duration in hours between start and end datetime
     * @return Duration in hours
     */
    public long calculateDurationHours() {
        if (eventDate == null || startTime == null || endDate == null || endTime == null) {
            return durationHours != null ? durationHours : 0;
        }

        LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

        return java.time.Duration.between(startDateTime, endDateTime).toHours();
    }

    /**
     * Format duration hours into readable text
     * @param hours Total hours
     * @return Formatted duration like "3 hours", "1 day 2 hours", "2 days 9 hours"
     */
    private String formatDurationText(long hours) {
        if (hours <= 24) {
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else {
            long days = hours / 24;
            long remainingHours = hours % 24;

            String result = days + " day" + (days == 1 ? "" : "s");
            if (remainingHours > 0) {
                result += " " + remainingHours + " hour" + (remainingHours == 1 ? "" : "s");
            }
            return result;
        }
    }

    /**
     * Get formatted duration display with date and time range
     * @return Formatted string like "23rd May, 09:00 AM - 06:00 PM (8 hours)"
     */
    public String getFormattedDuration() {
        if (eventDate == null || startTime == null) {
            return durationHours + " hours";
        }

        LocalTime actualEndTime = endTime != null ? endTime : startTime.plusHours(durationHours);
        LocalDate actualEndDate = endDate != null ? endDate : eventDate;

        // Calculate total hours
        long totalHours = calculateDurationHours();

        // Format date with ordinal suffix
        String dayWithSuffix = getDayWithOrdinalSuffix(eventDate.getDayOfMonth());
        String monthName = eventDate.getMonth().toString().toLowerCase();
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

        // Format times
        String startTimeFormatted = formatTime(startTime);
        String endTimeFormatted = formatTime(actualEndTime);

        // Format duration
        String durationText = formatDurationText(totalHours);

        return String.format("%s %s, %s - %s (%s)",
            dayWithSuffix, monthName, startTimeFormatted, endTimeFormatted, durationText);
    }

    private String getDayWithOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1: return day + "st";
            case 2: return day + "nd";
            case 3: return day + "rd";
            default: return day + "th";
        }
    }

    private String formatTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        String amPm = hour >= 12 ? "PM" : "AM";

        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour -= 12;
        }

        return String.format("%02d:%02d %s", hour, minute, amPm);
    }

    public String getTeamDescription() {
        if (teamType == TeamType.INDIVIDUAL || teamSize == 1) {
            return "Individual players";
        } else {
            return "Teams of " + teamSize + " players";
        }
    }

    /**
     * Get formatted date for display (e.g., "23rd May")
     * @return Formatted date string or "TBD" if not set
     */
    public String getFormattedDate() {
        if (eventDate == null) {
            return "TBD";
        }

        String dayWithSuffix = getDayWithOrdinalSuffix(eventDate.getDayOfMonth());
        String monthName = eventDate.getMonth().toString().toLowerCase();
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

        return String.format("%s %s", dayWithSuffix, monthName);
    }

    /**
     * Get formatted time range for display (e.g., "09:00 AM - 06:00 PM (3 hours)")
     * @return Formatted time range string with duration or duration in hours if times not set
     */
    public String getFormattedTimeRange() {
        if (eventDate == null || startTime == null) {
            return durationHours + " hours";
        }

        LocalTime actualEndTime = endTime != null ? endTime : startTime.plusHours(durationHours);
        String startTimeFormatted = formatTime(startTime);
        String endTimeFormatted = formatTime(actualEndTime);

        // Calculate total duration
        long totalHours = calculateDurationHours();
        String durationText = formatDurationText(totalHours);

        return String.format("%s - %s (%s)", startTimeFormatted, endTimeFormatted, durationText);
    }

    /**
     * Get formatted date range for display (e.g., "10th August" or "10th August - 12th August")
     * @return Formatted date range string
     */
    public String getFormattedDateRange() {
        if (eventDate == null) {
            return "TBD";
        }

        String startDateFormatted = getFormattedDate();

        // Check if it's a multi-day event
        if (endDate != null && !endDate.equals(eventDate)) {
            String endDayWithSuffix = getDayWithOrdinalSuffix(endDate.getDayOfMonth());
            String endMonthName = endDate.getMonth().toString().toLowerCase();
            endMonthName = endMonthName.substring(0, 1).toUpperCase() + endMonthName.substring(1);
            String endDateFormatted = String.format("%s %s", endDayWithSuffix, endMonthName);

            return String.format("%s - %s", startDateFormatted, endDateFormatted);
        }

        return startDateFormatted;
    }
}
