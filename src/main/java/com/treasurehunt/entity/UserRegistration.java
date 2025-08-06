package com.treasurehunt.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a User Registration for a Treasure Hunt Plan
 * Maps to user_registrations table in PostgreSQL database
 */
@Entity
@Table(name = "user_registrations",
       indexes = {
           @Index(name = "idx_registration_email", columnList = "email"),
           @Index(name = "idx_registration_status", columnList = "status"),
           @Index(name = "idx_registration_plan_id", columnList = "plan_id"),
           @Index(name = "idx_registration_date", columnList = "registration_date"),
           @Index(name = "idx_registration_application_id", columnList = "application_id", unique = true)
       })
public class UserRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TreasureHuntPlan plan;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Minimum age is 18")
    @Max(value = 65, message = "Maximum age is 65")
    @Column(name = "age", nullable = false)
    private Integer age;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Please provide a valid 10-digit Indian mobile number")
    @Size(max = 10, message = "Phone number must be exactly 10 digits")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 255, message = "Emergency contact name must not exceed 255 characters")
    @Column(name = "emergency_contact_name", nullable = false)
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Please provide a valid 10-digit Indian mobile number")
    @Size(max = 10, message = "Emergency contact phone must be exactly 10 digits")
    @Column(name = "emergency_contact_phone", nullable = false)
    private String emergencyContactPhone;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    @Column(name = "bio", length = 2000)
    private String bio;

    @NotNull(message = "Medical consent must be given")
    @Column(name = "medical_consent_given", nullable = false)
    private Boolean medicalConsentGiven = false;

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "application_id", unique = true, length = 50)
    private String applicationId;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UploadedDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "registration",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true) // Remove team members when registration is deleted
    @BatchSize(size = 10) // Optimize batch loading for team members
    @JsonIgnore
    private List<TeamMember> teamMembers = new ArrayList<>();

    @Size(max = 100, message = "Team name must not exceed 100 characters")
    @Column(name = "team_name", length = 100)
    private String teamName;

    // Enums
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum RegistrationStatus {
        PENDING, CONFIRMED, CANCELLED
    }

    // Constructors
    public UserRegistration() {}

    public UserRegistration(TreasureHuntPlan plan, String fullName, Integer age, Gender gender,
                           String email, String phoneNumber, String emergencyContactName,
                           String emergencyContactPhone, Boolean medicalConsentGiven) {
        this.plan = plan;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.medicalConsentGiven = medicalConsentGiven;
        this.status = RegistrationStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TreasureHuntPlan getPlan() { return plan; }
    public void setPlan(TreasureHuntPlan plan) { this.plan = plan; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public Boolean getMedicalConsentGiven() { return medicalConsentGiven; }
    public void setMedicalConsentGiven(Boolean medicalConsentGiven) { this.medicalConsentGiven = medicalConsentGiven; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }

    public List<UploadedDocument> getDocuments() { return documents; }
    public void setDocuments(List<UploadedDocument> documents) { this.documents = documents; }

    // Helper methods
    /**
     * Get the persisted application ID or a placeholder if not yet generated.
     */
    public String getRegistrationNumber() {
        return applicationId != null ? applicationId : "TH-PENDING";
    }

    public boolean hasRequiredDocuments() {
        boolean hasPhoto = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.PHOTO);
        boolean hasId = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.ID_DOCUMENT);
        boolean hasMedical = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.MEDICAL_CERTIFICATE);

        boolean medicalRequired = medicalConsentGiven == null || !medicalConsentGiven;

        return hasPhoto && hasId && (!medicalRequired || hasMedical);
    }

    // Getters and setters for new fields
    public List<TeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isTeamRegistration() {
        // Use team name as primary indicator to avoid lazy loading issues
        return teamName != null && !teamName.trim().isEmpty();
    }

    public int getTeamSize() {
        return teamMembers != null ? teamMembers.size() : 0;
    }

    public TeamMember getTeamLeader() {
        return teamMembers.stream()
                .filter(TeamMember::isTeamLeader)
                .findFirst()
                .orElse(null);
    }

    // ===== BIDIRECTIONAL RELATIONSHIP HELPER METHODS =====

    /**
     * Add a team member to this registration (bidirectional helper)
     * @param teamMember Team member to add
     */
    public void addTeamMember(TeamMember teamMember) {
        if (teamMember != null) {
            teamMembers.add(teamMember);
            teamMember.setRegistration(this);
        }
    }

    /**
     * Remove a team member from this registration (bidirectional helper)
     * @param teamMember Team member to remove
     */
    public void removeTeamMember(TeamMember teamMember) {
        if (teamMember != null) {
            teamMembers.remove(teamMember);
            teamMember.setRegistration(null);
        }
    }

    /**
     * Add a document to this registration (bidirectional helper)
     * @param document Document to add
     */
    public void addDocument(UploadedDocument document) {
        if (document != null) {
            documents.add(document);
            document.setRegistration(this);
        }
    }

    /**
     * Remove a document from this registration (bidirectional helper)
     * @param document Document to remove
     */
    public void removeDocument(UploadedDocument document) {
        if (document != null) {
            documents.remove(document);
            document.setRegistration(null);
        }
    }

    /**
     * Check if all required documents are uploaded
     * @return true if all required documents are present
     */
    public boolean hasAllRequiredDocuments() {
        return hasRequiredDocuments();
    }

    /**
     * Get team member by position
     * @param position Member position (1 = leader, 2+ = members)
     * @return Team member at position, or null if not found
     */
    public TeamMember getTeamMemberByPosition(int position) {
        return teamMembers.stream()
                .filter(member -> member.getMemberPosition() != null && member.getMemberPosition() == position)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get application ID (alias for applicationId field)
     * @return Application ID
     */
    public String getApplicationId() {
        return this.applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
}
