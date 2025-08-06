/**
 * Modal User Input Persistence System
 * Caches user inputs in localStorage to prevent data loss and reduce database writes
 * Only writes to database after final submission
 */

class ModalPersistence {
    constructor() {
        this.storageKey = 'treasureHunt_modalData';
        this.currentPlanId = null;
        this.autoSaveInterval = null;
        this.autoSaveDelay = 2000; // 2 seconds
    }

    /**
     * Initialize persistence for a specific plan
     */
    init(planId) {
        this.currentPlanId = planId;
        this.setupAutoSave();
        this.loadSavedData();
        console.log('📦 Modal persistence initialized for plan:', planId);
    }

    /**
     * Get storage key for current plan
     */
    getStorageKey() {
        return `${this.storageKey}_${this.currentPlanId}`;
    }

    /**
     * Save current form data to localStorage
     */
    saveFormData() {
        if (!this.currentPlanId) return;

        try {
            const formData = this.extractFormData();
            const storageData = {
                planId: this.currentPlanId,
                timestamp: Date.now(),
                step: this.getCurrentStep(),
                data: formData
            };

            localStorage.setItem(this.getStorageKey(), JSON.stringify(storageData));
            console.log('💾 Form data saved to localStorage:', Object.keys(formData).length, 'fields');
        } catch (error) {
            console.error('❌ Error saving form data:', error);
        }
    }

    /**
     * Load saved data from localStorage and populate form
     */
    loadSavedData() {
        if (!this.currentPlanId) return;

        try {
            const savedData = localStorage.getItem(this.getStorageKey());
            if (!savedData) return;

            const parsedData = JSON.parse(savedData);
            
            // Check if data is not too old (24 hours)
            const maxAge = 24 * 60 * 60 * 1000; // 24 hours
            if (Date.now() - parsedData.timestamp > maxAge) {
                this.clearSavedData();
                return;
            }

            this.populateForm(parsedData.data);
            console.log('📥 Loaded saved form data:', Object.keys(parsedData.data).length, 'fields');
        } catch (error) {
            console.error('❌ Error loading saved data:', error);
            this.clearSavedData();
        }
    }

    /**
     * Extract all form data from the modal
     */
    extractFormData() {
        const formData = {};
        const form = document.getElementById('registrationForm');
        if (!form) return formData;

        // Get all input, select, and textarea elements
        const elements = form.querySelectorAll('input, select, textarea');
        
        elements.forEach(element => {
            if (element.name && element.name !== '') {
                if (element.type === 'checkbox') {
                    formData[element.name] = element.checked;
                } else if (element.type === 'radio') {
                    if (element.checked) {
                        formData[element.name] = element.value;
                    }
                } else if (element.type === 'file') {
                    // Don't save file data, just note that a file was selected
                    formData[element.name + '_hasFile'] = element.files.length > 0;
                } else {
                    formData[element.name] = element.value;
                }
            }
        });

        return formData;
    }

    /**
     * Populate form with saved data
     */
    populateForm(data) {
        const form = document.getElementById('registrationForm');
        if (!form) return;

        Object.keys(data).forEach(fieldName => {
            const element = form.querySelector(`[name="${fieldName}"]`);
            if (!element) return;

            const value = data[fieldName];

            if (element.type === 'checkbox') {
                element.checked = value === true;
            } else if (element.type === 'radio') {
                if (element.value === value) {
                    element.checked = true;
                }
            } else if (element.type !== 'file') {
                element.value = value;
            }

            // Trigger change event to update any dependent UI
            element.dispatchEvent(new Event('change', { bubbles: true }));
        });
    }

    /**
     * Get current step number
     */
    getCurrentStep() {
        const activeStep = document.querySelector('.form-step:not(.d-none)');
        return activeStep ? parseInt(activeStep.dataset.step) || 1 : 1;
    }

    /**
     * Setup auto-save functionality
     */
    setupAutoSave() {
        const form = document.getElementById('registrationForm');
        if (!form) return;

        // Clear existing interval
        if (this.autoSaveInterval) {
            clearTimeout(this.autoSaveInterval);
        }

        // Add event listeners for form changes
        form.addEventListener('input', () => this.scheduleAutoSave());
        form.addEventListener('change', () => this.scheduleAutoSave());
    }

    /**
     * Schedule auto-save with debouncing
     */
    scheduleAutoSave() {
        if (this.autoSaveInterval) {
            clearTimeout(this.autoSaveInterval);
        }

        this.autoSaveInterval = setTimeout(() => {
            this.saveFormData();
        }, this.autoSaveDelay);
    }

    /**
     * Clear saved data for current plan
     */
    clearSavedData() {
        if (!this.currentPlanId) return;

        try {
            localStorage.removeItem(this.getStorageKey());
            console.log('🗑️ Cleared saved form data for plan:', this.currentPlanId);
        } catch (error) {
            console.error('❌ Error clearing saved data:', error);
        }
    }

    /**
     * Clear all saved modal data (cleanup function)
     */
    clearAllSavedData() {
        try {
            const keys = Object.keys(localStorage);
            keys.forEach(key => {
                if (key.startsWith(this.storageKey)) {
                    localStorage.removeItem(key);
                }
            });
            console.log('🗑️ Cleared all saved modal data');
        } catch (error) {
            console.error('❌ Error clearing all saved data:', error);
        }
    }

    /**
     * Get saved data summary for debugging
     */
    getSavedDataSummary() {
        const keys = Object.keys(localStorage);
        const modalKeys = keys.filter(key => key.startsWith(this.storageKey));
        
        return {
            totalSavedForms: modalKeys.length,
            currentPlanData: this.currentPlanId ? localStorage.getItem(this.getStorageKey()) : null,
            allKeys: modalKeys
        };
    }

    /**
     * Validate that we have minimum required data before allowing submission
     */
    hasMinimumRequiredData() {
        const formData = this.extractFormData();
        
        // Check for basic required fields
        const requiredFields = ['fullName_1', 'email_1', 'phoneNumber_1', 'age_1'];
        return requiredFields.every(field => formData[field] && formData[field].trim() !== '');
    }

    /**
     * Mark form as submitted and clear cache
     */
    markAsSubmitted() {
        this.clearSavedData();
        if (this.autoSaveInterval) {
            clearTimeout(this.autoSaveInterval);
        }
        console.log('✅ Form submitted - cleared cache for plan:', this.currentPlanId);
    }
}

// Global instance
window.modalPersistence = new ModalPersistence();

// Auto-cleanup old data on page load
document.addEventListener('DOMContentLoaded', function() {
    // Clean up data older than 7 days
    try {
        const keys = Object.keys(localStorage);
        const modalKeys = keys.filter(key => key.startsWith('treasureHunt_modalData'));
        
        modalKeys.forEach(key => {
            try {
                const data = JSON.parse(localStorage.getItem(key));
                const maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days
                if (Date.now() - data.timestamp > maxAge) {
                    localStorage.removeItem(key);
                    console.log('🗑️ Cleaned up old modal data:', key);
                }
            } catch (e) {
                // Invalid data, remove it
                localStorage.removeItem(key);
            }
        });
    } catch (error) {
        console.error('❌ Error during modal data cleanup:', error);
    }
});
