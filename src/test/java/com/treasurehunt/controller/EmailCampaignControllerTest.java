package com.treasurehunt.controller;

import com.treasurehunt.entity.EmailQueue;
import com.treasurehunt.repository.EmailQueueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EmailCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailQueueRepository repo;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void processNowEndpoint_works() throws Exception {
        mockMvc.perform(post("/admin/email-campaigns/api/process-now")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void retryAndCancelEndpoints_work() throws Exception {
        // create a FAILED email to retry
        EmailQueue e = new EmailQueue();
        e.setRecipientEmail("u@example.com");
        e.setRecipientName("U");
        e.setSubject("S");
        e.setBody("B");
        e.setEmailType(EmailQueue.EmailType.ADMIN_NOTIFICATION);
        e.setStatus(EmailQueue.EmailStatus.FAILED);
        repo.save(e);

        mockMvc.perform(post("/admin/email-campaigns/api/email/" + e.getId() + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());

        // create a PENDING email to cancel
        EmailQueue p = new EmailQueue();
        p.setRecipientEmail("p@example.com");
        p.setRecipientName("P");
        p.setSubject("SP");
        p.setBody("BP");
        p.setEmailType(EmailQueue.EmailType.ADMIN_NOTIFICATION);
        p.setStatus(EmailQueue.EmailStatus.PENDING);
        repo.save(p);

        mockMvc.perform(post("/admin/email-campaigns/api/email/" + p.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());
    }
}

