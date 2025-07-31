package com.treasurehunt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a User Registration for a Treasure Hunt Plan
 * Maps to user_registrations table in PostgreSQL database
 */
@Entity
@Table(name = "user_registrations")
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
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 255, message = "Emergency contact name must not exceed 255 characters")
    @Column(name = "emergency_contact_name", nullable = false)
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid emergency contact phone")
    @Size(max = 20, message = "Emergency contact phone must not exceed 20 characters")
    @Column(name = "emergency_contact_phone", nullable = false)
    private String emergencyContactPhone;

    @NotNull(message = "Medical consent must be given")
    @Column(name = "medical_consent_given", nullable = false)
    private Boolean medicalConsentGiven = false;

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UploadedDocument> documents = new ArrayList<>();

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
    public String getRegistrationNumber() {
        return String.format("TH-%06d", id);
    }

    public boolean hasRequiredDocuments() {
        boolean hasPhoto = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.PHOTO);
        boolean hasId = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.ID_DOCUMENT);
        boolean hasMedical = documents.stream()
                .anyMatch(doc -> doc.getDocumentType() == UploadedDocument.DocumentType.MEDICAL_CERTIFICATE);
        
        return hasPhoto && hasId && hasMedical;
    }
}
