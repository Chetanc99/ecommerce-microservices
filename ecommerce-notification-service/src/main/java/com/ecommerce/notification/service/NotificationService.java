package com.ecommerce.notification.service;

import com.ecommerce.notification.email.EmailSender;
import com.ecommerce.notification.email.NotificationMessage;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.repository.NotificationLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository logRepository;
    private final EmailSender emailSender;

    public NotificationService(
            NotificationLogRepository logRepository,
            EmailSender emailSender) {

        this.logRepository = logRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void deliver(NotificationMessage message) {

        if (logRepository.existsById(message.eventId())) {
            return;
        }

        if (message.recipient() == null || message.recipient().isBlank()) {
            log.warn("Skipping {} notification for event {} because no e-mail is available",
                    message.type(), message.eventId());
            save(message, NotificationStatus.SKIPPED, "No recipient e-mail");
            return;
        }

        try {
            boolean sent = emailSender.send(
                    message.recipient(), message.subject(), message.body());

            save(message, sent ? NotificationStatus.SENT : NotificationStatus.SKIPPED,
                    sent ? null : "Mail delivery disabled");

        } catch (RuntimeException exception) {
            log.error("Could not send {} notification for event {}",
                    message.type(), message.eventId(), exception);
            save(message, NotificationStatus.FAILED, truncate(exception.getMessage()));
        }
    }

    private void save(
            NotificationMessage message,
            NotificationStatus status,
            String errorMessage) {

        logRepository.save(new NotificationLog(
                message.eventId(),
                message.type(),
                message.recipient() == null ? "" : message.recipient(),
                status,
                errorMessage
        ));
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null) {
            return "Unknown mail error";
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }
}
