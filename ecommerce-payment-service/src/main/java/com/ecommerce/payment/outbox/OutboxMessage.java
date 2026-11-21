package com.ecommerce.payment.outbox;

public record OutboxMessage(
        String eventId,
        String eventType,
        String topic,
        String payload
) {
}
