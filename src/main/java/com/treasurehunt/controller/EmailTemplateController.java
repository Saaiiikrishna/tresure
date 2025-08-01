package com.treasurehunt.controller;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.entity.UserRegistration;
import com.treasurehunt.service.EmailQueueService;
import com.treasurehunt.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for email template management in admin panel
 */
@Controller
@RequestMapping("/admin/email-templates")
public class EmailTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateController.class);

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    @Qualifier("stringTemplateEngine")
    private TemplateEngine stringTemplateEngine;

    private static final Map<String, String> TEMPLATE_DESCRIPTIONS = new HashMap<>();
    
    static {
        TEMPLATE_DESCRIPTIONS.put("registration-confirmation", "Registration Received Email - Sent immediately after user submits registration");
        TEMPLATE_DESCRIPTIONS.put("application-approval", "Application Approval Email - Sent when admin approves the application");
        TEMPLATE_DESCRIPTIONS.put("admin-notification", "Admin Notification Email - Sent to admin when new registration is received");
        TEMPLATE_DESCRIPTIONS.put("team-member-confirmation", "Team Member Confirmation Email - Sent to team members when registration is confirmed");
        TEMPLATE_DESCRIPTIONS.put("team-cancellation", "Team Cancellation Email - Sent when team registration is cancelled");
        TEMPLATE_DESCRIPTIONS.put("individual-cancellation", "Individual Cancellation Email - Sent when individual registration is cancelled");
    }

    /**
     * Display email template management dashboard
     */
    @GetMapping
    public String templateDashboard(Model model) {
        logger.info("Displaying email template management dashboard");
        
        try {
            // Get available templates
            model.addAttribute("templates", TEMPLATE_DESCRIPTIONS);
            
            // Get email statistics
            Map<String, Object> emailStats = emailQueueService.getEmailStatistics();
            model.addAttribute("emailStats", emailStats);
            
            return "admin/email-templates/dashboard";
            
        } catch (Exception e) {
            logger.error("Error loading email template dashboard", e);
            model.addAttribute("error", "Error loading templates: " + e.getMessage());
            return "admin/email-templates/dashboard";
        }
    }

    /**
     * Display template editor
     */
    @GetMapping("/edit/{templateName}")
    public String editTemplate(@PathVariable String templateName, Model model) {
        logger.info("Editing template: {}", templateName);
        
        try {
            // Validate template name
            if (!TEMPLATE_DESCRIPTIONS.containsKey(templateName)) {
                model.addAttribute("error", "Template not found: " + templateName);
                return "redirect:/admin/email-templates";
            }
            
            // Read template content
            String templateContent = readTemplateFile(templateName);
            
            model.addAttribute("templateName", templateName);
            model.addAttribute("templateDescription", TEMPLATE_DESCRIPTIONS.get(templateName));
            model.addAttribute("templateContent", templateContent);
            
            return "admin/email-templates/editor";
            
        } catch (Exception e) {
            logger.error("Error loading template for editing: {}", templateName, e);
            model.addAttribute("error", "Error loading template: " + e.getMessage());
            return "redirect:/admin/email-templates";
        }
    }

    /**
     * Save template changes
     */
    @PostMapping("/save/{templateName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveTemplate(
            @PathVariable String templateName,
            @RequestParam String content) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Saving template: {}", templateName);
            
            // Validate template name
            if (!TEMPLATE_DESCRIPTIONS.containsKey(templateName)) {
                response.put("success", false);
                response.put("message", "Invalid template name");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Save template content
            saveTemplateFile(templateName, content);
            
            response.put("success", true);
            response.put("message", "Template saved successfully");
            
            logger.info("Successfully saved template: {}", templateName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error saving template: {}", templateName, e);
            response.put("success", false);
            response.put("message", "Error saving template: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Preview template with sample data
     */
    @PostMapping("/preview/{templateName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewTemplate(
            @PathVariable String templateName,
            @RequestParam String content) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Previewing template: {}", templateName);

            // Create sample data for preview
            Context context = createSampleContext(templateName);

            // Process template content directly using StringTemplateResolver
            String htmlContent = stringTemplateEngine.process(content, context);

            response.put("success", true);
            response.put("htmlContent", htmlContent);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error previewing template: {}", templateName, e);
            response.put("success", false);
            response.put("message", "Error previewing template: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Send test email with current template
     */
    @PostMapping("/test/{templateName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @PathVariable String templateName,
            @RequestParam String testEmail,
            @RequestParam(required = false) String content) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Sending test email for template: {} to: {}", templateName, testEmail);
            
            // Use provided content or read from file
            String emailContent = content != null ? content : readTemplateFile(templateName);
            
            // Create sample context
            Context context = createSampleContext(templateName);
            
            // Save temporary template if content provided
            String actualTemplateName = templateName;
            if (content != null) {
                actualTemplateName = "temp-" + templateName;
                saveTemplateFile(actualTemplateName, content);
            }
            
            // Process template
            String htmlContent = templateEngine.process("email/" + actualTemplateName, context);
            
            // Queue test email
            EmailQueue queuedEmail = emailQueueService.queueEmail(
                testEmail,
                "Test Recipient",
                "TEST: " + TEMPLATE_DESCRIPTIONS.get(templateName),
                htmlContent,
                EmailQueue.EmailType.ADMIN_NOTIFICATION
            );
            
            // Clean up temporary template
            if (content != null) {
                deleteTemplateFile(actualTemplateName);
            }
            
            response.put("success", true);
            response.put("message", "Test email queued successfully");
            response.put("emailId", queuedEmail.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending test email for template: {}", templateName, e);
            response.put("success", false);
            response.put("message", "Error sending test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload attachment for approval emails
     */
    @PostMapping("/upload-attachment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Uploading email attachment: {}", file.getOriginalFilename());
            
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Save file (implement your file storage logic here)
            // For now, just return success
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading attachment", e);
            response.put("success", false);
            response.put("message", "Error uploading file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Read template file content
     */
    private String readTemplateFile(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + templateName + ".html");
        return new String(resource.getInputStream().readAllBytes());
    }

    /**
     * Save template file content
     */
    private void saveTemplateFile(String templateName, String content) throws IOException {
        Path templatePath = Paths.get("src/main/resources/templates/email/" + templateName + ".html");
        Files.write(templatePath, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Delete template file
     */
    private void deleteTemplateFile(String templateName) throws IOException {
        Path templatePath = Paths.get("src/main/resources/templates/email/" + templateName + ".html");
        Files.deleteIfExists(templatePath);
    }

    /**
     * Create sample context for template preview
     */
    private Context createSampleContext(String templateName) {
        Context context = new Context();
        
        // Add common variables
        context.setVariable("companyName", "Treasure Hunt Adventures");
        context.setVariable("supportEmail", "support@treasurehuntadventures.com");
        
        // Create sample registration data
        // This would typically come from your actual entities
        Map<String, Object> sampleRegistration = new HashMap<>();
        sampleRegistration.put("fullName", "John Doe");
        sampleRegistration.put("email", "john.doe@example.com");
        sampleRegistration.put("phoneNumber", "9876543210");
        sampleRegistration.put("age", 25);
        sampleRegistration.put("teamName", "Adventure Seekers");
        sampleRegistration.put("registrationDate", java.time.LocalDateTime.now());
        sampleRegistration.put("emergencyContactName", "Jane Doe");
        sampleRegistration.put("emergencyContactPhone", "9876543211");
        sampleRegistration.put("teamRegistration", true);
        
        Map<String, Object> samplePlan = new HashMap<>();
        samplePlan.put("name", "Urban Explorer Adventure");
        samplePlan.put("durationHours", 4);
        samplePlan.put("difficultyLevel", "INTERMEDIATE");
        samplePlan.put("priceInr", 2490);
        samplePlan.put("teamDescription", "Teams of 4 players");
        
        context.setVariable("registration", sampleRegistration);
        context.setVariable("plan", samplePlan);
        context.setVariable("registrationNumber", "TH-000123");
        
        return context;
    }
}
