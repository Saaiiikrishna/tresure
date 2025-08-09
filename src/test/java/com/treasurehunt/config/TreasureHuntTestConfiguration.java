package com.treasurehunt.config;

import com.treasurehunt.service.MockEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Test configuration to ensure proper bean setup for tests
 */
@TestConfiguration
@Profile("test")
public class TreasureHuntTestConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TreasureHuntTestConfiguration.class);

    /**
     * Mock email service for tests
     */
    @Bean
    @Primary
    public JavaMailSender testJavaMailSender() {
        logger.info("🧪 Creating mock email service for tests");
        return new MockEmailService();
    }
}
