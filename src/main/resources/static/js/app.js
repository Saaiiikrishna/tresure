/**
 * Treasure Hunt Adventures - Frontend JavaScript
 * Handles registration form, file uploads, and UI interactions
 */

// Global variables
window.currentStep = 1;
let selectedPlan = null;

// Robust step navigation functions
window.showStepRobust = function(step) {
    console.log('showStepRobust called with step:', step);

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
        console.log('Step', step, 'is now visible');

        // Setup file uploads for step 2
        if (step === 2) {
            setTimeout(() => {
                setupFileUploads();
                console.log('File uploads setup for step 2');
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
    console.log('window.currentStep set to:', window.currentStep);
};

window.nextStepRobust = function() {
    console.log('nextStepRobust called, currentStep:', window.currentStep);

    // Ensure currentStep is set
    if (typeof window.currentStep === 'undefined' || window.currentStep === null) {
        window.currentStep = 1;
        console.log('currentStep was undefined, set to 1');
    }

    if (window.currentStep === 1) {
        // Validate step 1
        const fullName = document.getElementById('fullName')?.value?.trim();
        const age = document.getElementById('age')?.value;
        const gender = document.getElementById('gender')?.value;
        const email = document.getElementById('email')?.value?.trim();
        const phoneNumber = document.getElementById('phoneNumber')?.value?.trim();
        const emergencyContactName = document.getElementById('emergencyContactName')?.value?.trim();
        const emergencyContactPhone = document.getElementById('emergencyContactPhone')?.value?.trim();

        if (!fullName || !age || !gender || gender === '' || !email || !phoneNumber || !emergencyContactName || !emergencyContactPhone) {
            alert('Please fill in all required fields.');
            return;
        }

        console.log('Step 1 validated, moving to step 2');
        window.showStepRobust(2);
    }
};

// Debug function to help identify the issue
window.debugNextStep = function() {
    console.log('=== DEBUG NEXT STEP CLICKED ===');
    alert('Button clicked! Check console for details.');

    console.log('Current step:', window.currentStep);
    console.log('Form elements check:');

    const fullName = document.getElementById('fullName');
    const age = document.getElementById('age');
    const gender = document.getElementById('gender');
    const email = document.getElementById('email');
    const phoneNumber = document.getElementById('phoneNumber');
    const emergencyContactName = document.getElementById('emergencyContactName');
    const emergencyContactPhone = document.getElementById('emergencyContactPhone');

    console.log('fullName element:', fullName, 'value:', fullName?.value);
    console.log('age element:', age, 'value:', age?.value);
    console.log('gender element:', gender, 'value:', gender?.value);
    console.log('email element:', email, 'value:', email?.value);
    console.log('phoneNumber element:', phoneNumber, 'value:', phoneNumber?.value);
    console.log('emergencyContactName element:', emergencyContactName, 'value:', emergencyContactName?.value);
    console.log('emergencyContactPhone element:', emergencyContactPhone, 'value:', emergencyContactPhone?.value);

    // Try to call the original function
    try {
        console.log('Attempting to call nextStepRobust...');
        window.nextStepRobust();
    } catch (error) {
        console.error('Error calling nextStepRobust:', error);
        alert('Error: ' + error.message);
    }
};

let uploadedFiles = {
    photo: null,
    id: null,
    medical: null
};

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
 * Load plans from API
 */
async function loadPlans() {
    try {
        const response = await fetch('/api/plans');
        if (response.ok) {
            const plans = await response.json();
            updatePlanCards(plans);
        }
    } catch (error) {
        console.error('Error loading plans:', error);
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
function handleRegistrationClick(event) {
    const button = event.target.closest('.register-btn');
    const planId = button.dataset.planId;
    const planName = button.dataset.planName;
    const planPrice = button.dataset.planPrice;

    selectedPlan = {
        id: planId,
        name: planName,
        price: planPrice
    };

    loadRegistrationForm();
    showModal('registrationModal');
}

/**
 * Handle preview button click
 */
function handlePreviewClick(event) {
    const button = event.target.closest('.preview-btn');
    const planId = button.dataset.planId;
    
    // Load preview video (placeholder URL)
    const videoUrl = `https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1`;
    document.getElementById('previewVideo').src = videoUrl;
    
    showModal('videoModal');
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
 * Load registration form
 */
function loadRegistrationForm() {
    const container = document.getElementById('registrationFormContainer');
    if (!container) return;

    container.innerHTML = `
        <div class="registration-form">
            <!-- Progress Steps -->
            <div class="progress-steps mb-4">
                <div class="progress-step active" data-step="1">
                    <div class="step-circle">1</div>
                    <div class="step-label">Personal Info</div>
                </div>
                <div class="progress-step" data-step="2">
                    <div class="step-circle">2</div>
                    <div class="step-label">Documents & Consent</div>
                </div>
            </div>

            <!-- Plan Summary -->
            <div class="alert alert-info mb-4">
                <h6><i class="fas fa-info-circle me-2"></i>Selected Plan: ${selectedPlan.name}</h6>
                <p class="mb-0">Price: $${selectedPlan.price} per person</p>
            </div>

            <!-- Registration Form -->
            <form id="registrationForm" enctype="multipart/form-data">
                <input type="hidden" name="planId" value="${selectedPlan.id}">
                
                <!-- Step 1: Personal Information -->
                <div class="form-step" data-step="1">
                    <h5 class="mb-3">Personal Information</h5>
                    
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="fullName" class="form-label">Full Name *</label>
                            <input type="text" class="form-control" id="fullName" name="fullName" required maxlength="255">
                        </div>
                        <div class="col-md-6">
                            <label for="age" class="form-label">Age *</label>
                            <input type="number" class="form-control" id="age" name="age" required min="18" max="65">
                        </div>
                        <div class="col-md-6">
                            <label for="gender" class="form-label">Gender *</label>
                            <select class="form-control" id="gender" name="gender" required>
                                <option value="">Select Gender</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="email" class="form-label">Email Address *</label>
                            <input type="email" class="form-control" id="email" name="email" required maxlength="255">
                        </div>
                        <div class="col-md-6">
                            <label for="phoneNumber" class="form-label">Phone Number *</label>
                            <input type="tel" class="form-control" id="phoneNumber" name="phoneNumber" required maxlength="20">
                        </div>
                        <div class="col-md-6">
                            <label for="emergencyContactName" class="form-label">Emergency Contact Name *</label>
                            <input type="text" class="form-control" id="emergencyContactName" name="emergencyContactName" required maxlength="255">
                        </div>
                        <div class="col-md-6">
                            <label for="emergencyContactPhone" class="form-label">Emergency Contact Phone *</label>
                            <input type="tel" class="form-control" id="emergencyContactPhone" name="emergencyContactPhone" required maxlength="20">
                        </div>
                    </div>
                    
                    <div class="text-end mt-4">
                        <button type="button" id="nextStepBtn" class="btn btn-primary" onclick="debugNextStep()">
                            Next Step <i class="fas fa-arrow-right ms-2"></i>
                        </button>
                    </div>
                </div>

                <!-- Step 2: Documents & Consent -->
                <div class="form-step d-none" data-step="2">
                    <h5 class="mb-3">Document Upload & Medical Consent</h5>
                    
                    <!-- Medical Consent -->
                    <div class="mb-4">
                        <div class="card">
                            <div class="card-header">
                                <h6 class="mb-0">Medical Consent & Risk Acknowledgment</h6>
                            </div>
                            <div class="card-body">
                                <div class="medical-consent-text" style="max-height: 200px; overflow-y: auto; background-color: #f8f9fa; padding: 1rem; border-radius: 0.5rem; margin-bottom: 1rem;">
                                    <p><strong>IMPORTANT: Please read carefully before proceeding.</strong></p>
                                    <p>By participating in treasure hunt activities, I acknowledge and understand that:</p>
                                    <ul>
                                        <li>Physical activities involve inherent risks including but not limited to injury, illness, or property damage</li>
                                        <li>I am in good physical health and have no medical conditions that would prevent safe participation</li>
                                        <li>I will follow all safety instructions provided by guides and staff</li>
                                        <li>I understand that activities may involve walking on uneven terrain, climbing, and problem-solving under time pressure</li>
                                        <li>Weather conditions may affect the nature and safety of activities</li>
                                        <li>I am responsible for my own safety and the safety of others in my group</li>
                                        <li>Emergency medical treatment may be necessary and I consent to such treatment</li>
                                        <li>I have adequate insurance coverage for any potential medical expenses</li>
                                        <li>I release Treasure Hunt Adventures from liability for injuries or damages that may occur during participation</li>
                                        <li>I understand that activities may be modified or cancelled due to safety concerns</li>
                                    </ul>
                                    <p>Participants with medical conditions, injuries, or physical limitations must consult with their physician before participating and must inform our staff of any relevant conditions.</p>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="medicalConsent" name="medicalConsentGiven" required>
                                    <label class="form-check-label" for="medicalConsent">
                                        <strong>I have read, understood, and agree to the medical consent and risk acknowledgment above. *</strong>
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- File Uploads -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <label class="form-label">Passport Photo * <small class="text-muted">(JPG, PNG, max 2MB)</small></label>
                            <div class="file-upload-area" data-file-type="photo">
                                <i class="fas fa-camera fs-2 text-muted mb-2"></i>
                                <p class="mb-2">Click to upload or drag & drop</p>
                                <input type="file" name="photoFile" accept="image/jpeg,image/jpg,image/png" class="d-none" required>
                                <div class="file-info d-none"></div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Government ID * <small class="text-muted">(PDF, JPG, max 5MB)</small></label>
                            <div class="file-upload-area" data-file-type="id">
                                <i class="fas fa-id-card fs-2 text-muted mb-2"></i>
                                <p class="mb-2">Click to upload or drag & drop</p>
                                <input type="file" name="idFile" accept="application/pdf,image/jpeg,image/jpg" class="d-none" required>
                                <div class="file-info d-none"></div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Medical Certificate * <small class="text-muted">(PDF only, max 5MB)</small></label>
                            <div class="file-upload-area" data-file-type="medical">
                                <i class="fas fa-file-medical fs-2 text-muted mb-2"></i>
                                <p class="mb-2">Click to upload or drag & drop</p>
                                <input type="file" name="medicalFile" accept="application/pdf" class="d-none" required>
                                <div class="file-info d-none"></div>
                            </div>
                        </div>
                    </div>

                    <div class="d-flex justify-content-between">
                        <button type="button" class="btn btn-outline-secondary" onclick="previousStep()">
                            <i class="fas fa-arrow-left me-2"></i> Previous
                        </button>
                        <button type="submit" class="btn btn-success" id="submitBtn" disabled>
                            <span class="spinner-border spinner-border-sm d-none me-2"></span>
                            Submit Registration
                        </button>
                    </div>
                </div>
            </form>
        </div>
    `;

    // Setup form event listeners
    setupFormEventListeners();

    // Initialize form to show Step 1 - use robust functions
    setTimeout(() => {
        window.currentStep = 1;
        window.showStepRobust(1);
        console.log('Form initialized with step 1 using robust functions');
    }, 200);
}

/**
 * Setup form event listeners
 */
function setupFormEventListeners() {
    const form = document.getElementById('registrationForm');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
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

        if (e.target && (e.target.id === 'submitRegistrationBtn' || e.target.closest('#submitRegistrationBtn'))) {
            console.log('Submit button clicked via delegation');
            e.preventDefault();
            e.stopPropagation();
            submitRegistration();
            return false;
        }
    });

    // Method 2: Direct event listeners with polling
    function setupDirectListeners() {
        const nextBtn = document.getElementById('nextStepBtn');
        if (nextBtn) {
            console.log('Found nextStepBtn, adding direct listener');
            nextBtn.onclick = function(e) {
                console.log('Next step button clicked via direct listener');
                e.preventDefault();
                e.stopPropagation();
                window.nextStepRobust();
                return false;
            };
        }

        const prevBtn = document.getElementById('prevStepBtn');
        if (prevBtn) {
            console.log('Found prevStepBtn, adding direct listener');
            prevBtn.onclick = function(e) {
                console.log('Previous step button clicked via direct listener');
                e.preventDefault();
                e.stopPropagation();
                previousStep();
                return false;
            };
        }

        const submitBtn = document.getElementById('submitRegistrationBtn');
        if (submitBtn) {
            console.log('Found submitRegistrationBtn, adding direct listener');
            submitBtn.onclick = function(e) {
                console.log('Submit button clicked via direct listener');
                e.preventDefault();
                e.stopPropagation();
                submitRegistration();
                return false;
            };
        }
    }

    // Setup direct listeners immediately and with polling
    setupDirectListeners();
    setTimeout(setupDirectListeners, 500);
    setTimeout(setupDirectListeners, 1000);

    // File upload areas - setup after form is loaded
    setupFileUploads();

    // Form validation
    setupFormValidation();
}

/**
 * Setup all file upload areas
 */
function setupFileUploads() {
    // Wait for DOM to be ready and then setup file uploads
    setTimeout(() => {
        const uploadAreas = document.querySelectorAll('.file-upload-area');
        uploadAreas.forEach(area => {
            setupFileUpload(area);
        });
    }, 100);
}

/**
 * Setup file upload functionality
 */
function setupFileUpload(uploadArea) {
    // Check if already set up to prevent duplicate event listeners
    if (uploadArea.dataset.uploadSetup === 'true') {
        return;
    }

    const fileInput = uploadArea.querySelector('input[type="file"]');
    const fileInfo = uploadArea.querySelector('.file-info');

    if (!fileInput || !fileInfo) {
        console.error('File input or file info element not found in upload area');
        return;
    }

    // Click to upload
    const clickHandler = (e) => {
        e.preventDefault();
        e.stopPropagation();
        fileInput.click();
    };
    uploadArea.addEventListener('click', clickHandler);

    // File selection
    const changeHandler = function() {
        handleFileSelection(this, uploadArea, fileInfo);
    };
    fileInput.addEventListener('change', changeHandler);

    // Drag and drop
    const dragOverHandler = function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.add('dragover');
    };
    uploadArea.addEventListener('dragover', dragOverHandler);

    const dragLeaveHandler = function() {
        this.classList.remove('dragover');
    };
    uploadArea.addEventListener('dragleave', dragLeaveHandler);

    const dropHandler = function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            fileInput.files = files;
            handleFileSelection(fileInput, uploadArea, fileInfo);
        }
    };
    uploadArea.addEventListener('drop', dropHandler);

    // Mark as set up
    uploadArea.dataset.uploadSetup = 'true';
}

/**
 * Handle file selection
 */
function handleFileSelection(input, uploadArea, fileInfo) {
    const file = input.files[0];
    if (!file) return;

    const fileType = uploadArea.dataset.fileType;
    const maxSize = fileType === 'photo' ? 2 * 1024 * 1024 : 5 * 1024 * 1024; // 2MB for photos, 5MB for documents

    // Validate file size
    if (file.size > maxSize) {
        showAlert('File size exceeds the maximum allowed size.', 'danger');
        input.value = '';
        return;
    }

    // Update UI
    fileInfo.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
            <span><i class="fas fa-file me-2"></i>${file.name}</span>
            <span class="text-muted">${formatFileSize(file.size)}</span>
        </div>
    `;
    fileInfo.classList.remove('d-none');
    uploadArea.classList.add('border-success');

    // Store file reference
    uploadedFiles[fileType] = file;

    // Check if all files are uploaded
    checkFormCompletion();
}

/**
 * Setup form validation
 */
function setupFormValidation() {
    const form = document.getElementById('registrationForm');
    const inputs = form.querySelectorAll('input[required], select[required]');

    inputs.forEach(input => {
        input.addEventListener('blur', validateField);
        input.addEventListener('input', validateField);
    });
}

/**
 * Validate individual field
 */
function validateField(event) {
    const field = event.target;
    const isValid = field.checkValidity();

    if (isValid) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
    }
}

/**
 * Check if form is complete
 */
function checkFormCompletion() {
    const step2 = document.querySelector('[data-step="2"]');
    if (!step2 || step2.classList.contains('d-none')) return;

    const medicalConsent = document.getElementById('medicalConsent').checked;
    const allFilesUploaded = Object.values(uploadedFiles).every(file => file !== null);
    const submitBtn = document.getElementById('submitBtn');

    submitBtn.disabled = !(medicalConsent && allFilesUploaded);
}

/**
 * Navigate to next step
 */
function nextStep() {
    console.log('nextStep called, currentStep:', window.currentStep);

    // Force set currentStep if undefined
    if (typeof window.currentStep === 'undefined') {
        window.currentStep = 1;
        console.log('currentStep was undefined, set to 1');
    }

    if (window.currentStep === 1) {
        if (validateStep1()) {
            console.log('Step 1 validated, moving to step 2');

            // Use the working manual approach
            try {
                // Hide all steps
                document.querySelectorAll('.form-step').forEach(stepEl => {
                    stepEl.classList.add('d-none');
                });

                // Show step 2
                const step2 = document.querySelector('.form-step[data-step="2"]');
                if (step2) {
                    step2.classList.remove('d-none');
                    step2.classList.add('fade-in');
                    console.log('Step 2 is now visible');

                    // Setup file uploads
                    setTimeout(() => {
                        setupFileUploads();
                        console.log('File uploads setup for step 2');
                    }, 100);
                } else {
                    console.error('Step 2 element not found');
                }

                // Update progress
                updateProgress(2);
                window.currentStep = 2;
                console.log('window.currentStep set to:', window.currentStep);

            } catch (error) {
                console.error('Error in nextStep:', error);
            }
        } else {
            console.log('Step 1 validation failed');
        }
    } else {
        console.log('Not on step 1, currentStep:', window.currentStep);
    }
}

/**
 * Navigate to previous step
 */
function previousStep() {
    if (currentStep === 2) {
        showStep(1);
    }
}

/**
 * Show specific step
 */
function showStep(step) {
    console.log('showStep called with step:', step);

    // Hide all steps
    document.querySelectorAll('.form-step').forEach(stepEl => {
        stepEl.classList.add('d-none');
    });

    // Show target step - specifically target form steps, not progress indicators
    const targetStep = document.querySelector(`.form-step[data-step="${step}"]`);
    console.log('Target step element found:', !!targetStep);

    if (targetStep) {
        targetStep.classList.remove('d-none');
        targetStep.classList.add('fade-in');
        console.log('Step', step, 'is now visible');

        // Setup file uploads for step 2
        if (step === 2) {
            setTimeout(() => {
                setupFileUploads();
                console.log('File uploads setup for step 2');
            }, 100);
        }
    } else {
        console.error('Target step element not found for step:', step);
    }

    // Update progress
    updateProgress(step);
    window.currentStep = step;
    console.log('window.currentStep set to:', window.currentStep);
}

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
        const formData = new FormData(event.target);
        
        const response = await fetch('/api/register', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            showSuccessMessage(result);
            hideModal('registrationModal');
        } else {
            showAlert(result.message || 'Registration failed. Please try again.', 'danger');
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
 * Show success message
 */
function showSuccessMessage(result) {
    const message = `
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <h5><i class="fas fa-check-circle me-2"></i>Registration Successful!</h5>
            <p class="mb-2">${result.message}</p>
            <p class="mb-0"><strong>Registration Number:</strong> ${result.registrationNumber}</p>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    // Insert at top of page
    const container = document.querySelector('.container');
    container.insertAdjacentHTML('afterbegin', message);
    
    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
    currentStep = 1;
    selectedPlan = null;
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
