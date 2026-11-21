package com.ecommerce.common.events;

import java.math.BigDecimal;

public record OrderConfirmedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        String occurredAt
) {
}
