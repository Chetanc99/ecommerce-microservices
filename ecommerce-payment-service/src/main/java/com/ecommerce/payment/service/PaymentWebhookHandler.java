package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.entity.ProcessedWebhook;
import com.ecommerce.payment.razorpay.RazorpayWebhookEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedWebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookHandler.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final PaymentSettlementService settlementService;

    public PaymentWebhookHandler(
            PaymentRepository paymentRepository,
            ProcessedWebhookRepository processedWebhookRepository,
            PaymentSettlementService settlementService) {

        this.paymentRepository = paymentRepository;
        this.processedWebhookRepository = processedWebhookRepository;
        this.settlementService = settlementService;
    }

    @Transactional
    public void handle(String eventId, RazorpayWebhookEvent event) {

        if (eventId != null && processedWebhookRepository.existsById(eventId)) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(event.razorpayOrderId())
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook {} references unknown Razorpay order {}",
                    event.event(), event.razorpayOrderId());
            markProcessed(eventId);
            return;
        }

        switch (event.event()) {
            case "payment.captured", "order.paid" ->
                    settlementService.markSuccessful(payment, event.razorpayPaymentId());
            case "payment.failed" ->
                    settlementService.markFailed(payment, event.failureReason());
            default -> log.debug("Ignoring Razorpay webhook event {}", event.event());
        }

        markProcessed(eventId);
    }

    private void markProcessed(String eventId) {
        if (eventId != null) {
            processedWebhookRepository.save(new ProcessedWebhook(eventId));
        }
    }
}
