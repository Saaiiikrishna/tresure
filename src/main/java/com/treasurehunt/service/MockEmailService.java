package com.treasurehunt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock email service for development and testing
 * Simulates email sending without actually sending emails
 */
@Service
@ConditionalOnProperty(name = "app.email.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockEmailService implements JavaMailSender {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    private final AtomicLong emailCounter = new AtomicLong(0);
    
    @Override
    public void send(SimpleMailMessage simpleMessage) throws org.springframework.mail.MailException {
        long emailId = emailCounter.incrementAndGet();
        
        logger.info("📧 MOCK EMAIL SENT #{}", emailId);
        logger.info("   To: {}", simpleMessage.getTo() != null ? String.join(", ", simpleMessage.getTo()) : "N/A");
        logger.info("   From: {}", simpleMessage.getFrom());
        logger.info("   Subject: {}", simpleMessage.getSubject());
        logger.info("   Text: {}", simpleMessage.getText() != null ? 
            (simpleMessage.getText().length() > 100 ? 
                simpleMessage.getText().substring(0, 100) + "..." : 
                simpleMessage.getText()) : "N/A");
        logger.info("   ✅ Email #{} delivered successfully (MOCK)", emailId);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws org.springframework.mail.MailException {
        for (SimpleMailMessage message : simpleMessages) {
            send(message);
        }
    }

    @Override
    public MimeMessage createMimeMessage() {
        return new MockMimeMessage();
    }

    @Override
    public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws org.springframework.mail.MailException {
        return new MockMimeMessage();
    }

    @Override
    public void send(MimeMessage mimeMessage) throws org.springframework.mail.MailException {
        long emailId = emailCounter.incrementAndGet();
        
        try {
            logger.info("📧 MOCK MIME EMAIL SENT #{}", emailId);
            logger.info("   To: {}", String.join(", ", mimeMessage.getHeader("To")));
            logger.info("   From: {}", String.join(", ", mimeMessage.getHeader("From")));
            logger.info("   Subject: {}", mimeMessage.getSubject());
            logger.info("   Content-Type: {}", mimeMessage.getContentType());
            
            // Log content preview for HTML emails
            Object content = mimeMessage.getContent();
            if (content instanceof String) {
                String contentStr = (String) content;
                String preview = contentStr.length() > 200 ? 
                    contentStr.substring(0, 200) + "..." : contentStr;
                logger.info("   Content Preview: {}", preview);
            }
            
            logger.info("   ✅ MIME Email #{} delivered successfully (MOCK)", emailId);
            
        } catch (Exception e) {
            logger.info("   ✅ MIME Email #{} delivered successfully (MOCK) - Content parsing skipped", emailId);
        }
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws org.springframework.mail.MailException {
        for (MimeMessage message : mimeMessages) {
            send(message);
        }
    }

    /**
     * Mock MimeMessage implementation
     */
    private static class MockMimeMessage extends MimeMessage {
        private String subject;
        private String[] toHeaders = new String[0];
        private String[] fromHeaders = new String[0];
        private String contentType = "text/plain";
        private Object content = "";

        public MockMimeMessage() {
            super((jakarta.mail.Session) null);
        }

        @Override
        public void setSubject(String subject) throws MessagingException {
            this.subject = subject;
        }

        @Override
        public String getSubject() throws MessagingException {
            return subject;
        }

        @Override
        public void setHeader(String name, String value) throws MessagingException {
            if ("To".equalsIgnoreCase(name)) {
                toHeaders = new String[]{value};
            } else if ("From".equalsIgnoreCase(name)) {
                fromHeaders = new String[]{value};
            }
        }

        @Override
        public String[] getHeader(String name) throws MessagingException {
            if ("To".equalsIgnoreCase(name)) {
                return toHeaders;
            } else if ("From".equalsIgnoreCase(name)) {
                return fromHeaders;
            }
            return new String[0];
        }

        @Override
        public void setContent(Object content, String type) throws MessagingException {
            this.content = content;
            this.contentType = type;
        }

        @Override
        public Object getContent() throws java.io.IOException, MessagingException {
            return content;
        }

        @Override
        public String getContentType() throws MessagingException {
            return contentType;
        }

        @Override
        public void setText(String text) throws MessagingException {
            this.content = text;
            this.contentType = "text/plain";
        }

        @Override
        public void saveChanges() throws MessagingException {
            // Mock implementation - do nothing
        }
    }
}
