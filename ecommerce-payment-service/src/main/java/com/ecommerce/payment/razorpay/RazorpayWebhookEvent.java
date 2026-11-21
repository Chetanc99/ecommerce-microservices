package com.ecommerce.payment.razorpay;

public record RazorpayWebhookEvent(
        String event,
        String razorpayOrderId,
        String razorpayPaymentId,
        String failureReason
) {
}
