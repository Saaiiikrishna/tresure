package com.treasurehunt.service;

import com.treasurehunt.config.TreasureHuntTestConfiguration;
import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.repository.EmailQueueRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TreasureHuntTestConfiguration.class)
class EmailQueueServiceTest {

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Test
    void queueEmail_persistsEntity() {
        String to = "test@example.com";
        String name = "Test User";
        String subject = "Test Subject";
        String body = "<b>Hi</b>";

        EmailQueue saved = emailQueueService.queueEmail(
                to, name, subject, body, EmailQueue.EmailType.ADMIN_NOTIFICATION);

        Assertions.assertNotNull(saved.getId());
        EmailQueue reloaded = emailQueueRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertEquals(EmailQueue.EmailStatus.PENDING, reloaded.getStatus());
        Assertions.assertEquals(to, reloaded.getRecipientEmail());
        Assertions.assertEquals(subject, reloaded.getSubject());
    }
}

