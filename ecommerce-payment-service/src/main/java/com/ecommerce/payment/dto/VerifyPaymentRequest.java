package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPaymentRequest(
        @NotBlank(message = "Razorpay order ID is required")
        @Size(max = 100, message = "Razorpay order ID is too long")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment ID is required")
        @Size(max = 100, message = "Razorpay payment ID is too long")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay signature is required")
        @Size(max = 255, message = "Razorpay signature is too long")
        String razorpaySignature
) {
}
