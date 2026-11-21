package com.ecommerce.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@ecommerce.local}") String from) {

        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean send(String recipient, String subject, String body) {

        if (!enabled) {
            log.info("Mail disabled. Would send '{}' to {}: {}", subject, recipient, body);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        return true;
    }
}
