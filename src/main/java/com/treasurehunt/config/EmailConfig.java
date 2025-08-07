package com.treasurehunt.config;

import com.treasurehunt.service.MockEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Email configuration for development and production environments
 */
@Configuration
public class EmailConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmailConfig.class);

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /**
     * Production JavaMailSender - only created if Jakarta Mail classes are available
     */
    @Bean
    @Primary
    @ConditionalOnClass(name = "jakarta.mail.internet.MimeMessage")
    @ConditionalOnProperty(name = "spring.mail.host")
    public JavaMailSender productionJavaMailSender() {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

            mailSender.setHost(mailHost);
            mailSender.setPort(mailPort);
            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "false");

            // Add connection timeouts
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");

            logger.info("✅ Production JavaMailSender configured for host: {}", mailHost);
            return mailSender;

        } catch (Exception e) {
            logger.error("❌ Failed to configure production JavaMailSender: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Mock email service for development/testing only
     * This bean will be used when app.email.mock.enabled=true
     */
    @Bean
    @ConditionalOnProperty(name = "app.email.mock.enabled", havingValue = "true")
    public JavaMailSender mockJavaMailSender() {
        logger.warn("🚨 MOCK EMAIL SERVICE ENABLED - Emails will be simulated, not actually sent!");
        return new MockEmailService();
    }

    /**
     * Fallback email service when Jakarta Mail is not available
     */
    @Bean
    @ConditionalOnMissingClass("jakarta.mail.internet.MimeMessage")
    public JavaMailSender fallbackJavaMailSender() {
        logger.warn("⚠️ Jakarta Mail not available - using fallback email service");
        return new MockEmailService();
    }
}
