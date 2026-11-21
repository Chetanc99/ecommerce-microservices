package com.ecommerce.payment.razorpay;

import java.math.BigDecimal;

public record RazorpayOrderResult(
        String orderId,
        BigDecimal amount,
        String currency
) {
}
