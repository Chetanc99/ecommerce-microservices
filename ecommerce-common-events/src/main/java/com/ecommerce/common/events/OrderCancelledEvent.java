package com.ecommerce.common.events;

import java.math.BigDecimal;

public record OrderCancelledEvent(
        String eventId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        String reason,
        String occurredAt
) {
}
