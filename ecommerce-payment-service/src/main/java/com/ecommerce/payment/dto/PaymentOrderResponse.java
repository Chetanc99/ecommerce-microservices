package com.ecommerce.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        Long paymentId,
        Long orderId,
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        String currency
) {
}
