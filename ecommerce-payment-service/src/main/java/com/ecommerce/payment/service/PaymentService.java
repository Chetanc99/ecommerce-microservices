package com.ecommerce.payment.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.ecommerce.payment.dto.OrderSummary;
import com.ecommerce.payment.dto.PaymentOrderResponse;
import com.ecommerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.payment.dto.VerifyPaymentRequest;

public interface PaymentService {

    PaymentOrderResponse createPayment(JwtPrincipal principal, CreatePaymentRequest request);

    PaymentStatusResponse verifyPayment(JwtPrincipal principal, VerifyPaymentRequest request);

    PaymentStatusResponse getPaymentForOrder(Long orderId, JwtPrincipal principal);

    void handleWebhook(String rawBody, String signature, String eventId);
}
