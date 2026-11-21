package com.ecommerce.payment.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.config.RazorpayProperties;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.ecommerce.payment.dto.OrderSummary;
import com.ecommerce.payment.dto.PaymentOrderResponse;
import com.ecommerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.payment.dto.VerifyPaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentAccessDeniedException;
import com.ecommerce.payment.exception.PaymentConfigurationException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentStateException;
import com.ecommerce.payment.exception.PaymentVerificationException;
import com.ecommerce.payment.razorpay.RazorpayGateway;
import com.ecommerce.payment.razorpay.RazorpayOrderResult;
import com.ecommerce.payment.razorpay.RazorpaySignatureVerifier;
import com.ecommerce.payment.razorpay.RazorpayWebhookEvent;
import com.ecommerce.payment.razorpay.RazorpayWebhookParser;
import com.ecommerce.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYABLE_ORDER_STATUS = "PENDING_PAYMENT";

    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceService persistenceService;
    private final PaymentSettlementService settlementService;
    private final PaymentWebhookHandler webhookHandler;
    private final OrderClient orderClient;
    private final RazorpayGateway razorpayGateway;
    private final RazorpaySignatureVerifier signatureVerifier;
    private final RazorpayWebhookParser webhookParser;
    private final RazorpayProperties properties;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentPersistenceService persistenceService,
            PaymentSettlementService settlementService,
            PaymentWebhookHandler webhookHandler,
            OrderClient orderClient,
            RazorpayGateway razorpayGateway,
            RazorpaySignatureVerifier signatureVerifier,
            RazorpayWebhookParser webhookParser,
            RazorpayProperties properties) {

        this.paymentRepository = paymentRepository;
        this.persistenceService = persistenceService;
        this.settlementService = settlementService;
        this.webhookHandler = webhookHandler;
        this.orderClient = orderClient;
        this.razorpayGateway = razorpayGateway;
        this.signatureVerifier = signatureVerifier;
        this.webhookParser = webhookParser;
        this.properties = properties;
    }

    @Override
    public synchronized PaymentOrderResponse createPayment(
            JwtPrincipal principal, CreatePaymentRequest request) {

        OrderSummary order = orderClient.getOrderSummary(request.orderId(), principal.userId());

        if (!PAYABLE_ORDER_STATUS.equals(order.status())) {
            throw new PaymentStateException("Order " + order.id() + " is not awaiting payment");
        }

        if (hasSuccessfulPayment(order.id())) {
            throw new PaymentStateException("Order " + order.id() + " has already been paid");
        }

        Payment reusable = paymentRepository
                .findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                        order.id(), List.of(PaymentStatus.CREATED, PaymentStatus.PENDING))
                .orElse(null);

        if (reusable != null) {
            return toOrderResponse(reusable);
        }

        if (!properties.isConfigured()) {
            throw new PaymentConfigurationException(
                    "Razorpay test credentials are not configured on the server"
            );
        }

        RazorpayOrderResult razorpayOrder = razorpayGateway.createOrder(
                order.totalAmount(),
                "order_" + order.id(),
                Map.of("orderId", order.id().toString(), "userId", principal.userId().toString())
        );

        Payment payment = new Payment();
        payment.setOrderId(order.id());
        payment.setUserId(principal.userId());
        payment.setUserEmail(principal.email() == null ? order.userEmail() : principal.email());
        payment.setAmount(order.totalAmount());
        payment.setCurrency(razorpayOrder.currency());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setRazorpayOrderId(razorpayOrder.orderId());

        return toOrderResponse(persistenceService.createPayment(payment));
    }

    @Override
    public PaymentStatusResponse verifyPayment(
            JwtPrincipal principal,
            VerifyPaymentRequest request) {

        Payment payment = findByRazorpayOrderId(request.razorpayOrderId());
        assertOwner(payment, principal);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toStatusResponse(payment);
        }

        if (!properties.isConfigured()) {
            throw new PaymentConfigurationException(
                    "Razorpay test credentials are not configured on the server"
            );
        }

        boolean validSignature = signatureVerifier.verifyPayment(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature(),
                properties.keySecret()
        );

        if (!validSignature) {
            throw new PaymentVerificationException("Razorpay payment signature is invalid");
        }

        try {
            Payment settled = settlementService.markSuccessful(
                    payment, request.razorpayPaymentId());
            return toStatusResponse(settled);
        } catch (IllegalStateException exception) {
            throw new PaymentStateException(exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentForOrder(Long orderId, JwtPrincipal principal) {

        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment exists for order " + orderId));

        assertOwner(payment, principal);
        return toStatusResponse(payment);
    }

    @Override
    public void handleWebhook(String rawBody, String signature, String eventId) {

        if (!properties.isWebhookConfigured()) {
            throw new PaymentConfigurationException(
                    "Razorpay webhook secret is not configured on the server"
            );
        }

        boolean validSignature = signatureVerifier.verifyWebhook(
                rawBody, signature, properties.webhookSecret());

        if (!validSignature) {
            throw new PaymentVerificationException("Razorpay webhook signature is invalid");
        }

        RazorpayWebhookEvent event = webhookParser.parse(rawBody);
        webhookHandler.handle(eventId, event);
    }

    private boolean hasSuccessfulPayment(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.SUCCESS);
    }

    private Payment findByRazorpayOrderId(String razorpayOrderId) {
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for Razorpay order " + razorpayOrderId));
    }

    private void assertOwner(Payment payment, JwtPrincipal principal) {
        if (!payment.getUserId().equals(principal.userId())) {
            throw new PaymentAccessDeniedException("You cannot access this payment");
        }
    }

    private PaymentOrderResponse toOrderResponse(Payment payment) {
        return new PaymentOrderResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getRazorpayOrderId(),
                properties.keyId(),
                payment.getAmount(),
                payment.getCurrency()
        );
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getRazorpayPaymentId()
        );
    }
}
