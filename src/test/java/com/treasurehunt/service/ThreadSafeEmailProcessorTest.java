package com.treasurehunt.service;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.repository.EmailQueueRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("dev")
class ThreadSafeEmailProcessorTest {

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private ThreadSafeEmailProcessor processor;

    @Autowired
    private EmailQueueRepository repo;

    @Test
    void processEmailQueue_sendsQueuedEmail() {
        EmailQueue email = emailQueueService.queueEmail(
                "test2@example.com", "User2", "Subj2", "<i>Body</i>", EmailQueue.EmailType.ADMIN_NOTIFICATION);
        // Ensure it is ready to send
        email.setScheduledDate(LocalDateTime.now().minusMinutes(1));
        repo.save(email);

        // Run processing
        processor.processEmailsSync();

        EmailQueue reloaded = repo.findById(email.getId()).orElseThrow();
        Assertions.assertEquals(EmailQueue.EmailStatus.SENT, reloaded.getStatus());
        Assertions.assertNotNull(reloaded.getSentDate());
    }
}

