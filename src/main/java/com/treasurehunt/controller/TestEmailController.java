package com.treasurehunt.controller;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.service.EmailQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test controller for sending test emails to verify Gmail SMTP configuration
 */
@RestController
@RequestMapping("/api/test")
public class TestEmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestEmailController.class);
    
    @Autowired
    private EmailQueueService emailQueueService;
    
    /**
     * Send a test email to verify Gmail SMTP configuration
     */
    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail() {
        try {
            logger.info("Creating test email to verify Gmail SMTP configuration");
            
            // Create a test email
            String recipientEmail = "tresurhunting@gmail.com";
            String recipientName = "Test Recipient";
            String subject = "🎉 Test Email - Gmail SMTP Configuration Verification";
            String body = createTestEmailBody();
            
            // Queue the test email
            EmailQueue testEmail = emailQueueService.queueEmail(
                recipientEmail, 
                recipientName, 
                subject, 
                body, 
                EmailQueue.EmailType.ADMIN_NOTIFICATION
            );
            
            logger.info("Test email queued successfully with ID: {}", testEmail.getId());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test email queued successfully",
                "emailId", testEmail.getId().toString(),
                "recipient", recipientEmail,
                "note", "Email will be processed by the queue within 1 minute"
            ));
            
        } catch (Exception e) {
            logger.error("Error creating test email", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error creating test email: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Create test email body with HTML content
     */
    private String createTestEmailBody() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Gmail SMTP Test</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                        <h1 style="margin: 0; font-size: 28px;">🎉 Gmail SMTP Test Successful!</h1>
                        <p style="margin: 10px 0 0 0; font-size: 18px;">Real Email Sending is Working!</p>
                    </div>
                    <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #667eea; margin-top: 0;">Congratulations!</h2>
                        <p>This email confirms that your Gmail SMTP configuration is working correctly.</p>
                        
                        <div style="background: white; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <h3 style="margin-top: 0; color: #333;">Configuration Details:</h3>
                            <ul style="list-style-type: none; padding: 0;">
                                <li><strong>✅ Gmail Address:</strong> tresurhunting@gmail.com</li>
                                <li><strong>✅ SMTP Host:</strong> smtp.gmail.com:587</li>
                                <li><strong>✅ Authentication:</strong> App Password</li>
                                <li><strong>✅ Encryption:</strong> TLS/STARTTLS</li>
                                <li><strong>✅ Mock Service:</strong> Disabled</li>
                            </ul>
                        </div>
                        
                        <div style="background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <h4 style="margin-top: 0;">✅ Email System Status: OPERATIONAL</h4>
                            <p style="margin-bottom: 0;">Your treasure hunt registration system can now send real emails to participants!</p>
                        </div>
                        
                        <h3 style="color: #333;">What This Means:</h3>
                        <ul>
                            <li>Registration confirmation emails will be sent to participants</li>
                            <li>Admin notification emails will be sent for new registrations</li>
                            <li>Team member emails will be sent for team registrations</li>
                            <li>Email campaigns can be sent to registered users</li>
                        </ul>
                        
                        <div style="text-align: center; margin-top: 30px;">
                            <p style="color: #6c757d;">
                                <strong>Treasure Hunt Registration System</strong><br>
                                Email System Successfully Configured
                            </p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
