-- Development Sample Data for Treasure Hunt Application

-- Insert App Settings
INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('company_name', 'Treasure Hunt Adventures', 'Company name displayed in emails and UI'),
('support_email', 'support@treasurehunt-dev.com', 'Support email address'),
('max_file_size', '5242880', 'Maximum file upload size in bytes (5MB)'),
('min_participant_age', '18', 'Minimum age for participants'),
('max_team_size', '6', 'Maximum team size allowed'),
('registration_open', 'true', 'Whether registration is currently open');

-- Insert Treasure Hunt Plans
INSERT INTO treasure_hunt_plans (name, description, duration_hours, max_participants, available_slots, price_inr, difficulty_level, status, is_featured, team_size, batches_completed, discount_enabled, prize_money, rating, created_date) VALUES
('City Explorer Adventure', 'Explore the historic city center while solving exciting puzzles and riddles. Perfect for beginners and families.', 3, 6, 50, 1500.00, 'BEGINNER', 'ACTIVE', true, 4, 0, false, 5000.00, 4.5, CURRENT_TIMESTAMP),
('Mystery Manor Challenge', 'Uncover the secrets of the old manor house in this intermediate-level treasure hunt with mysterious clues.', 4, 5, 30, 2000.00, 'INTERMEDIATE', 'ACTIVE', false, 5, 0, false, 7500.00, 4.2, CURRENT_TIMESTAMP),
('Extreme Urban Quest', 'The ultimate challenge for experienced treasure hunters. Navigate through complex puzzles across the entire city.', 6, 4, 20, 3000.00, 'ADVANCED', 'ACTIVE', false, 4, 0, false, 10000.00, 4.8, CURRENT_TIMESTAMP),
('Family Fun Hunt', 'A delightful treasure hunt designed specifically for families with children. Easy clues and exciting rewards.', 2, 8, 40, 1000.00, 'BEGINNER', 'ACTIVE', false, 6, 0, false, 2500.00, 4.0, CURRENT_TIMESTAMP),
('Corporate Team Building', 'Professional team-building treasure hunt perfect for corporate groups and office outings.', 4, 10, 25, 2500.00, 'INTERMEDIATE', 'ACTIVE', false, 8, 0, false, 8000.00, 4.3, CURRENT_TIMESTAMP);

-- Insert Sample User Registrations
INSERT INTO user_registrations (application_id, full_name, email, phone_number, age, team_name, plan_id, status, registration_date, gender, emergency_contact_name, emergency_contact_phone, medical_consent_given) VALUES
('TH2024-001-001', 'John Doe', 'john.doe@example.com', '9876543210', 28, null, 1, 'CONFIRMED', CURRENT_TIMESTAMP, 'MALE', 'Jane Doe', '9876543211', true),
('TH2024-001-002', 'Jane Smith', 'jane.smith@example.com', '9876543211', 25, 'Adventure Seekers', 2, 'CONFIRMED', CURRENT_TIMESTAMP, 'FEMALE', 'John Smith', '9876543212', true),
('TH2024-001-003', 'Mike Johnson', 'mike.johnson@example.com', '9876543212', 32, null, 1, 'PENDING', CURRENT_TIMESTAMP, 'MALE', 'Lisa Johnson', '9876543213', true),
('TH2024-001-004', 'Sarah Wilson', 'sarah.wilson@example.com', '9876543213', 29, 'Mystery Solvers', 3, 'CONFIRMED', CURRENT_TIMESTAMP, 'FEMALE', 'Tom Wilson', '9876543214', true),
('TH2024-001-005', 'David Brown', 'david.brown@example.com', '9876543214', 35, null, 4, 'CONFIRMED', CURRENT_TIMESTAMP, 'MALE', 'Mary Brown', '9876543215', true);

-- Insert Team Members for Team Registrations
INSERT INTO team_members (full_name, email, phone_number, age, member_position, registration_id, gender, emergency_contact_name, emergency_contact_phone, medical_consent_given) VALUES
-- Team members for Jane Smith's team (registration_id = 2)
('Jane Smith', 'jane.smith@example.com', '9876543211', 25, 1, 2, 'FEMALE', 'John Smith', '9876543212', true),
('Bob Anderson', 'bob.anderson@example.com', '9876543215', 27, 2, 2, 'MALE', 'Mary Anderson', '9876543220', true),
('Alice Cooper', 'alice.cooper@example.com', '9876543216', 24, 3, 2, 'FEMALE', 'Tom Cooper', '9876543221', true),
('Tom Wilson', 'tom.wilson@example.com', '9876543217', 30, 4, 2, 'MALE', 'Sarah Wilson', '9876543222', true),

-- Team members for Sarah Wilson's team (registration_id = 4)
('Sarah Wilson', 'sarah.wilson@example.com', '9876543213', 29, 1, 4, 'FEMALE', 'Tom Wilson', '9876543214', true),
('Chris Davis', 'chris.davis@example.com', '9876543218', 31, 2, 4, 'MALE', 'Lisa Davis', '9876543223', true),
('Emma Taylor', 'emma.taylor@example.com', '9876543219', 26, 3, 4, 'FEMALE', 'Mark Taylor', '9876543224', true);

-- Insert Sample Email Queue Entries
INSERT INTO email_queue (recipient_email, recipient_name, subject, body, status, email_type, registration_id, created_date, retry_count, max_retry_attempts, priority) VALUES
('john.doe@example.com', 'John Doe', 'Registration Confirmed - City Explorer Adventure', 'Dear John, your registration has been confirmed...', 'SENT', 'REGISTRATION_CONFIRMATION', 1, CURRENT_TIMESTAMP, 0, 3, 1),
('jane.smith@example.com', 'Jane Smith', 'Team Registration Confirmed - Mystery Manor Challenge', 'Dear Jane, your team registration has been confirmed...', 'SENT', 'REGISTRATION_CONFIRMATION', 2, CURRENT_TIMESTAMP, 0, 3, 1),
('mike.johnson@example.com', 'Mike Johnson', 'Registration Received - City Explorer Adventure', 'Dear Mike, we have received your registration...', 'PENDING', 'ADMIN_NOTIFICATION', 3, CURRENT_TIMESTAMP, 0, 3, 2);

-- Insert Sample Uploaded Documents
INSERT INTO uploaded_documents (original_filename, stored_filename, file_path, file_size_bytes, content_type, document_type, registration_id, upload_date) VALUES
('john_id.pdf', 'doc_001_john_id.pdf', 'uploads/documents/doc_001_john_id.pdf', 1024000, 'application/pdf', 'ID_DOCUMENT', 1, CURRENT_TIMESTAMP),
('jane_photo.jpg', 'img_002_jane_photo.jpg', 'uploads/documents/img_002_jane_photo.jpg', 512000, 'image/jpeg', 'PHOTO', 2, CURRENT_TIMESTAMP),
('sarah_id.pdf', 'doc_004_sarah_id.pdf', 'uploads/documents/doc_004_sarah_id.pdf', 1536000, 'application/pdf', 'ID_DOCUMENT', 4, CURRENT_TIMESTAMP);

-- Insert Sample Uploaded Images
INSERT INTO uploaded_images (original_filename, stored_filename, file_path, file_size, content_type, alt_text, is_active, upload_date, image_category, uploaded_by) VALUES
('plan1_banner.jpg', 'plan_1_banner.jpg', 'uploads/images/plan_1_banner.jpg', 2048000, 'image/jpeg', 'City Explorer Adventure Banner', true, CURRENT_TIMESTAMP, 'PLAN_BANNER', 'admin'),
('plan2_banner.jpg', 'plan_2_banner.jpg', 'uploads/images/plan_2_banner.jpg', 1843200, 'image/jpeg', 'Mystery Manor Challenge Banner', true, CURRENT_TIMESTAMP, 'PLAN_BANNER', 'admin'),
('john_profile.jpg', 'user_1_profile.jpg', 'uploads/images/user_1_profile.jpg', 768000, 'image/jpeg', 'John Doe Profile Picture', true, CURRENT_TIMESTAMP, 'PROFILE', 'admin');

-- Update sequence values for H2 (if using identity columns)
-- These will be automatically handled by H2's AUTO_INCREMENT
