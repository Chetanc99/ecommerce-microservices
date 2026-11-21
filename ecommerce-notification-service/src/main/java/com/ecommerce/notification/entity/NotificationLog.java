package com.ecommerce.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public NotificationLog() {
    }

    public NotificationLog(
            String eventId,
            String notificationType,
            String recipient,
            NotificationStatus status,
            String errorMessage) {

        this.eventId = eventId;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getEventId() {
        return eventId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getRecipient() {
        return recipient;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
