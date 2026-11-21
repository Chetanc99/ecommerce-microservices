package com.ecommerce.common.events;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        String eventId,
        Long paymentId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal amount,
        String reason,
        String occurredAt
) {
}
