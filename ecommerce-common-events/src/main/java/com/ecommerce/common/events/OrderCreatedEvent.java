package com.ecommerce.common.events;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        List<OrderItemPayload> items,
        String occurredAt
) {
}
