package com.ecommerce.order.outbox;

public record OutboxMessage(
        String eventId,
        String eventType,
        String topic,
        String payload
) {
}
