package com.treasurehunt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class RegistrationRequestDTO {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String teamName;

    @NotNull(message = "Team size is required")
    @Min(value = 1, message = "Team size must be at least 1")
    @Max(value = 10, message = "Team size cannot exceed 10")
    private Integer teamSize;

    @NotNull(message = "Team based flag is required")
    private Boolean isTeamBased;

    @NotNull(message = "Medical consent is required")
    private Boolean medicalConsentGiven;

    @Valid
    @NotEmpty(message = "At least one team member is required")
    @Size(max = 10, message = "Maximum 10 team members allowed")
    private List<TeamMemberDTO> members;

    // Constructors
    public RegistrationRequestDTO() {}

    public RegistrationRequestDTO(Long planId, String teamName, Integer teamSize, 
                                 Boolean isTeamBased, Boolean medicalConsentGiven, 
                                 List<TeamMemberDTO> members) {
        this.planId = planId;
        this.teamName = teamName;
        this.teamSize = teamSize;
        this.isTeamBased = isTeamBased;
        this.medicalConsentGiven = medicalConsentGiven;
        this.members = members;
    }

    // Getters and setters
    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Boolean getIsTeamBased() {
        return isTeamBased;
    }

    public void setIsTeamBased(Boolean isTeamBased) {
        this.isTeamBased = isTeamBased;
    }

    public Boolean getMedicalConsentGiven() {
        return medicalConsentGiven;
    }

    public void setMedicalConsentGiven(Boolean medicalConsentGiven) {
        this.medicalConsentGiven = medicalConsentGiven;
    }

    public List<TeamMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMemberDTO> members) {
        this.members = members;
    }

    // Validation methods
    public boolean isValidTeamRegistration() {
        if (isTeamBased && (teamName == null || teamName.trim().isEmpty())) {
            return false;
        }
        return members != null && members.size() == teamSize;
    }

    public TeamMemberDTO getTeamLeader() {
        return members != null && !members.isEmpty() ? members.get(0) : null;
    }
}
