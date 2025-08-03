package com.treasurehunt.dto;

import jakarta.validation.constraints.*;

public class TeamMemberDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotNull(message = "Age is required")
    @Min(value = 16, message = "Minimum age is 16")
    @Max(value = 100, message = "Maximum age is 100")
    private Integer age;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{10,20}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    // Emergency contact fields - only required for individual registrations and team leaders
    // Note: Validation is conditional - only applied when fields are not empty
    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^$|^[+]?[0-9\\s\\-\\(\\)]{10,20}$", message = "Please provide a valid emergency contact phone")
    private String emergencyContactPhone;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    // Constructors
    public TeamMemberDTO() {}

    public TeamMemberDTO(String fullName, Integer age, String gender, String email,
                        String phoneNumber, String emergencyContactName, String emergencyContactPhone, String bio) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.bio = bio;
    }

    // Getters and setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Validate emergency contact fields based on member position
     * @param isTeamLeader true if this is the team leader (first member)
     * @param isIndividualRegistration true if this is an individual registration
     * @return true if validation passes
     */
    public boolean validateEmergencyContacts(boolean isTeamLeader, boolean isIndividualRegistration) {
        // Emergency contacts are required for individual registrations and team leaders
        if (isIndividualRegistration || isTeamLeader) {
            return emergencyContactName != null && !emergencyContactName.trim().isEmpty() &&
                   emergencyContactPhone != null && !emergencyContactPhone.trim().isEmpty() &&
                   emergencyContactName.trim().length() >= 2; // Minimum length check
        }
        // For team members (non-leaders), emergency contacts are optional
        // But if provided, they should be valid
        if (emergencyContactName != null && !emergencyContactName.trim().isEmpty()) {
            return emergencyContactName.trim().length() >= 2;
        }
        return true;
    }
}
