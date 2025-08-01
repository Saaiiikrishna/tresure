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

    @NotBlank(message = "Emergency contact name is required")
    @Size(min = 2, max = 100, message = "Emergency contact name must be between 2 and 100 characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{10,20}$", message = "Please provide a valid emergency contact phone")
    private String emergencyContactPhone;

    // Constructors
    public TeamMemberDTO() {}

    public TeamMemberDTO(String fullName, Integer age, String gender, String email, 
                        String phoneNumber, String emergencyContactName, String emergencyContactPhone) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
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
}
