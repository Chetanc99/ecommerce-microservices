package com.ecommerce.payment.controller;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.ecommerce.payment.dto.PaymentOrderResponse;
import com.ecommerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.payment.dto.VerifyPaymentRequest;
import com.ecommerce.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders")
    public ResponseEntity<PaymentOrderResponse> createPayment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(principal, request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentStatusResponse> verifyPayment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody VerifyPaymentRequest request) {

        return ResponseEntity.ok(paymentService.verifyPayment(principal, request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentForOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtPrincipal principal) {

        return ResponseEntity.ok(paymentService.getPaymentForOrder(orderId, principal));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {

        paymentService.handleWebhook(rawBody, signature, eventId);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
