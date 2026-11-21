package com.ecommerce.common.events;

import java.math.BigDecimal;

public record PaymentInitiatedEvent(
        String eventId,
        Long paymentId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal amount,
        String razorpayOrderId,
        String occurredAt
) {
}
