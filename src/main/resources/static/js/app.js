/**
 * Treasure Hunt Adventures - Frontend JavaScript
 * Handles registration form, file uploads, and UI interactions
 */

// Global variables
window.currentStep = 1;
window.selectedPlan = null;

let formListenersInitialized = false;
let progressListenersInitialized = false;

// Robust step navigation functions
window.showStepRobust = function(step) {
    // Hide all steps
    const allSteps = document.querySelectorAll('.form-step');
    allSteps.forEach(stepEl => {
        stepEl.classList.add('d-none');
        stepEl.classList.remove('fade-in');
    });

    // Show target step
    const targetStep = document.querySelector(`.form-step[data-step="${step}"]`);
    if (targetStep) {
        targetStep.classList.remove('d-none');
        targetStep.classList.add('fade-in');

        // Setup file uploads for step 2
        if (step === 2) {
            setTimeout(() => {
                setupFileUploads();
            }, 200);
        }
    } else {
        console.error('Target step element not found for step:', step);
    }

    // Update progress
    document.querySelectorAll('.progress-step').forEach(progressStep => {
        progressStep.classList.remove('active');
    });
    const activeProgress = document.querySelector(`.progress-step[data-step="${step}"]`);
    if (activeProgress) {
        activeProgress.classList.add('active');
    }

    window.currentStep = step;
};

window.nextStepRobust = function() {
    // Ensure currentStep is set
    if (typeof window.currentStep === 'undefined' || window.currentStep === null) {
        window.currentStep = 1;
    }

    if (window.currentStep === 1) {
        // Check if this is a team form or individual form
        const teamNameField = document.getElementById('teamName');
        const isTeamForm = teamNameField !== null;

        // Get all member forms
        const memberForms = document.querySelectorAll('.member-form');
        const totalMembers = memberForms.length;

        // Validate team name if it's a team form
        if (isTeamForm) {
            const teamName = teamNameField.value?.trim();
            if (!teamName || teamName.length < 3) {
                alert('Please enter a valid team name (minimum 3 characters).');
                teamNameField.focus();
                return;
            }
        }

        // Validate all member forms
        let allMembersValid = true;
        let invalidMemberNumber = 0;

        for (let i = 1; i <= totalMembers; i++) {
            const fullName = document.getElementById(`fullName_${i}`)?.value?.trim();
            const age = document.getElementById(`age_${i}`)?.value;
            const gender = document.getElementById(`gender_${i}`)?.value;
            const email = document.getElementById(`email_${i}`)?.value?.trim();
            const phoneNumber = document.getElementById(`phoneNumber_${i}`)?.value?.trim();

            // Emergency contact fields are only required for individual registrations or team leaders (member 1)
            const isIndividualRegistration = totalMembers === 1;
            const isTeamLeader = i === 1;
            const requiresEmergencyContact = isIndividualRegistration || isTeamLeader;

            const emergencyContactName = document.getElementById(`emergencyContactName_${i}`)?.value?.trim();
            const emergencyContactPhone = document.getElementById(`emergencyContactPhone_${i}`)?.value?.trim();

            console.log(`Validating member ${i}:`, {
                fullName: !!fullName,
                age: !!age,
                gender: !!gender,
                email: !!email,
                phoneNumber: !!phoneNumber,
                requiresEmergencyContact,
                emergencyContactName: !!emergencyContactName,
                emergencyContactPhone: !!emergencyContactPhone
            });

            // Check basic required fields for all members
            if (!fullName || !age || !gender || gender === '' || !email || !phoneNumber) {
                allMembersValid = false;
                invalidMemberNumber = i;
                console.log(`❌ Member ${i} missing basic required fields`);
                break;
            }

            // Check emergency contact fields only if required
            if (requiresEmergencyContact && (!emergencyContactName || !emergencyContactPhone)) {
                allMembersValid = false;
                invalidMemberNumber = i;
                console.log(`❌ Member ${i} missing required emergency contact fields`);
                break;
            }

            // Additional validation
            const ageNum = parseInt(age);
            if (isNaN(ageNum) || ageNum < 18 || ageNum > 65) {
                console.log(`❌ Member ${i} age validation failed: ${age}`);
                alert(`Member ${i}: Age must be between 18 and 65.`);
                document.getElementById(`age_${i}`)?.focus();
                return;
            }

            // Phone validation
            const phoneRegex = /^[6-9][0-9]{9}$/;
            if (!phoneRegex.test(phoneNumber)) {
                console.log(`❌ Member ${i} phone validation failed: ${phoneNumber}`);
                alert(`Member ${i}: Please enter a valid 10-digit Indian mobile number.`);
                document.getElementById(`phoneNumber_${i}`)?.focus();
                return;
            }

            // Emergency contact phone validation (only if required)
            if (requiresEmergencyContact && emergencyContactPhone && !phoneRegex.test(emergencyContactPhone)) {
                console.log(`❌ Member ${i} emergency phone validation failed: ${emergencyContactPhone}`);
                alert(`Member ${i}: Please enter a valid emergency contact number.`);
                document.getElementById(`emergencyContactPhone_${i}`)?.focus();
                return;
            }

            // Email validation
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                console.log(`❌ Member ${i} email validation failed: ${email}`);
                alert(`Member ${i}: Please enter a valid email address.`);
                document.getElementById(`email_${i}`)?.focus();
                return;
            }

            console.log(`✅ Member ${i} validation passed`)
        }

        if (!allMembersValid) {
            const memberType = totalMembers > 1 ? `Team Member ${invalidMemberNumber}` : 'Personal information';
            const errorMessage = `Please complete all required fields for ${memberType}.`;
            console.log('❌ Step 1 validation failed:', errorMessage);
            alert(errorMessage);

            // Focus on the first invalid field
            const firstInvalidField = document.getElementById(`fullName_${invalidMemberNumber}`) ||
                                    document.getElementById(`age_${invalidMemberNumber}`) ||
                                    document.getElementById(`gender_${invalidMemberNumber}`) ||
                                    document.getElementById(`email_${invalidMemberNumber}`) ||
                                    document.getElementById(`phoneNumber_${invalidMemberNumber}`) ||
                                    document.getElementById(`emergencyContactName_${invalidMemberNumber}`) ||
                                    document.getElementById(`emergencyContactPhone_${invalidMemberNumber}`);

            if (firstInvalidField) {
                firstInvalidField.focus();
            }
            return;
        }

        console.log('✅ Step 1 validation passed, moving to step 2');
        window.showStepRobust(2);
    }
};

// Production-ready file upload tracking
const uploadedFiles = new Map(); // Using Map for better performance and memory management

/**
 * Show error notification to user
 * @param {string} message Error message to display
 */
function showErrorNotification(message) {
    // Create or update error notification
    let errorDiv = document.getElementById('error-notification');
    if (!errorDiv) {
        errorDiv = document.createElement('div');
        errorDiv.id = 'error-notification';
        errorDiv.className = 'alert alert-danger alert-dismissible fade show position-fixed';
        errorDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 400px;';
        document.body.appendChild(errorDiv);
    }

    errorDiv.innerHTML = `
        <i class="fas fa-exclamation-triangle me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    // Auto-hide after 5 seconds
    setTimeout(() => {
        if (errorDiv && errorDiv.parentNode) {
            errorDiv.remove();
        }
    }, 5000);
}

// DOM Content Loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

/**
 * Initialize the application
 */
function initializeApp() {
    setupEventListeners();
    setupPlanFilters();
    setupSmoothScrolling();
    loadPlans();
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Registration buttons
    document.querySelectorAll('.register-btn').forEach(btn => {
        btn.addEventListener('click', handleRegistrationClick);
    });

    // Preview buttons
    document.querySelectorAll('.preview-btn').forEach(btn => {
        btn.addEventListener('click', handlePreviewClick);
    });

    // Plan filter buttons
    document.querySelectorAll('[data-filter]').forEach(btn => {
        btn.addEventListener('click', handleFilterClick);
    });

    // Modal events
    const registrationModal = document.getElementById('registrationModal');
    if (registrationModal) {
        registrationModal.addEventListener('hidden.bs.modal', resetRegistrationForm);
    }

    const videoModal = document.getElementById('videoModal');
    if (videoModal) {
        videoModal.addEventListener('hidden.bs.modal', stopVideo);
    }
}

/**
 * Setup plan filters
 */
function setupPlanFilters() {
    const filterButtons = document.querySelectorAll('[data-filter]');
    const planCards = document.querySelectorAll('.plan-card');

    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            const filter = this.dataset.filter;
            
            // Update active button
            filterButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // Filter plans
            planCards.forEach(card => {
                if (filter === 'all' || card.dataset.difficulty === filter) {
                    card.style.display = 'block';
                    card.classList.add('fade-in');
                } else {
                    card.style.display = 'none';
                }
            });
        });
    });
}

/**
 * Setup smooth scrolling for navigation links
 */
function setupSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

/**
 * Load plans from API with proper error handling
 */
async function loadPlans() {
    try {
        const response = await fetch('/api/plans');
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        const plans = await response.json();
        updatePlanCards(plans);
    } catch (error) {
        console.error('Error loading plans:', error);
        // Show user-friendly error message
        showErrorNotification('Unable to load adventure plans. Please refresh the page.');
    }
}

/**
 * Update plan cards with fresh data
 */
function updatePlanCards(plans) {
    const container = document.getElementById('plansContainer');
    if (!container) return;

    // Update existing cards or create new ones
    plans.forEach(plan => {
        const existingCard = document.querySelector(`[data-plan-id="${plan.id}"]`);
        if (existingCard) {
            updatePlanCard(existingCard, plan);
        }
    });
}

/**
 * Update individual plan card
 */
function updatePlanCard(cardElement, plan) {
    const availableSpots = plan.maxParticipants - plan.registrationCount;
    const spotsElement = cardElement.closest('.plan-card').querySelector('[data-spots]');
    if (spotsElement) {
        spotsElement.textContent = availableSpots;
    }

    const registerBtn = cardElement.closest('.plan-card').querySelector('.register-btn');
    if (registerBtn) {
        registerBtn.disabled = !plan.available;
        registerBtn.innerHTML = plan.available 
            ? '<i class="fas fa-user-plus me-2"></i>Register Now'
            : '<i class="fas fa-ban me-2"></i>Fully Booked';
    }
}

/**
 * Handle registration button click
 */
async function handleRegistrationClick(event) {
    const button = event.target.closest('.register-btn');
    const planId = button.dataset.planId;

    try {
        // Fetch complete plan data from API
        const response = await fetch(`/api/plans/${planId}`);
        if (response.ok) {
            window.selectedPlan = await response.json();
        } else {
            // Fallback to button data if API fails
            window.selectedPlan = {
                id: planId,
                name: button.dataset.planName,
                price: parseFloat(button.dataset.planPrice),
                teamType: button.dataset.teamType || 'INDIVIDUAL',
                teamSize: parseInt(button.dataset.teamSize) || 1
            };
        }

        loadRegistrationForm();
        showModal('registrationModal');
    } catch (error) {
        console.error('Error fetching plan data:', error);
        // Use fallback data
        window.selectedPlan = {
            id: planId,
            name: button.dataset.planName,
            price: parseFloat(button.dataset.planPrice),
            teamType: button.dataset.teamType || 'INDIVIDUAL',
            teamSize: parseInt(button.dataset.teamSize) || 1
        };

        // Initialize modal persistence for this plan
        if (window.modalPersistence) {
            window.modalPersistence.init(planId);
        }

        loadRegistrationForm();
        showModal('registrationModal');
    }
}

/**
 * Handle preview button click
 */
function handlePreviewClick(event) {
    const button = event.target.closest('.preview-btn');
    const planId = button.dataset.planId;

    console.log('Preview button clicked for plan ID:', planId);

    // Fetch plan details to get video URL
    fetch(`/api/plans/${planId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch plan details');
            }
            return response.json();
        })
        .then(plan => {
            let videoUrl = plan.previewVideoUrl;

            // If no preview video URL is set, use default
            if (!videoUrl || videoUrl.trim() === '') {
                videoUrl = 'https://www.youtube.com/embed/dQw4w9WgXcQ';
            }

            // Add autoplay parameter if not already present
            if (videoUrl.includes('youtube.com') && !videoUrl.includes('autoplay=')) {
                videoUrl += videoUrl.includes('?') ? '&autoplay=1' : '?autoplay=1';
            }

            console.log('Loading video URL:', videoUrl);
            document.getElementById('previewVideo').src = videoUrl;

            // Update modal title with plan name
            const modalTitle = document.querySelector('#videoModal .modal-title');
            if (modalTitle) {
                modalTitle.textContent = `${plan.name} - Preview`;
            }

            showModal('videoModal');
        })
        .catch(error => {
            console.error('Error fetching plan details:', error);

            // Fallback to default video
            const defaultVideoUrl = 'https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1';
            document.getElementById('previewVideo').src = defaultVideoUrl;

            showModal('videoModal');
        });
}

/**
 * Handle filter button click
 */
function handleFilterClick(event) {
    const button = event.target;
    const filter = button.dataset.filter;
    
    // Update active state
    document.querySelectorAll('[data-filter]').forEach(btn => {
        btn.classList.remove('active');
    });
    button.classList.add('active');

    // Filter plan cards
    filterPlans(filter);
}

/**
 * Filter plans by difficulty
 */
function filterPlans(filter) {
    const planCards = document.querySelectorAll('.plan-card');
    
    planCards.forEach(card => {
        const difficulty = card.dataset.difficulty;
        const shouldShow = filter === 'all' || difficulty === filter;
        
        if (shouldShow) {
            card.style.display = 'block';
            card.classList.add('fade-in');
        } else {
            card.style.display = 'none';
            card.classList.remove('fade-in');
        }
    });
}

/**
 * Update character count for bio field
 */
function updateCharacterCount(memberNumber) {
    const bioField = document.getElementById(`bio_${memberNumber}`);
    const charCountElement = document.getElementById(`charCount_${memberNumber}`);

    if (bioField && charCountElement) {
        const currentLength = bioField.value.length;
        charCountElement.textContent = currentLength;

        // Change color based on character count
        if (currentLength > 1800) {
            charCountElement.style.color = '#dc3545'; // Red when approaching limit
        } else if (currentLength > 1500) {
            charCountElement.style.color = '#fd7e14'; // Orange when getting close
        } else {
            charCountElement.style.color = '#6c757d'; // Default gray
        }
    }
}

/**
 * Load registration form
 */
async function loadRegistrationForm() {
    const container = document.getElementById('registrationFormContainer');
    if (!container) return;

    const planId = window.selectedPlan.id;
    if (!planId) {
        container.innerHTML = '<div class="alert alert-danger">Could not load registration form. Plan ID is missing.</div>';
        return;
    }

    try {
        const response = await fetch(`/register/form/${planId}`);
        if (!response.ok) {
            throw new Error(`Failed to load form: ${response.statusText}`);
        }
        const formHtml = await response.text();
        container.innerHTML = formHtml;

        // After injecting the HTML, setup all event listeners and initial states
        setupFormEventListeners();

        // Initialize form to show Step 1
        setTimeout(() => {
            window.currentStep = 1;
            window.showStepRobust(1);

            // Setup medical consent checkbox listener
            const medicalConsent = document.getElementById('medicalConsent');
            if (medicalConsent) {
                medicalConsent.addEventListener('change', function() {
                    toggleMedicalCertificateUpload(this.checked);
                    checkFormCompletion();
                });
                toggleMedicalCertificateUpload(medicalConsent.checked);
            }

            // Setup validation and progress tracking
            setupFormValidation();
            updateTeamProgress();
        }, 100);

    } catch (error) {
        console.error('Error loading registration form:', error);
        container.innerHTML = `<div class="alert alert-danger">Error loading registration form: ${error.message}</div>`;
    }
}

/**
 * Handle input changes for validation and progress tracking
 */
function handleInputChange(event) {
    const field = event.target;
    console.log(`🔄 Input changed: ${field.id || field.name} (${event.type})`);

    // Validate the field
    validateField(field);

    // Update progress
    setTimeout(() => {
        updateTeamProgress();
    }, 100);
}

/**
 * Setup form validation behavior
 */
function setupFormValidation() {
    const form = document.getElementById('registrationForm');
    if (!form) {
        console.log('Registration form not found, skipping validation setup');
        return;
    }

    // Get all form inputs
    const inputs = form.querySelectorAll('input, select');
    console.log(`Setting up validation for ${inputs.length} form inputs`);

    inputs.forEach((input) => {
        // Skip if already has event listeners attached
        if (input.dataset.validationSetup === 'true') {
            return;
        }

        // Remove any pre-existing validation classes
        input.classList.remove('is-invalid', 'is-valid');

        // Consolidated input event handler
        const handleInputEvent = function() {
            // Phone number formatting
            if (input.type === 'tel') {
                formatPhoneNumber(input);
            }

            // Age validation
            if (input.type === 'number' && input.name.includes('age')) {
                validateAgeField(input);
            }

            // Progress tracking
            updateTeamProgress();
        };

        // Consolidated blur event handler
        const handleBlurEvent = function() {
            validateField(input);
            updateTeamProgress();
        };

        // Consolidated change event handler
        const handleChangeEvent = function() {
            updateTeamProgress();
        };

        // Add event listeners
        input.addEventListener('input', handleInputEvent);
        input.addEventListener('blur', handleBlurEvent);

        if (input.tagName === 'SELECT') {
            input.addEventListener('change', handleChangeEvent);
        }

        // Mark as setup to prevent duplicate listeners
        input.dataset.validationSetup = 'true';
    });
}

/**
 * Validate individual field
 */
function validateField(field) {
    if (!field) return false;

    const value = (field.value || '').trim();
    let isValid = true;
    let errorMessage = '';

    console.log(`🔍 Validating field: ${field.id || field.name}, value: "${value}"`);

    // Check if field is required and empty
    if (field.required && !value) {
        isValid = false;
        errorMessage = 'This field is required.';
    }
    // Validate email
    else if (field.type === 'email' && value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid email address.';
        }
    }
    // Validate phone number
    else if (field.type === 'tel' && value) {
        const phoneRegex = /^[6-9][0-9]{9}$/;
        if (!phoneRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid 10-digit Indian mobile number.';
        }
    }
    // Validate age
    else if (field.type === 'number' && field.name && field.name.includes('age') && value) {
        const age = parseInt(value);
        if (isNaN(age)) {
            isValid = false;
            errorMessage = 'Please enter a valid age.';
        } else if (age < 18) {
            isValid = false;
            errorMessage = 'Minimum age requirement is 18 years.';
        } else if (age > 65) {
            isValid = false;
            errorMessage = 'Maximum age limit is 65 years.';
        }
    }
    // Validate select fields
    else if (field.tagName === 'SELECT' && field.required && !value) {
        isValid = false;
        errorMessage = 'Please select an option.';
    }

    // Apply validation styling
    if (isValid) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');

        // Remove any existing error message
        removeErrorMessage(field);
        console.log(`✅ Field ${field.id || field.name} is valid`);
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');

        // Show error message
        showErrorMessage(field, errorMessage);
        console.log(`❌ Field ${field.id || field.name} is invalid: ${errorMessage}`);
    }

    return isValid;
}

/**
 * Show error message for a field
 */
function showErrorMessage(field, message) {
    // Remove existing error message first
    removeErrorMessage(field);

    // Create new error message
    const feedback = document.createElement('div');
    feedback.className = 'invalid-feedback';
    feedback.style.display = 'block';
    feedback.textContent = message;

    // Insert after the field or input group
    const inputGroup = field.closest('.input-group');
    const container = field.closest('.col-md-6, .col-12') || field.parentNode;

    if (inputGroup) {
        // Insert after the input group
        inputGroup.insertAdjacentElement('afterend', feedback);
    } else {
        // Insert after the field
        field.insertAdjacentElement('afterend', feedback);
    }

    console.log(`📝 Error message shown for ${field.id || field.name}: ${message}`);
}

/**
 * Remove error message for a field
 */
function removeErrorMessage(field) {
    // Find and remove existing error messages
    const container = field.closest('.col-md-6, .col-12') || field.parentNode;
    const existingFeedback = container.querySelector('.invalid-feedback');

    if (existingFeedback) {
        existingFeedback.remove();
        console.log(`🗑️ Error message removed for ${field.id || field.name}`);
    }
}

/**
 * Format phone number input
 */
function formatPhoneNumber(field) {
    let value = field.value.replace(/\D/g, ''); // Remove non-digits

    // Limit to 10 digits
    if (value.length > 10) {
        value = value.substring(0, 10);
    }

    field.value = value;
}

/**
 * Validate age field - improved to show proper error messages instead of auto-correction
 */
function validateAgeField(field) {
    const value = (field.value || '').trim();
    if (value) {
        const age = parseInt(value);

        // Don't auto-correct - let the main validateField function handle validation
        // This allows users to see proper error messages for invalid ages
        if (!isNaN(age)) {
            // Just trigger the main validation which will show appropriate error messages
            validateField(field);
        }
    }
}

/**
 * Update team progress indicator (debounced for performance)
 */
const updateTeamProgress = debounce(function() {
    const progressBadge = document.getElementById('teamProgress');
    const isTeamForm = progressBadge !== null;

    // Get all member forms
    const memberForms = document.querySelectorAll('.member-form');
    if (memberForms.length === 0) return;

    // Ensure event listeners are attached (do this once)
    if (!window.progressListenersAttached) {
        console.log('🔧 Attaching progress event listeners...');
        attachProgressEventListeners();
        window.progressListenersAttached = true;
    }

    let completedMembers = 0;
    const totalMembers = memberForms.length;

    // Check each member form for completion
    memberForms.forEach((memberForm, index) => {
        const memberNumber = index + 1;

        // Get all input fields within this member form that have the 'required' attribute
        const requiredFields = memberForm.querySelectorAll('input[required], select[required]');

        // Check if all required fields are filled (dynamic validation)
        let memberComplete = true;
        let filledFields = 0;
        const totalRequiredFields = requiredFields.length;

        requiredFields.forEach(field => {
            const value = (field.value || '').trim();
            if (value && value !== '') {
                filledFields++;

                // Basic validation for specific field types
                if (field.type === 'number' && field.name.includes('age')) {
                    const age = parseInt(value);
                    if (isNaN(age) || age < 18 || age > 65) {
                        memberComplete = false;
                    }
                } else if (field.type === 'tel' || field.name.includes('phoneNumber') || field.name.includes('emergencyContactPhone')) {
                    // More lenient phone validation - just check if it's 10 digits
                    if (value.length !== 10 || !/^\d+$/.test(value)) {
                        memberComplete = false;
                    }
                } else if (field.type === 'email' || field.name.includes('email')) {
                    // Basic email validation
                    if (!value.includes('@') || !value.includes('.')) {
                        memberComplete = false;
                    }
                }
            } else {
                memberComplete = false;
            }
        });

        console.log(`Member ${memberNumber}: ${filledFields}/${totalRequiredFields} required fields filled, complete: ${memberComplete}`);

        // Member is complete if all required fields are filled and valid
        if (memberComplete && filledFields === totalRequiredFields) {
            completedMembers++;

            // Add visual indicator to completed member form
            memberForm.classList.add('member-completed');
            memberForm.style.borderColor = '#28a745';
            memberForm.style.backgroundColor = '#f8fff9';

            // Add checkmark to member title
            const memberTitle = memberForm.querySelector('h6');
            if (memberTitle && !memberTitle.querySelector('.completion-check')) {
                const checkmark = document.createElement('span');
                checkmark.className = 'completion-check text-success ms-2';
                checkmark.innerHTML = '<i class="fas fa-check-circle"></i>';
                memberTitle.appendChild(checkmark);
            }
        } else {
            // Remove completion styling if member is no longer complete
            memberForm.classList.remove('member-completed');
            memberForm.style.borderColor = '';
            memberForm.style.backgroundColor = '#f8f9fa';

            // Remove checkmark
            const checkmark = memberForm.querySelector('.completion-check');
            if (checkmark) {
                checkmark.remove();
            }
        }
    });

    // Update progress badge (only for team forms)
    if (isTeamForm) {
        progressBadge.textContent = `${completedMembers} / ${totalMembers} completed`;

        // Update badge color based on progress
        progressBadge.className = 'badge bg-secondary';
        if (completedMembers === totalMembers) {
            progressBadge.className = 'badge bg-success';
        } else if (completedMembers > 0) {
            progressBadge.className = 'badge bg-warning text-dark';
        }
    }

    // Update team name field if it exists
    const teamNameField = document.getElementById('teamName');
    let teamNameComplete = false;
    if (teamNameField) {
        const teamNameValue = (teamNameField.value || '').trim();
        teamNameComplete = teamNameValue.length >= 3; // Minimum 3 characters for team name

        if (teamNameComplete) {
            teamNameField.classList.add('is-valid');
            teamNameField.classList.remove('is-invalid');
        }
    }

    // Enable/disable next step button based on overall completion
    const nextStepBtn = document.getElementById('nextStepBtn');
    if (nextStepBtn) {
        const allComplete = (completedMembers === totalMembers) && (teamNameComplete || totalMembers === 1);

        if (allComplete) {
            nextStepBtn.disabled = false;
            nextStepBtn.classList.remove('btn-secondary');
            nextStepBtn.classList.add('btn-primary');
            nextStepBtn.innerHTML = 'Next Step <i class="fas fa-arrow-right ms-2"></i>';
        } else {
            nextStepBtn.disabled = true;
            nextStepBtn.classList.remove('btn-primary');
            nextStepBtn.classList.add('btn-secondary');
            if (isTeamForm) {
                nextStepBtn.innerHTML = `Complete all team members to continue (${completedMembers}/${totalMembers})`;
            } else {
                nextStepBtn.innerHTML = 'Complete form to continue';
            }
        }
    }

    console.log(`Team progress: ${completedMembers}/${totalMembers} members completed`);
}, 300); // Debounce with 300ms delay

/**
 * Attach event listeners for progress tracking
 */
function attachProgressEventListeners() {
    if (progressListenersInitialized) return;
    progressListenersInitialized = true;

    const form = document.getElementById('registrationForm');
    if (!form) {
        console.log('❌ Registration form not found for progress listeners');
        return;
    }

    const inputs = form.querySelectorAll('input, select');
    console.log(`📝 Attaching progress listeners to ${inputs.length} form inputs`);

    inputs.forEach((input, index) => {
        input.addEventListener('input', handleProgressUpdate);
        input.addEventListener('change', handleProgressUpdate);
        input.addEventListener('blur', handleProgressUpdate);
        console.log(`✅ Listeners attached to ${input.id || input.name || 'unnamed field'}`);
    });
}

/**
 * Handle progress update events
 */
function handleProgressUpdate(event) {
    console.log(`🔄 Progress update triggered by ${event.target.id || event.target.name} (${event.type})`);
    setTimeout(() => updateTeamProgress(), 100); // Small delay to ensure value is updated
}

/**
 * Setup form event listeners
 */
function setupFormEventListeners() {
    if (formListenersInitialized) return;
    formListenersInitialized = true;

    const form = document.getElementById('registrationForm');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);

        // Add validation on form submission
        form.addEventListener('submit', function(event) {
            const inputs = form.querySelectorAll('input[required], select[required]');
            let hasErrors = false;

            inputs.forEach(input => {
                if (!validateField(input)) {
                    hasErrors = true;
                }
            });

            if (hasErrors) {
                event.preventDefault();
                event.stopPropagation();
            }
        });
    }

    // Robust event listener setup with multiple approaches
    console.log('Setting up robust event listeners...');

    // Method 1: Event delegation on document
    document.addEventListener('click', function(e) {
        console.log('Document click detected on:', e.target.id, e.target.className);

        if (e.target && (e.target.id === 'nextStepBtn' || e.target.closest('#nextStepBtn'))) {
            console.log('Next step button clicked via delegation');
            e.preventDefault();
            e.stopPropagation();
            window.nextStepRobust();
            return false;
        }

        if (e.target && (e.target.id === 'prevStepBtn' || e.target.closest('#prevStepBtn'))) {
            console.log('Previous step button clicked via delegation');
            e.preventDefault();
            e.stopPropagation();
            previousStep();
            return false;
        }

        // Submit button handling is now done through form submission event
        // No need for direct click handling here
    });

    // Method 2: Direct event listeners with proper cleanup
    function setupDirectListeners() {
        const nextBtn = document.getElementById('nextStepBtn');
        if (nextBtn && !nextBtn.dataset.listenerAttached) {
            console.log('Found nextStepBtn, adding direct listener');
            nextBtn.onclick = function(e) {
                console.log('Next step button clicked via direct listener');
                e.preventDefault();
                e.stopPropagation();
                window.nextStepRobust();
                return false;
            };
            nextBtn.dataset.listenerAttached = 'true';
        }

        const prevBtn = document.getElementById('prevStepBtn');
        if (prevBtn && !prevBtn.dataset.listenerAttached) {
            console.log('Found prevStepBtn, adding direct listener');
            prevBtn.onclick = function(e) {
                console.log('Previous step button clicked via direct listener');
                e.preventDefault();
                e.stopPropagation();
                previousStep();
                return false;
            };
            prevBtn.dataset.listenerAttached = 'true';
        }
    }

    // Setup direct listeners with limited polling to avoid duplication
    setupDirectListeners();
    setTimeout(setupDirectListeners, 500);

    // File upload areas - setup after form is loaded
    setupFileUploads();

    // Form validation
    setupFormValidation();
}

/**
 * Simple file upload handler - called directly from HTML onchange
 */
function handleFileUpload(input, fileType, maxSizeMB) {
    console.log(`📁 File upload triggered for ${fileType}`);

    const file = input.files[0];
    if (!file) {
        console.log('No file selected');
        return;
    }

    console.log(`📄 File selected: ${file.name}, Size: ${file.size} bytes, Type: ${file.type}`);

    // Validate file size
    const maxSize = maxSizeMB * 1024 * 1024; // Convert MB to bytes
    if (file.size > maxSize) {
        alert(`File size exceeds the maximum allowed size of ${maxSizeMB}MB.`);
        input.value = '';
        return;
    }

    // Update UI to show file is selected
    const fileInfoElement = document.getElementById(`${fileType}FileInfo`);
    if (fileInfoElement) {
        fileInfoElement.innerHTML = `
            <div class="text-success">
                <i class="fas fa-check-circle me-2"></i>
                <small>${file.name} (${formatFileSize(file.size)})</small>
            </div>
        `;
        fileInfoElement.classList.remove('d-none');
    }

    // Update the upload area styling
    const uploadArea = input.parentElement.querySelector('.file-upload-area');
    if (uploadArea) {
        uploadArea.style.borderColor = '#28a745';
        uploadArea.style.backgroundColor = '#f8fff9';
    }

    // Store file reference for form validation
    uploadedFiles[fileType] = file;

    console.log(`✅ File uploaded successfully for ${fileType}`);

    // Check if form is complete
    checkFormCompletion();
}

/**
 * Setup file uploads - simplified version
 */
function setupFileUploads() {
    console.log('📁 Setting up file uploads...');

    // Add change listeners to all file inputs
    const fileInputs = ['photoFile', 'idFile', 'medicalFile'];
    fileInputs.forEach(inputId => {
        const input = document.getElementById(inputId);
        if (input) {
            console.log(`✅ File input found: ${inputId}`);
        } else {
            console.log(`❌ File input not found: ${inputId}`);
        }
    });
}

/**
 * Setup form validation
 */
// Duplicate setupFormValidation() function removed - using the comprehensive version at line 802



/**
 * Toggle medical certificate upload field based on medical consent
 * @param {boolean} consentGiven - Whether medical consent is given
 */
function toggleMedicalCertificateUpload(consentGiven) {
    console.log('🏥 Toggling medical certificate upload, consent given:', consentGiven);

    const container = document.getElementById('medicalCertificateContainer');
    const label = document.getElementById('medicalCertificateLabel');
    const requiredText = document.getElementById('medicalCertificateRequired');
    const uploadArea = document.getElementById('medicalFileUploadArea');
    const uploadText = document.getElementById('medicalFileUploadText');
    const helperText = document.getElementById('medicalCertificateHelperText');
    const fileInput = document.getElementById('medicalFile');

    if (!container) {
        console.log('Medical certificate container not found');
        return;
    }

    if (consentGiven) {
        // Consent given - make upload optional
        container.style.opacity = '0.6';
        uploadArea.style.cursor = 'not-allowed';
        uploadArea.onclick = null;

        if (requiredText) {
            requiredText.innerHTML = '(Optional - PDF only, max 5MB)';
            requiredText.className = 'text-muted';
        }

        if (uploadText) {
            uploadText.textContent = 'Medical certificate not required (consent given)';
        }

        if (helperText) {
            helperText.innerHTML = '<i class="fas fa-check-circle text-success me-1"></i>Medical consent given - certificate upload is optional.';
            helperText.className = 'form-text mt-2 text-success';
        }

        if (fileInput) {
            fileInput.required = false;
            fileInput.disabled = true;
        }

        console.log('✅ Medical certificate upload disabled (consent given)');
    } else {
        // Consent not given - make upload required
        container.style.opacity = '1';
        uploadArea.style.cursor = 'pointer';
        uploadArea.onclick = function() { document.getElementById('medicalFile').click(); };

        if (requiredText) {
            requiredText.innerHTML = '(Required - PDF only, max 5MB)';
            requiredText.className = 'text-danger';
        }

        if (uploadText) {
            uploadText.textContent = 'Click to upload or drag & drop';
        }

        if (helperText) {
            helperText.innerHTML = '<i class="fas fa-exclamation-triangle text-warning me-1"></i>Medical certificate is required unless medical consent is given above.';
            helperText.className = 'form-text mt-2 text-warning';
        }

        if (fileInput) {
            fileInput.required = true;
            fileInput.disabled = false;
        }

        console.log('✅ Medical certificate upload enabled (consent not given)');
    }
}

/**
 * Check if form is complete - simplified and reliable
 */
function checkFormCompletion() {
    console.log('🔍 Checking form completion...');

    // Check if we're on step 2
    const step2 = document.querySelector('[data-step="2"]');
    if (!step2 || step2.classList.contains('d-none')) {
        console.log('⏭️ Step 2 not visible, skipping form completion check');
        return;
    }

    // Find submit button
    const submitBtn = document.getElementById('submitBtn');
    if (!submitBtn) {
        console.error('❌ Submit button not found!');
        return;
    }

    // Check medical consent
    const medicalConsentElement = document.getElementById('medicalConsent');
    const medicalConsent = medicalConsentElement ? medicalConsentElement.checked : false;
    console.log(`📋 Medical consent: ${medicalConsent}`);

    // Check all file uploads using the file input elements directly
    const photoInput = document.getElementById('photoFile');
    const idInput = document.getElementById('idFile');
    const medicalInput = document.getElementById('medicalFile');

    const photoUploaded = photoInput && photoInput.files && photoInput.files.length > 0;
    const idUploaded = idInput && idInput.files && idInput.files.length > 0;
    const medicalUploaded = medicalInput && medicalInput.files && medicalInput.files.length > 0;

    console.log(`📁 Files uploaded:`, {
        photo: photoUploaded,
        id: idUploaded,
        medical: medicalUploaded
    });

    // Medical certificate requirement depends on consent
    const medicalRequirementMet = medicalConsent || medicalUploaded;

    // All requirements must be met
    const allDocumentsUploaded = photoUploaded && idUploaded && medicalRequirementMet;
    const formComplete = medicalConsent && allDocumentsUploaded;

    // Enable/disable submit button
    submitBtn.disabled = !formComplete;

    console.log(`✅ Form completion result:`, {
        medicalConsent,
        allDocumentsUploaded,
        formComplete,
        submitButtonEnabled: !submitBtn.disabled
    });

    // Update submit button text to show status
    if (formComplete) {
        // Keep the original structure with spinner
        submitBtn.innerHTML = `
            <span class="spinner-border spinner-border-sm d-none me-2"></span>
            Submit Registration
        `;
        submitBtn.classList.remove('btn-secondary');
        submitBtn.classList.add('btn-success');
    } else {
        submitBtn.innerHTML = `
            <span class="spinner-border spinner-border-sm d-none me-2"></span>
            Complete All Requirements
        `;
        submitBtn.classList.remove('btn-success');
        submitBtn.classList.add('btn-secondary');
    }
}

/**
 * Navigate to next step
 */
// Duplicate nextStep() function removed - using window.nextStepRobust() instead

/**
 * Navigate to previous step
 */
function previousStep() {
    if (window.currentStep === 2) {
        window.showStepRobust(1);
    }
}

/**
 * Show specific step
 */
// Duplicate showStep() function removed - using window.showStepRobust() instead

/**
 * Update progress indicators
 */
function updateProgress(step) {
    document.querySelectorAll('.progress-step').forEach((stepEl, index) => {
        const stepNumber = index + 1;
        
        if (stepNumber < step) {
            stepEl.classList.add('completed');
            stepEl.classList.remove('active');
        } else if (stepNumber === step) {
            stepEl.classList.add('active');
            stepEl.classList.remove('completed');
        } else {
            stepEl.classList.remove('active', 'completed');
        }
    });
}

/**
 * Validate step 1
 */
function validateStep1() {
    const form = document.getElementById('registrationForm');
    const step1Fields = form.querySelectorAll('[data-step="1"] input[required], [data-step="1"] select[required]');
    
    let isValid = true;
    step1Fields.forEach(field => {
        if (!field.checkValidity()) {
            field.classList.add('is-invalid');
            isValid = false;
        } else {
            field.classList.remove('is-invalid');
            field.classList.add('is-valid');
        }
    });

    if (!isValid) {
        showAlert('Please fill in all required fields correctly.', 'danger');
    }

    return isValid;
}

/**
 * Handle form submission
 */
async function handleFormSubmit(event) {
    event.preventDefault();

    const submitBtn = document.getElementById('submitBtn');
    const spinner = submitBtn.querySelector('.spinner-border');

    // Show loading state
    submitBtn.disabled = true;
    spinner.classList.remove('d-none');

    try {
        console.log('🚀 Starting form submission...');

        // Determine if this is a team or individual registration
        const teamNameField = document.getElementById('teamName');
        const isTeamRegistration = teamNameField !== null;
        const memberForms = document.querySelectorAll('.member-form');
        const memberCount = memberForms.length;

        console.log(`Registration type: ${isTeamRegistration ? 'Team' : 'Individual'}, Members: ${memberCount}`);

        let response;

        if (isTeamRegistration) {
            // Handle team registration
            response = await submitTeamRegistration(event.target);
        } else {
            // Handle individual registration
            response = await submitIndividualRegistration(event.target);
        }

        const result = await response.json();

        console.log('📋 Server response:', result);

        if (response.ok && result.success) {
            // Clear saved form data on successful submission
            if (window.modalPersistence) {
                window.modalPersistence.markAsSubmitted();
            }

            showEnhancedSuccessModal(result, isTeamRegistration);
            hideModal('registrationModal');
        } else {
            const errorMessage = result.message || result.error || 'Registration failed. Please try again.';
            showAlert(errorMessage, 'danger');
            console.error('Registration failed:', result);
        }

    } catch (error) {
        console.error('Registration error:', error);
        showAlert('An error occurred. Please try again.', 'danger');
    } finally {
        // Hide loading state
        submitBtn.disabled = false;
        spinner.classList.add('d-none');
    }
}

/**
 * Submit team registration
 */
async function submitTeamRegistration(form) {
    console.log('📝 Preparing team registration data...');

    // Get plan ID from selectedPlan
    const planId = window.selectedPlan ? window.selectedPlan.id : null;
    if (!planId) {
        throw new Error('Plan ID not found');
    }

    // Get team name
    const teamName = document.getElementById('teamName')?.value?.trim();

    // Get medical consent
    const medicalConsent = document.getElementById('medicalConsent')?.checked || false;

    // Get all member forms
    const memberForms = document.querySelectorAll('.member-form');
    const members = [];

    // Extract member data
    for (let i = 1; i <= memberForms.length; i++) {
        const isTeamLeader = i === 1;
        const emergencyContactNameEl = document.getElementById(`emergencyContactName_${i}`);
        const emergencyContactPhoneEl = document.getElementById(`emergencyContactPhone_${i}`);

        const member = {
            fullName: document.getElementById(`fullName_${i}`)?.value?.trim(),
            age: parseInt(document.getElementById(`age_${i}`)?.value),
            gender: document.getElementById(`gender_${i}`)?.value,
            email: document.getElementById(`email_${i}`)?.value?.trim(),
            phoneNumber: document.getElementById(`phoneNumber_${i}`)?.value?.trim(),
            bio: document.getElementById(`bio_${i}`)?.value?.trim() || '',
            // Only include emergency contacts for team leader or if fields exist (individual registration)
            emergencyContactName: emergencyContactNameEl ? emergencyContactNameEl.value?.trim() : '',
            emergencyContactPhone: emergencyContactPhoneEl ? emergencyContactPhoneEl.value?.trim() : ''
        };

        members.push(member);
        console.log(`Member ${i}:`, member);
    }

    // Prepare team registration data
    const registrationData = {
        planId: planId,
        teamName: teamName,
        teamSize: members.length,
        isTeamBased: true,
        medicalConsentGiven: medicalConsent,
        members: members
    };

    console.log('📤 Sending team registration data:', registrationData);

    // Create FormData for multipart request (includes files)
    const formData = new FormData();

    // Add JSON data as form fields
    formData.append('planId', registrationData.planId);
    formData.append('teamName', registrationData.teamName);
    formData.append('teamSize', registrationData.teamSize);
    formData.append('isTeamBased', registrationData.isTeamBased);
    formData.append('medicalConsentGiven', registrationData.medicalConsentGiven);

    // Add members data
    registrationData.members.forEach((member, index) => {
        formData.append(`members[${index}].fullName`, member.fullName);
        formData.append(`members[${index}].age`, member.age);
        formData.append(`members[${index}].gender`, member.gender);
        formData.append(`members[${index}].email`, member.email);
        formData.append(`members[${index}].phoneNumber`, member.phoneNumber);
        formData.append(`members[${index}].emergencyContactName`, member.emergencyContactName || '');
        formData.append(`members[${index}].emergencyContactPhone`, member.emergencyContactPhone || '');
        formData.append(`members[${index}].bio`, member.bio || '');
    });

    // Add file uploads
    const photoFile = document.getElementById('photoFile')?.files[0];
    const idFile = document.getElementById('idFile')?.files[0];
    const medicalFile = document.getElementById('medicalFile')?.files[0];

    if (photoFile) formData.append('photoFile', photoFile);
    if (idFile) formData.append('idFile', idFile);
    if (medicalFile) formData.append('medicalFile', medicalFile);

    return fetch('/api/register/team', {
        method: 'POST',
        body: formData
    });
}

/**
 * Submit individual registration
 */
async function submitIndividualRegistration(form) {
    console.log('📝 Preparing individual registration data...');

    // Get plan ID from selectedPlan
    const planId = window.selectedPlan ? window.selectedPlan.id : null;
    if (!planId) {
        throw new Error('Plan ID not found');
    }

    // Get individual member data (member 1)
    const formData = new FormData();
    formData.append('planId', planId);
    formData.append('fullName', document.getElementById('fullName_1')?.value?.trim());
    formData.append('age', document.getElementById('age_1')?.value);
    formData.append('gender', document.getElementById('gender_1')?.value);
    formData.append('email', document.getElementById('email_1')?.value?.trim());
    formData.append('phoneNumber', document.getElementById('phoneNumber_1')?.value?.trim());
    formData.append('bio', document.getElementById('bio_1')?.value?.trim() || '');
    formData.append('emergencyContactName', document.getElementById('emergencyContactName_1')?.value?.trim());
    formData.append('emergencyContactPhone', document.getElementById('emergencyContactPhone_1')?.value?.trim());
    formData.append('medicalConsentGiven', document.getElementById('medicalConsent')?.checked || false);

    // Add file uploads
    const photoFile = document.getElementById('photoFile')?.files[0];
    const idFile = document.getElementById('idFile')?.files[0];
    const medicalFile = document.getElementById('medicalFile')?.files[0];

    if (photoFile) formData.append('photoFile', photoFile);
    if (idFile) formData.append('idFile', idFile);
    if (medicalFile) formData.append('medicalFile', medicalFile);

    console.log('📤 Sending individual registration data');

    return fetch('/api/register', {
        method: 'POST',
        body: formData
    });
}

/**
 * Show enhanced success modal with registration details
 */
function showEnhancedSuccessModal(result, isTeamRegistration) {
    console.log('📋 Showing enhanced success modal:', result);

    // Create enhanced success modal
    const modalHtml = `
        <div class="modal fade" id="successModal" tabindex="-1" aria-labelledby="successModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header bg-success text-white">
                        <h5 class="modal-title" id="successModalLabel">
                            <i class="fas fa-check-circle me-2"></i>
                            Registration Successful!
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="text-center mb-4">
                            <div class="success-icon mb-3">
                                <i class="fas fa-trophy text-warning" style="font-size: 3rem;"></i>
                            </div>
                            <h4 class="text-success">Welcome to the Adventure!</h4>
                            <p class="lead">${result.message || 'Your registration has been confirmed.'}</p>
                        </div>

                        <div class="registration-details">
                            <div class="card border-success">
                                <div class="card-header bg-light">
                                    <h6 class="mb-0"><i class="fas fa-info-circle me-2"></i>Registration Details</h6>
                                </div>
                                <div class="card-body">
                                    <div class="row">
                                        <div class="col-md-6">
                                            <p><strong>Registration Number:</strong><br>
                                            <span class="badge bg-primary fs-6">${result.registrationNumber || 'N/A'}</span></p>
                                        </div>
                                        <div class="col-md-6">
                                            <p><strong>Registration Date:</strong><br>
                                            ${new Date().toLocaleDateString('en-IN', {
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric',
                                                hour: '2-digit',
                                                minute: '2-digit'
                                            })}</p>
                                        </div>
                                    </div>

                                    ${isTeamRegistration ? `
                                        <div class="row mt-3">
                                            <div class="col-md-6">
                                                <p><strong>Team Name:</strong><br>${result.teamName || 'N/A'}</p>
                                            </div>
                                            <div class="col-md-6">
                                                <p><strong>Team Size:</strong><br>${result.teamSize || 'N/A'} members</p>
                                            </div>
                                        </div>

                                        <div class="mt-3">
                                            <h6><i class="fas fa-users me-2"></i>Team Members:</h6>
                                            <div id="teamMembersList" class="mt-2">
                                                <!-- Team members will be populated here -->
                                            </div>
                                        </div>
                                    ` : `
                                        <div class="mt-3">
                                            <h6><i class="fas fa-user me-2"></i>Participant Details:</h6>
                                            <div id="participantDetails" class="mt-2">
                                                <!-- Participant details will be populated here -->
                                            </div>
                                        </div>
                                    `}
                                </div>
                            </div>
                        </div>

                        <div class="alert alert-info mt-4">
                            <h6><i class="fas fa-envelope me-2"></i>What's Next?</h6>
                            <ul class="mb-0">
                                <li>You will receive a confirmation email shortly with all the details</li>
                                <li>Please check your spam folder if you don't see the email within 10 minutes</li>
                                <li>Save your registration number for future reference</li>
                                <li>We'll send you event details and instructions closer to the date</li>
                            </ul>
                        </div>

                        <div class="alert alert-warning">
                            <h6><i class="fas fa-exclamation-triangle me-2"></i>Important Notes:</h6>
                            <ul class="mb-0">
                                <li>Please arrive 15 minutes before the scheduled start time</li>
                                <li>Bring a valid ID and comfortable walking shoes</li>
                                <li>Mobile phones will be required for the treasure hunt</li>
                                ${isTeamRegistration ? '<li>All team members must be present at the start</li>' : ''}
                            </ul>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            <i class="fas fa-times me-2"></i>Close
                        </button>
                        <button type="button" class="btn btn-primary" onclick="printRegistration()">
                            <i class="fas fa-print me-2"></i>Print Details
                        </button>
                        <button type="button" class="btn btn-success" onclick="shareRegistration()">
                            <i class="fas fa-share me-2"></i>Share
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Remove existing success modal if any
    const existingModal = document.getElementById('successModal');
    if (existingModal) {
        existingModal.remove();
    }

    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Populate member/participant details
    if (isTeamRegistration) {
        populateTeamMembersList();
    } else {
        populateParticipantDetails();
    }

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('successModal'));
    modal.show();
}

// Legacy showSuccessMessage function removed - using showEnhancedSuccessModal instead

/**
 * Populate team members list in success modal
 */
function populateTeamMembersList() {
    const membersList = document.getElementById('teamMembersList');
    if (!membersList) return;

    const memberForms = document.querySelectorAll('.member-form');
    let membersHtml = '';

    memberForms.forEach((form, index) => {
        const memberNumber = index + 1;
        const isTeamLeader = memberNumber === 1;
        const fullName = document.getElementById(`fullName_${memberNumber}`)?.value || 'N/A';
        const email = document.getElementById(`email_${memberNumber}`)?.value || 'N/A';
        const phone = document.getElementById(`phoneNumber_${memberNumber}`)?.value || 'N/A';
        const age = document.getElementById(`age_${memberNumber}`)?.value || 'N/A';
        const gender = document.getElementById(`gender_${memberNumber}`)?.value || 'N/A';
        const emergencyName = document.getElementById(`emergencyContactName_${memberNumber}`)?.value || '';
        const emergencyPhone = document.getElementById(`emergencyContactPhone_${memberNumber}`)?.value || '';

        membersHtml += `
            <div class="card mb-2">
                <div class="card-header py-2">
                    <strong>${fullName}</strong> -
                    <small class="text-muted">${isTeamLeader ? '👑 Team Leader' : `👤 Member ${memberNumber}`}</small>
                </div>
                <div class="card-body py-2">
                    <div class="row">
                        <div class="col-md-6">
                            <p class="mb-1"><i class="fas fa-envelope me-2"></i>${email}</p>
                            <p class="mb-0"><i class="fas fa-phone me-2"></i>+91 ${phone}</p>
                        </div>
                        <div class="col-md-6">
                            <p class="mb-1"><i class="fas fa-birthday-cake me-2"></i>${age} years old</p>
                            <p class="mb-0"><i class="fas fa-venus-mars me-2"></i>${gender}</p>
                        </div>
                        ${(emergencyName && emergencyPhone) ? `
                        <div class="col-12 mt-2">
                             <p class="mb-0 border-top pt-2"><i class="fas fa-first-aid me-2"></i><strong>Emergency:</strong> ${emergencyName} (${emergencyPhone})</p>
                        </div>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;
    });

    membersList.innerHTML = membersHtml;
}

/**
 * Populate participant details in success modal
 */
function populateParticipantDetails() {
    const participantDetails = document.getElementById('participantDetails');
    if (!participantDetails) return;

    const fullName = document.getElementById('fullName_1')?.value || 'N/A';
    const email = document.getElementById('email_1')?.value || 'N/A';
    const phone = document.getElementById('phoneNumber_1')?.value || 'N/A';
    const age = document.getElementById('age_1')?.value || 'N/A';
    const gender = document.getElementById('gender_1')?.value || 'N/A';
    const emergencyName = document.getElementById('emergencyContactName_1')?.value || 'N/A';
    const emergencyPhone = document.getElementById('emergencyContactPhone_1')?.value || 'N/A';

    participantDetails.innerHTML = `
        <div class="card">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <p class="mb-2"><strong>Name:</strong> ${fullName}</p>
                        <p class="mb-2"><strong>Email:</strong> ${email}</p>
                        <p class="mb-0"><strong>Phone:</strong> +91 ${phone}</p>
                    </div>
                    <div class="col-md-6">
                        <p class="mb-2"><strong>Age:</strong> ${age} years</p>
                        <p class="mb-2"><strong>Gender:</strong> ${gender}</p>
                    </div>
                    <div class="col-12 mt-2">
                        <p class="mb-0 border-top pt-2"><strong>Emergency Contact:</strong> ${emergencyName} (${emergencyPhone})</p>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Print registration details
 */
function printRegistration() {
    // Ensure the success modal is visible
    const successModal = document.getElementById('successModal');
    if (!successModal || !successModal.classList.contains('show')) {
        console.warn('Success modal is not visible, cannot print');
        showAlert('Please ensure the registration confirmation is displayed before printing.', 'warning');
        return;
    }

    // Add a print-specific class to help with styling
    document.body.classList.add('printing');

    // Small delay to ensure styles are applied
    setTimeout(() => {
        try {
            window.print();
        } catch (error) {
            console.error('Print failed:', error);
            showAlert('Print failed. Please try again or use your browser\'s print function.', 'danger');
        } finally {
            // Remove the print class after printing
            setTimeout(() => {
                document.body.classList.remove('printing');
            }, 1000);
        }
    }, 100);
}

/**
 * Share registration details
 */
function shareRegistration() {
    const registrationNumber = document.querySelector('.badge.bg-primary')?.textContent || 'N/A';
    const teamName = window.selectedPlan?.name || 'Treasure Hunt';

    const shareText = `🎉 I just registered for ${teamName}! Registration #${registrationNumber}. Join me for an amazing treasure hunt adventure!`;

    if (navigator.share) {
        navigator.share({
            title: 'Treasure Hunt Registration',
            text: shareText,
            url: window.location.href
        });
    } else {
        // Fallback: copy to clipboard
        navigator.clipboard.writeText(shareText).then(() => {
            showAlert('Registration details copied to clipboard!', 'success');
        });
    }
}

/**
 * Show alert message
 */
function showAlert(message, type = 'info') {
    const alertHtml = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    const container = document.getElementById('registrationFormContainer');
    container.insertAdjacentHTML('afterbegin', alertHtml);
}

/**
 * Reset registration form
 */
function resetRegistrationForm() {
    window.currentStep = 1;
    window.selectedPlan = null;
    uploadedFiles = { photo: null, id: null, medical: null };

    const container = document.getElementById('registrationFormContainer');
    if (container) {
        container.innerHTML = '';
    }
}

/**
 * Stop video playback
 */
function stopVideo() {
    const video = document.getElementById('previewVideo');
    if (video) {
        video.src = '';
    }
}

/**
 * Show modal
 */
function showModal(modalId) {
    const modal = new bootstrap.Modal(document.getElementById(modalId));
    modal.show();
}

/**
 * Hide modal
 */
function hideModal(modalId) {
    const modal = bootstrap.Modal.getInstance(document.getElementById(modalId));
    if (modal) {
        modal.hide();
    }
}

/**
 * Format file size
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Debounce function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
