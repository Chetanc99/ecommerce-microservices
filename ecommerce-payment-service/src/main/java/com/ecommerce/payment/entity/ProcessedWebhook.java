package com.ecommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_webhooks")
public class ProcessedWebhook {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedWebhook() {
    }

    public ProcessedWebhook(String eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
