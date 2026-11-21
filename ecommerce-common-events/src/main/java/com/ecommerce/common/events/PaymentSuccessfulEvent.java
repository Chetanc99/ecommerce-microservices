package com.ecommerce.common.events;

import java.math.BigDecimal;

public record PaymentSuccessfulEvent(
        String eventId,
        Long paymentId,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal amount,
        String razorpayPaymentId,
        String occurredAt
) {
}
