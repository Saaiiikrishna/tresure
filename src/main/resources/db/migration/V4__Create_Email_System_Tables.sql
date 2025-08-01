-- Create email_queue table
CREATE TABLE email_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 5,
    max_retry_attempts INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_date TIMESTAMP NULL,
    sent_date TIMESTAMP NULL,
    error_message TEXT NULL,
    registration_id BIGINT NULL,
    campaign_name VARCHAR(255) NULL,
    campaign_id VARCHAR(100) NULL,
    template_name VARCHAR(100) NULL,
    template_variables TEXT NULL,
    
    INDEX idx_email_queue_status (status),
    INDEX idx_email_queue_scheduled (scheduled_date),
    INDEX idx_email_queue_priority (priority),
    INDEX idx_email_queue_campaign (campaign_id),
    INDEX idx_email_queue_registration (registration_id),
    INDEX idx_email_queue_created (created_date),
    INDEX idx_email_queue_recipient (recipient_email),
    
    FOREIGN KEY (registration_id) REFERENCES user_registrations(id) ON DELETE SET NULL
);

-- Create email_campaigns table
CREATE TABLE email_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    campaign_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    target_audience VARCHAR(100) NULL,
    scheduled_date TIMESTAMP NULL,
    sent_date TIMESTAMP NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    total_recipients INT NOT NULL DEFAULT 0,
    emails_sent INT NOT NULL DEFAULT 0,
    emails_failed INT NOT NULL DEFAULT 0,
    emails_pending INT NOT NULL DEFAULT 0,
    template_name VARCHAR(100) NULL,
    template_variables TEXT NULL,
    priority INT NOT NULL DEFAULT 5,
    max_retry_attempts INT NOT NULL DEFAULT 3,
    
    INDEX idx_email_campaigns_status (status),
    INDEX idx_email_campaigns_type (campaign_type),
    INDEX idx_email_campaigns_created (created_date),
    INDEX idx_email_campaigns_scheduled (scheduled_date),
    INDEX idx_email_campaigns_creator (created_by),
    INDEX idx_email_campaigns_audience (target_audience)
);

-- Insert sample email templates data (optional)
INSERT INTO email_campaigns (
    name, 
    description, 
    subject, 
    body, 
    campaign_type, 
    status, 
    target_audience, 
    created_by
) VALUES 
(
    'Welcome Campaign',
    'Welcome email for new registrations',
    'Welcome to Treasure Hunt Adventures! 🎉',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Welcome</title></head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
            <h1 style="margin: 0; font-size: 28px;">🎉 Welcome to the Adventure!</h1>
            <p style="margin: 10px 0 0 0; font-size: 18px;">Thank you for joining us!</p>
        </div>
        <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
            <h2 style="color: #667eea; margin-top: 0;">Hello {{fullName}}!</h2>
            <p>We are excited to have you join our treasure hunt adventure. Get ready for an amazing experience!</p>
            <div style="background: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ffc107;">
                <h3 style="margin-top: 0; color: #856404;">📅 What to Expect:</h3>
                <ul style="margin: 0; padding-left: 20px;">
                    <li>Exciting challenges and puzzles</li>
                    <li>Team building activities</li>
                    <li>Prizes and rewards</li>
                    <li>Unforgettable memories</li>
                </ul>
            </div>
            <div style="text-align: center; margin-top: 30px;">
                <p style="color: #6c757d;">Questions? Contact us at <a href="mailto:support@treasurehunt.com" style="color: #667eea;">support@treasurehunt.com</a></p>
            </div>
        </div>
    </div>
</body>
</html>',
    'PROMOTIONAL',
    'DRAFT',
    'ALL',
    'admin'
),
(
    'Event Reminder',
    'Reminder email for upcoming events',
    'Don''t Forget: Your Treasure Hunt Adventure is Tomorrow! ⏰',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Event Reminder</title></head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: #ffc107; color: #212529; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
            <h1 style="margin: 0; font-size: 28px;">⏰ Reminder: Adventure Tomorrow!</h1>
            <p style="margin: 10px 0 0 0; font-size: 18px;">Don''t miss out on the fun!</p>
        </div>
        <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
            <h2 style="color: #ffc107; margin-top: 0;">Hello {{fullName}}!</h2>
            <p>This is a friendly reminder that your treasure hunt adventure is scheduled for tomorrow. We can''t wait to see you there!</p>
            <div style="background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;">
                <h3 style="margin-top: 0; color: #333;">📋 Important Reminders:</h3>
                <ul style="margin: 0; padding-left: 20px;">
                    <li>Arrive 15 minutes before start time</li>
                    <li>Bring a valid ID</li>
                    <li>Wear comfortable walking shoes</li>
                    <li>Bring your mobile phone (fully charged)</li>
                    <li>Bring water and snacks</li>
                </ul>
            </div>
            <div style="text-align: center; margin-top: 30px;">
                <p style="color: #6c757d;">See you tomorrow! Contact us at <a href="mailto:support@treasurehunt.com" style="color: #ffc107;">support@treasurehunt.com</a></p>
            </div>
        </div>
    </div>
</body>
</html>',
    'REMINDER',
    'DRAFT',
    'ALL',
    'admin'
);

-- Add some sample email queue entries for testing
INSERT INTO email_queue (
    recipient_email,
    recipient_name,
    subject,
    body,
    email_type,
    status,
    priority,
    scheduled_date
) VALUES 
(
    'test@example.com',
    'Test User',
    'Test Email - System Check',
    '<!DOCTYPE html><html><body><h1>System Test Email</h1><p>This is a test email to verify the email queue system is working correctly.</p></body></html>',
    'ADMIN_NOTIFICATION',
    'PENDING',
    1,
    NOW()
),
(
    'admin@treasurehunt.com',
    'Admin User',
    'Email System Initialized',
    '<!DOCTYPE html><html><body><h1>Email System Ready</h1><p>The email queue system has been successfully initialized and is ready to process emails.</p></body></html>',
    'ADMIN_NOTIFICATION',
    'PENDING',
    1,
    NOW()
);

-- Create indexes for better performance
CREATE INDEX idx_email_queue_ready_to_send ON email_queue (status, scheduled_date, priority);
CREATE INDEX idx_email_campaigns_active ON email_campaigns (status, scheduled_date);

-- Add constraints
ALTER TABLE email_queue 
ADD CONSTRAINT chk_email_queue_status 
CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED', 'SCHEDULED'));

ALTER TABLE email_queue 
ADD CONSTRAINT chk_email_queue_type 
CHECK (email_type IN ('REGISTRATION_CONFIRMATION', 'ADMIN_NOTIFICATION', 'CAMPAIGN_EMAIL', 'REMINDER_EMAIL', 'CANCELLATION_EMAIL', 'WELCOME_EMAIL', 'EVENT_UPDATE'));

ALTER TABLE email_campaigns 
ADD CONSTRAINT chk_email_campaigns_status 
CHECK (status IN ('DRAFT', 'SCHEDULED', 'SENDING', 'SENT', 'PAUSED', 'CANCELLED', 'FAILED'));

ALTER TABLE email_campaigns 
ADD CONSTRAINT chk_email_campaigns_type 
CHECK (campaign_type IN ('PROMOTIONAL', 'INFORMATIONAL', 'REMINDER', 'ANNOUNCEMENT', 'NEWSLETTER', 'EVENT_UPDATE', 'REGISTRATION_FOLLOWUP'));

-- Add comments for documentation
ALTER TABLE email_queue COMMENT = 'Queue for managing email sending with retry logic and scheduling';
ALTER TABLE email_campaigns COMMENT = 'Email campaigns for bulk messaging to users';

-- Create view for email statistics
CREATE VIEW email_statistics AS
SELECT 
    'TOTAL' as metric,
    COUNT(*) as count,
    NULL as status,
    NULL as type
FROM email_queue
UNION ALL
SELECT 
    'BY_STATUS' as metric,
    COUNT(*) as count,
    status,
    NULL as type
FROM email_queue
GROUP BY status
UNION ALL
SELECT 
    'BY_TYPE' as metric,
    COUNT(*) as count,
    NULL as status,
    email_type as type
FROM email_queue
GROUP BY email_type;

-- Create view for campaign statistics
CREATE VIEW campaign_statistics AS
SELECT 
    'TOTAL' as metric,
    COUNT(*) as count,
    NULL as status,
    NULL as type
FROM email_campaigns
UNION ALL
SELECT 
    'BY_STATUS' as metric,
    COUNT(*) as count,
    status,
    NULL as type
FROM email_campaigns
GROUP BY status
UNION ALL
SELECT 
    'BY_TYPE' as metric,
    COUNT(*) as count,
    NULL as status,
    campaign_type as type
FROM email_campaigns
GROUP BY campaign_type;
