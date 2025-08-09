/**
 * Treasure Hunt Adventures - Frontend JavaScript
 * Clean, focused implementation for registration flow
 */

// Global variables
window.currentStep = 1;
window.selectedPlan = null;

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
 * Handle registration button click
 */
async function handleRegistrationClick(event) {
    const button = event.target.closest('.register-btn');
    const planId = button.dataset.planId;

    try {
        // Fetch plan data from API
        const response = await fetch(`/api/plans/${planId}`);
        if (response.ok) {
            window.selectedPlan = await response.json();
        } else {
            // Fallback to button data
            window.selectedPlan = {
                id: planId,
                name: button.dataset.planName,
                price: parseFloat(button.dataset.planPrice),
                teamType: button.dataset.teamType || 'INDIVIDUAL',
                teamSize: parseInt(button.dataset.teamSize) || 1
            };
        }

        await loadRegistrationForm();
        showModal('registrationModal');
    } catch (error) {
        console.error('Error loading registration:', error);
        showAlert('Unable to load registration form. Please try again.', 'danger');
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
        container.innerHTML = '<div class="alert alert-danger">Plan ID is missing.</div>';
        return;
    }

    try {
        const response = await fetch(`/register/form/${planId}`);
        if (!response.ok) {
            throw new Error(`Failed to load form: ${response.statusText}`);
        }

        const formHtml = await response.text();
        container.innerHTML = formHtml;

        // Setup form after loading
        setTimeout(() => {
            setupFormEventListeners();
            clearAllValidationErrors(); // Clear any validation errors on form load
            window.currentStep = 1;
            showStep(1);
        }, 100);

    } catch (error) {
        console.error('Error loading form:', error);
        container.innerHTML = '<div class="alert alert-danger">Error loading registration form.</div>';
    }
}

/**
 * Setup form event listeners
 */
function setupFormEventListeners() {
    const form = document.getElementById('registrationForm');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }

    // Step navigation
    document.addEventListener('click', function(e) {
        if (e.target.id === 'nextStepBtn' || e.target.closest('#nextStepBtn')) {
            e.preventDefault();
            nextStep();
        }
        if (e.target.id === 'prevStepBtn' || e.target.closest('#prevStepBtn')) {
            e.preventDefault();
            previousStep();
        }
    });

    // Medical consent checkbox
    const medicalConsent = document.getElementById('medicalConsent');
    if (medicalConsent) {
        medicalConsent.addEventListener('change', function() {
            toggleMedicalCertificateUpload(this.checked);
            validateStep2(); // Re-validate step 2 when consent changes
        });
    }

    // File upload inputs
    ['photoFile', 'idFile', 'medicalFile'].forEach(fileId => {
        const fileInput = document.getElementById(fileId);
        if (fileInput) {
            fileInput.addEventListener('change', function() {
                validateStep2(); // Re-validate step 2 when files change
            });
        }
    });

    // Setup drag and drop for file uploads
    setupDragAndDrop();

    // Setup real-time validation
    setupRealTimeValidation();
}

/**
 * Clear all validation errors from the form
 */
function clearAllValidationErrors() {
    const form = document.getElementById('registrationForm');
    if (!form) return;

    // Remove all validation classes and error messages
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        input.classList.remove('is-valid', 'is-invalid');
        input.removeAttribute('data-touched');

        const fieldContainer = input.closest('.mb-3') || input.closest('.form-group') || input.parentElement;
        const errorElement = fieldContainer.querySelector('.invalid-feedback') || fieldContainer.querySelector('.error-message');
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
    });
}

/**
 * Setup real-time validation for form fields
 */
function setupRealTimeValidation() {
    // Get all form inputs
    const form = document.getElementById('registrationForm');
    if (!form) return;

    // Clear any existing validation errors first
    clearAllValidationErrors();

    // Add real-time validation to all inputs
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        // Mark field as touched when user interacts with it
        input.addEventListener('focus', function() {
            this.setAttribute('data-touched', 'true');
        });

        // Add event listeners for real-time validation
        input.addEventListener('input', function() {
            this.setAttribute('data-touched', 'true');
            validateFieldRealTime(this);
        });

        input.addEventListener('blur', function() {
            this.setAttribute('data-touched', 'true');
            validateFieldRealTime(this);
        });

        input.addEventListener('change', function() {
            this.setAttribute('data-touched', 'true');
            validateFieldRealTime(this);
        });
    });
}

/**
 * Real-time field validation
 */
function validateFieldRealTime(field) {
    const fieldContainer = field.closest('.mb-3') || field.closest('.form-group') || field.parentElement;
    const errorElement = fieldContainer.querySelector('.invalid-feedback') || fieldContainer.querySelector('.error-message');

    // Always remove existing validation classes first
    field.classList.remove('is-valid', 'is-invalid');
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.style.display = 'none';
    }

    // Check if field has been interacted with (has data-touched attribute)
    if (!field.hasAttribute('data-touched')) {
        return true; // Don't validate until user has interacted with the field
    }

    // Skip validation for empty optional fields
    if (!field.hasAttribute('required') && !field.value.trim()) {
        return true;
    }

    let isValid = true;
    let errorMessage = '';

    // Validate based on field type and attributes
    if (field.hasAttribute('required') && !field.value.trim()) {
        isValid = false;
        errorMessage = `${getFieldLabel(field)} is required.`;
    } else if (field.type === 'email' && field.value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(field.value)) {
            isValid = false;
            errorMessage = 'Please enter a valid email address.';
        }
    } else if (field.type === 'tel' && field.value) {
        const phoneRegex = /^[6-9][0-9]{9}$/;
        if (!phoneRegex.test(field.value)) {
            isValid = false;
            errorMessage = 'Please enter a valid 10-digit phone number starting with 6-9.';
        }
    } else if (field.type === 'number' && field.value) {
        const min = parseInt(field.getAttribute('min'));
        const max = parseInt(field.getAttribute('max'));
        const value = parseInt(field.value);

        if (min && value < min) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)} must be at least ${min}.`;
        } else if (max && value > max) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)} must be at most ${max}.`;
        }
    } else if (field.hasAttribute('pattern') && field.value) {
        const pattern = new RegExp(field.getAttribute('pattern'));
        if (!pattern.test(field.value)) {
            isValid = false;
            errorMessage = `Please enter a valid ${getFieldLabel(field).toLowerCase()}.`;
        }
    } else if (field.hasAttribute('maxlength') && field.value.length > parseInt(field.getAttribute('maxlength'))) {
        isValid = false;
        errorMessage = `${getFieldLabel(field)} must be less than ${field.getAttribute('maxlength')} characters.`;
    } else if (field.hasAttribute('minlength') && field.value.length < parseInt(field.getAttribute('minlength'))) {
        isValid = false;
        errorMessage = `${getFieldLabel(field)} must be at least ${field.getAttribute('minlength')} characters.`;
    }

    // Apply validation styling only if field has been touched
    if (isValid) {
        field.classList.add('is-valid');
    } else {
        field.classList.add('is-invalid');
        if (errorElement) {
            errorElement.textContent = errorMessage;
            errorElement.style.display = 'block';
        } else {
            // Create error element if it doesn't exist
            const newErrorElement = document.createElement('div');
            newErrorElement.className = 'invalid-feedback';
            newErrorElement.textContent = errorMessage;
            newErrorElement.style.display = 'block';
            fieldContainer.appendChild(newErrorElement);
        }
    }

    return isValid;
}

/**
 * Get field label for error messages
 */
function getFieldLabel(field) {
    const label = field.closest('.mb-3')?.querySelector('label') ||
                  field.closest('.form-group')?.querySelector('label');
    if (label) {
        return label.textContent.replace('*', '').trim();
    }

    // Fallback to placeholder or field name
    return field.placeholder || field.name || 'Field';
}

/**
 * Setup drag and drop functionality for file uploads
 */
function setupDragAndDrop() {
    const fileUploadAreas = [
        { areaId: 'photoFileUploadArea', inputId: 'photoFile', type: 'photo' },
        { areaId: 'idFileUploadArea', inputId: 'idFile', type: 'id' },
        { areaId: 'medicalFileUploadArea', inputId: 'medicalFile', type: 'medical' }
    ];

    fileUploadAreas.forEach(({ areaId, inputId, type }) => {
        const uploadArea = document.getElementById(areaId);
        const fileInput = document.getElementById(inputId);

        if (!uploadArea || !fileInput) return;

        // Prevent default drag behaviors
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            uploadArea.addEventListener(eventName, preventDefaults, false);
            document.body.addEventListener(eventName, preventDefaults, false);
        });

        // Highlight drop area when item is dragged over it
        ['dragenter', 'dragover'].forEach(eventName => {
            uploadArea.addEventListener(eventName, () => highlight(uploadArea), false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            uploadArea.addEventListener(eventName, () => unhighlight(uploadArea), false);
        });

        // Handle dropped files
        uploadArea.addEventListener('drop', (e) => handleDrop(e, fileInput, type), false);

        // Handle click to upload
        uploadArea.addEventListener('click', () => {
            if (!fileInput.disabled) {
                fileInput.click();
            }
        });
    });
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

function highlight(element) {
    if (!element.classList.contains('disabled')) {
        element.classList.add('drag-over');
        element.style.borderColor = '#007bff';
        element.style.backgroundColor = '#f8f9fa';
    }
}

function unhighlight(element) {
    element.classList.remove('drag-over');
    element.style.borderColor = '';
    element.style.backgroundColor = '';
}

function handleDrop(e, fileInput, type) {
    const dt = e.dataTransfer;
    const files = dt.files;

    if (files.length > 0 && !fileInput.disabled) {
        fileInput.files = files;

        // Trigger change event
        const event = new Event('change', { bubbles: true });
        fileInput.dispatchEvent(event);

        // Handle file upload
        handleFileUpload(fileInput, type, getMaxFileSize(type));
    }
}

function getMaxFileSize(type) {
    switch (type) {
        case 'photo': return 5; // 5MB
        case 'id': return 10; // 10MB
        case 'medical': return 10; // 10MB
        default: return 5; // 5MB default
    }
}

/**
 * Show specific step
 */
function showStep(step) {
    // Hide all steps
    document.querySelectorAll('.form-step').forEach(stepEl => {
        stepEl.classList.add('d-none');
    });

    // Show target step
    const targetStep = document.querySelector(`.form-step[data-step="${step}"]`);
    if (targetStep) {
        targetStep.classList.remove('d-none');
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
}

/**
 * Navigate to next step
 */
function nextStep() {
    if (window.currentStep === 1) {
        if (validateStep1()) {
            showStep(2);
            validateStep2(); // Enable submit button if step 2 is valid
        }
    }
}

/**
 * Robust next step function (called from template)
 */
window.nextStepRobust = function() {
    nextStep();
};

/**
 * Update character count for bio fields
 */
window.updateCharacterCount = function(identifier) {
    let bioField, charCountElement;

    if (identifier === 'individual') {
        bioField = document.getElementById('bio');
        charCountElement = document.getElementById('charCount_individual');
    } else {
        bioField = document.getElementById(`bio_${identifier}`);
        charCountElement = document.getElementById(`charCount_${identifier}`);
    }

    if (bioField && charCountElement) {
        const currentLength = bioField.value.length;
        charCountElement.textContent = currentLength;

        // Add visual feedback for character limit
        if (currentLength > 1800) {
            charCountElement.style.color = '#dc3545'; // Bootstrap danger color
        } else if (currentLength > 1500) {
            charCountElement.style.color = '#fd7e14'; // Bootstrap warning color
        } else {
            charCountElement.style.color = '#6c757d'; // Bootstrap muted color
        }
    }
};

/**
 * Navigate to previous step
 */
function previousStep() {
    if (window.currentStep === 2) {
        showStep(1);
    }
}

/**
 * Validate step 1
 */
function validateStep1() {
    const form = document.getElementById('registrationForm');
    const step1Fields = form.querySelectorAll('[data-step="1"] input[required], [data-step="1"] select[required]');

    let isValid = true;
    let invalidFields = [];

    step1Fields.forEach(field => {
        if (!field.checkValidity()) {
            field.classList.add('is-invalid');
            isValid = false;

            // Get field label for better error messaging
            const label = form.querySelector(`label[for="${field.id}"]`);
            if (label) {
                invalidFields.push(label.textContent.replace('*', '').trim());
            }
        } else {
            field.classList.remove('is-invalid');
        }
    });

    // Additional validation for team registrations
    const isTeamBased = form.querySelector('input[name="isTeamBased"]')?.value === 'true';
    if (isTeamBased) {
        const teamNameField = form.querySelector('input[name="teamName"]');
        if (teamNameField && (!teamNameField.value || teamNameField.value.trim().length < 2)) {
            teamNameField.classList.add('is-invalid');
            isValid = false;
            invalidFields.push('Team Name');
        }
    }

    if (!isValid) {
        if (invalidFields.length > 0) {
            showAlert(`Please fill in the following required fields correctly: ${invalidFields.join(', ')}`, 'danger');
        } else {
            showAlert('Please fill in all required fields correctly.', 'danger');
        }
    }

    return isValid;
}

/**
 * Validate step 2
 */
function validateStep2() {
    const form = document.getElementById('registrationForm');
    const submitBtn = document.getElementById('submitBtn');

    if (!form || !submitBtn) return false;

    // Check medical consent
    const medicalConsent = document.getElementById('medicalConsent');
    const medicalConsentGiven = medicalConsent && medicalConsent.checked;

    // Check required files
    const photoFile = document.getElementById('photoFile');
    const idFile = document.getElementById('idFile');
    const medicalFile = document.getElementById('medicalFile');

    let isValid = true;

    // Medical consent is always required
    if (!medicalConsentGiven) {
        isValid = false;
    }

    // Photo file is always required
    if (!photoFile || !photoFile.files[0]) {
        isValid = false;
    }

    // ID file is always required
    if (!idFile || !idFile.files[0]) {
        isValid = false;
    }

    // Medical file is required only if consent is not given
    if (!medicalConsentGiven && (!medicalFile || !medicalFile.files[0])) {
        isValid = false;
    }

    // Enable/disable submit button
    submitBtn.disabled = !isValid;

    return isValid;
}

/**
 * Navigate to previous step
 */
function previousStep() {
    if (window.currentStep === 2) {
        showStep(1);
    }
}

/**
 * Toggle medical certificate upload
 */
function toggleMedicalCertificateUpload(consentGiven) {
    const fileInput = document.getElementById('medicalFile');
    const uploadArea = document.getElementById('medicalFileUploadArea');
    const requiredText = document.getElementById('medicalCertificateRequired');
    const helperText = document.getElementById('medicalCertificateHelperText');
    const uploadText = document.getElementById('medicalFileUploadText');

    if (fileInput) {
        fileInput.required = !consentGiven;
        fileInput.disabled = consentGiven;
    }

    if (uploadArea) {
        if (consentGiven) {
            uploadArea.classList.add('disabled');
            uploadArea.style.cursor = 'not-allowed';
        } else {
            uploadArea.classList.remove('disabled');
            uploadArea.style.cursor = 'pointer';
        }
    }

    if (requiredText) {
        if (consentGiven) {
            requiredText.innerHTML = '<small class="text-success">(Optional - Medical consent given)</small>';
        } else {
            requiredText.innerHTML = '<small class="text-danger">(Required - PDF only, max 5MB)</small>';
        }
    }

    if (helperText) {
        if (consentGiven) {
            helperText.textContent = 'Medical certificate is optional since you have given medical consent above.';
            helperText.className = 'form-text mt-2 text-success';
        } else {
            helperText.textContent = 'Medical certificate is required unless medical consent is given above.';
            helperText.className = 'form-text mt-2';
        }
    }

    if (uploadText) {
        if (consentGiven) {
            uploadText.textContent = 'Medical certificate not required';
        } else {
            uploadText.textContent = 'Click to upload or drag & drop';
        }
    }
}

/**
 * Handle file upload
 */
function handleFileUpload(input, fileType, maxSizeMB) {
    const file = input.files[0];
    if (!file) return;

    // Validate file size
    const maxSize = maxSizeMB * 1024 * 1024;
    if (file.size > maxSize) {
        alert(`File size exceeds ${maxSizeMB}MB limit.`);
        input.value = '';
        return;
    }

    // Update UI
    const fileInfo = document.getElementById(`${fileType}FileInfo`);
    if (fileInfo) {
        fileInfo.innerHTML = `<small class="text-success"><i class="fas fa-check me-1"></i>${file.name}</small>`;
        fileInfo.classList.remove('d-none');
    }

    // Re-validate step 2 when files are uploaded
    if (window.currentStep === 2) {
        validateStep2();
    }
}

/**
 * Handle form submission
 */
async function handleFormSubmit(event) {
    event.preventDefault();

    const submitBtn = document.getElementById('submitBtn');
    const spinner = submitBtn.querySelector('.spinner-border');

    // Show loading
    submitBtn.disabled = true;
    spinner.classList.remove('d-none');

    try {
        const formData = new FormData(event.target);

        // Add files
        ['photoFile', 'idFile', 'medicalFile'].forEach(fileId => {
            const fileInput = document.getElementById(fileId);
            if (fileInput && fileInput.files[0]) {
                formData.append(fileId, fileInput.files[0]);
            }
        });

        // Determine if this is a team registration
        const isTeamBased = formData.get('isTeamBased') === 'true';
        const endpoint = isTeamBased ? '/api/register/team' : '/api/register';

        const response = await fetch(endpoint, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (response.ok && result.success) {
            showSuccessModal(result);
            hideModal('registrationModal');
        } else {
            showAlert(result.message || 'Registration failed. Please try again.', 'danger');
        }

    } catch (error) {
        console.error('Registration error:', error);
        showAlert('An error occurred. Please try again.', 'danger');
    } finally {
        submitBtn.disabled = false;
        spinner.classList.add('d-none');
    }
}

/**
 * Show/hide modals
 */
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    }
}

function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        const bsModal = bootstrap.Modal.getInstance(modal);
        if (bsModal) bsModal.hide();
    }
}

/**
 * Show alert
 */
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 400px;';
    alertDiv.innerHTML = `${message}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => alertDiv.remove(), 5000);
}

/**
 * Show success modal with comprehensive registration details
 */
function showSuccessModal(result) {
    const successContent = document.getElementById('successModalContent');
    if (successContent) {
        // Extract form data to show in confirmation
        const formData = extractRegistrationFormData();

        successContent.innerHTML = createSuccessModalContent(result, formData);
    }
    showModal('successModal');
}

/**
 * Extract registration form data for confirmation display
 */
function extractRegistrationFormData() {
    const form = document.getElementById('registrationForm');
    if (!form) return {};

    const data = {};

    // Get form data
    const formData = new FormData(form);
    for (let [key, value] of formData.entries()) {
        data[key] = value;
    }

    // Get selected plan info
    if (window.selectedPlan) {
        data.planName = window.selectedPlan.name;
        data.planPrice = window.selectedPlan.priceInr;
        data.isTeamBased = window.selectedPlan.teamType === 'TEAM';
    }

    return data;
}

/**
 * Create comprehensive success modal content
 */
function createSuccessModalContent(result, formData) {
    const isTeamRegistration = formData.isTeamBased === true || formData.isTeamBased === 'true';

    let content = `
        <div class="text-center mb-4">
            <i class="fas fa-check-circle text-success mb-3" style="font-size: 3rem;"></i>
            <h4 class="text-success">Registration Successful!</h4>
            <div class="alert alert-success">
                <strong>Registration Number: ${result.registrationNumber}</strong>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6">
                <h6 class="text-primary mb-3">
                    <i class="fas fa-info-circle me-2"></i>Registration Information
                </h6>
                <table class="table table-sm table-borderless">
                    <tr>
                        <td><strong>Plan:</strong></td>
                        <td>${result.planName || formData.planName || 'N/A'}</td>
                    </tr>
                    <tr>
                        <td><strong>Price:</strong></td>
                        <td>₹${formData.planPrice || 'N/A'}</td>
                    </tr>
                    <tr>
                        <td><strong>Registration Date:</strong></td>
                        <td>${new Date().toLocaleDateString('en-IN', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                        })}</td>
                    </tr>
                    <tr>
                        <td><strong>Status:</strong></td>
                        <td><span class="badge bg-warning">Pending Confirmation</span></td>
                    </tr>
                </table>
            </div>
            <div class="col-md-6">
                <h6 class="text-primary mb-3">
                    <i class="fas fa-envelope me-2"></i>Next Steps
                </h6>
                <ul class="list-unstyled">
                    <li class="mb-2">
                        <i class="fas fa-check text-success me-2"></i>
                        Confirmation email will be sent shortly
                    </li>
                    <li class="mb-2">
                        <i class="fas fa-clock text-warning me-2"></i>
                        Our team will review your application
                    </li>
                    <li class="mb-2">
                        <i class="fas fa-phone text-info me-2"></i>
                        We'll contact you within 24-48 hours
                    </li>
                </ul>
            </div>
        </div>

        <hr class="my-4">
    `;

    if (isTeamRegistration && formData.teamName) {
        content += `
            <h6 class="text-primary mb-3">
                <i class="fas fa-users me-2"></i>Team Registration Details
            </h6>
            <div class="card mb-3">
                <div class="card-body">
                    <h6 class="card-title">${formData.teamName}</h6>
                    <p class="text-muted mb-0">Team registration submitted successfully</p>
                </div>
            </div>
        `;
    } else {
        content += `
            <h6 class="text-primary mb-3">
                <i class="fas fa-user me-2"></i>Your Registration Details
            </h6>
            <div class="row">
                <div class="col-md-6">
                    <table class="table table-sm table-borderless">
                        <tr>
                            <td><strong>Name:</strong></td>
                            <td>${formData.fullName || 'N/A'}</td>
                        </tr>
                        <tr>
                            <td><strong>Email:</strong></td>
                            <td>${formData.email || 'N/A'}</td>
                        </tr>
                        <tr>
                            <td><strong>Phone:</strong></td>
                            <td>+91 ${formData.phoneNumber || 'N/A'}</td>
                        </tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="table table-sm table-borderless">
                        <tr>
                            <td><strong>Age:</strong></td>
                            <td>${formData.age || 'N/A'}</td>
                        </tr>
                        <tr>
                            <td><strong>Gender:</strong></td>
                            <td>${formData.gender || 'N/A'}</td>
                        </tr>
                        <tr>
                            <td><strong>Emergency Contact:</strong></td>
                            <td>${formData.emergencyContactName || 'N/A'}</td>
                        </tr>
                    </table>
                </div>
            </div>
        `;
    }

    content += `
        <div class="alert alert-info mt-3">
            <i class="fas fa-info-circle me-2"></i>
            <strong>Important:</strong> Please save your registration number <strong>${result.registrationNumber}</strong> for future reference.
        </div>
    `;

    return content;
}

// Additional helper functions
function setupPlanFilters() {
    const filterButtons = document.querySelectorAll('[data-filter]');
    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            const filter = this.dataset.filter;

            // Update active button
            filterButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // Filter plans
            const planCards = document.querySelectorAll('.plan-card');
            planCards.forEach(card => {
                if (filter === 'all' || card.dataset.difficulty === filter) {
                    card.style.display = 'block';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    });
}

function setupSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
}

function loadPlans() {
    fetch('/api/plans')
        .then(response => response.json())
        .then(plans => {
            // Update plan cards with fresh data if needed
            console.log('Plans loaded:', plans.length);
        })
        .catch(error => console.error('Error loading plans:', error));
}

function handlePreviewClick(event) {
    const button = event.target.closest('.preview-btn');
    const planId = button.dataset.planId;

    fetch(`/api/plans/${planId}`)
        .then(response => response.json())
        .then(plan => {
            const videoUrl = plan.previewVideoUrl || 'https://www.youtube.com/embed/dQw4w9WgXcQ';
            document.getElementById('previewVideo').src = videoUrl;
            showModal('videoModal');
        })
        .catch(error => {
            console.error('Error loading preview:', error);
            document.getElementById('previewVideo').src = 'https://www.youtube.com/embed/dQw4w9WgXcQ';
            showModal('videoModal');
        });
}

function resetRegistrationForm() {
    window.selectedPlan = null;
    window.currentStep = 1;
    const container = document.getElementById('registrationFormContainer');
    if (container) {
        container.innerHTML = '';
    }
}

function stopVideo() {
    const video = document.getElementById('previewVideo');
    if (video) {
        video.src = '';
    }
}

/**
 * Update the consent label color based on checkbox state
 * @param {boolean} isChecked - Whether the checkbox is checked
 */
function updateConsentLabelColor(isChecked) {
    const consentLabel = document.querySelector('.consent-label-required');
    if (consentLabel) {
        if (isChecked) {
            consentLabel.style.color = '#198754'; // Success green
        } else {
            consentLabel.style.color = '#dc3545'; // Danger red
        }
    }
}

function closeSuccessModal() {
    hideModal('successModal');
    // Optionally reload the page to refresh plan availability
    setTimeout(() => {
        window.location.reload();
    }, 500);
}
