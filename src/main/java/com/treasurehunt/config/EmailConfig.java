package com.treasurehunt.config;

import com.treasurehunt.service.MockEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Email configuration for development and production environments
 */
@Configuration
public class EmailConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmailConfig.class);

    /**
     * Mock email service for development/testing only
     * This bean will be used when app.email.mock.enabled=true
     * CURRENTLY DISABLED - Real SMTP is enabled
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.email.mock.enabled", havingValue = "true")
    public JavaMailSender mockJavaMailSender() {
        logger.warn("🚨 MOCK EMAIL SERVICE ENABLED - Emails will be simulated, not actually sent!");
        return new MockEmailService();
    }
}
