package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.entity.ProcessedWebhook;
import com.ecommerce.payment.razorpay.RazorpayWebhookEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedWebhookRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookHandlerTest {

    private static final String EVENT_ID = "evt_razorpay_1";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedWebhookRepository processedWebhookRepository;

    @Mock
    private PaymentSettlementService settlementService;

    @InjectMocks
    private PaymentWebhookHandler handler;

    @Test
    void duplicateWebhookDeliveriesAreIgnored() {
        when(processedWebhookRepository.existsById(EVENT_ID)).thenReturn(true);

        handler.handle(EVENT_ID, capturedEvent());

        verifyNoInteractions(paymentRepository, settlementService);
        verify(processedWebhookRepository, never()).save(any());
    }

    @Test
    void capturedPaymentSettlesTheLocalPaymentAndRecordsTheEvent() {
        Payment payment = payment();
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.of(payment));

        handler.handle(EVENT_ID, capturedEvent());

        verify(settlementService).markSuccessful(payment, "pay_XYZ23");
        verify(processedWebhookRepository).save(any(ProcessedWebhook.class));
    }

    @Test
    void orderPaidAlsoMarksThePaymentSuccessful() {
        Payment payment = payment();
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.of(payment));

        handler.handle(EVENT_ID, new RazorpayWebhookEvent(
                "order.paid", "order_ABC23", "pay_XYZ23", null));

        verify(settlementService).markSuccessful(payment, "pay_XYZ23");
        verify(processedWebhookRepository).save(any(ProcessedWebhook.class));
    }

    @Test
    void failedPaymentMarksThePaymentFailedWithTheReason() {
        Payment payment = payment();
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.of(payment));

        handler.handle(EVENT_ID, new RazorpayWebhookEvent(
                "payment.failed", "order_ABC23", "pay_XYZ23", "Card declined"));

        verify(settlementService).markFailed(payment, "Card declined");
        verify(processedWebhookRepository).save(any(ProcessedWebhook.class));
    }

    @Test
    void unknownOrdersAreAcknowledgedWithoutSettlingAnything() {
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.empty());

        handler.handle(EVENT_ID, capturedEvent());

        verifyNoInteractions(settlementService);
        verify(processedWebhookRepository).save(any(ProcessedWebhook.class));
    }

    @Test
    void unrelatedEventsAreIgnoredButRecorded() {
        Payment payment = payment();
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.of(payment));

        handler.handle(EVENT_ID, new RazorpayWebhookEvent(
                "payment.authorized", "order_ABC23", "pay_XYZ23", null));

        verifyNoInteractions(settlementService);
        verify(processedWebhookRepository).save(any(ProcessedWebhook.class));
    }

    @Test
    void eventsWithoutAnIdAreProcessedWithoutAnIdempotencyRecord() {
        Payment payment = payment();
        when(paymentRepository.findByRazorpayOrderId("order_ABC23"))
                .thenReturn(Optional.of(payment));

        handler.handle(null, capturedEvent());

        verify(settlementService).markSuccessful(payment, "pay_XYZ23");
        verify(processedWebhookRepository, never()).save(any());
    }

    private RazorpayWebhookEvent capturedEvent() {
        return new RazorpayWebhookEvent(
                "payment.captured", "order_ABC23", "pay_XYZ23", null);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(7L);
        payment.setUserEmail("buyer@example.com");
        payment.setAmount(new BigDecimal("59.98"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setRazorpayOrderId("order_ABC23");
        return payment;
    }
}
