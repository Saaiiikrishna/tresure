package com.treasurehunt.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Minimum age is 18")
    @Max(value = 65, message = "Maximum age is 65")
    @Column(name = "age", nullable = false)
    private Integer age;

    @NotBlank(message = "Gender is required")
    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Please provide a valid 10-digit Indian mobile number")
    @Column(name = "phone_number", nullable = false, length = 10)
    private String phoneNumber;

    // Emergency contact fields - only required for team leaders, optional for team members
    @Size(min = 2, max = 100, message = "Emergency contact name must be between 2 and 100 characters")
    @Column(name = "emergency_contact_name", nullable = true, length = 100)
    private String emergencyContactName;

    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Please provide a valid 10-digit Indian mobile number")
    @Column(name = "emergency_contact_phone", nullable = true, length = 10)
    private String emergencyContactPhone;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    @Column(name = "bio", length = 2000)
    private String bio;

    @Column(name = "member_position", nullable = false)
    private Integer memberPosition; // 1 for team leader, 2+ for other members

    @Column(name = "medical_consent_given", nullable = true)
    private Boolean medicalConsentGiven;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    @JsonIgnore
    private UserRegistration registration;

    // Constructors
    public TeamMember() {}

    public TeamMember(String fullName, Integer age, String gender, String email,
                     String phoneNumber, String emergencyContactName,
                     String emergencyContactPhone, String bio, Integer memberPosition) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.bio = bio;
        this.memberPosition = memberPosition;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getMemberPosition() {
        return memberPosition;
    }

    public void setMemberPosition(Integer memberPosition) {
        this.memberPosition = memberPosition;
    }

    public Boolean getMedicalConsentGiven() {
        return medicalConsentGiven;
    }

    public void setMedicalConsentGiven(Boolean medicalConsentGiven) {
        this.medicalConsentGiven = medicalConsentGiven;
    }

    public UserRegistration getRegistration() {
        return registration;
    }

    public void setRegistration(UserRegistration registration) {
        this.registration = registration;
    }

    public boolean isTeamLeader() {
        return memberPosition != null && memberPosition == 1;
    }
}
