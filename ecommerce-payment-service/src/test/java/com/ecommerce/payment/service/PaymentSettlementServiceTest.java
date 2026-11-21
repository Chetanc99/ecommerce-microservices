package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.outbox.OutboxMessage;
import com.ecommerce.payment.outbox.PaymentEventFactory;
import com.ecommerce.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementServiceTest {

    private static final OutboxMessage SUCCESS_MESSAGE =
            new OutboxMessage("evt-success", "PaymentSuccessful", "payment.successful", "{}");
    private static final OutboxMessage FAILED_MESSAGE =
            new OutboxMessage("evt-failed", "PaymentFailed", "payment.failed", "{}");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPersistenceService persistenceService;

    @Mock
    private PaymentEventFactory eventFactory;

    @InjectMocks
    private PaymentSettlementService settlementService;

    @Test
    void markingACreatedPaymentSuccessfulLinksTheRazorpayPaymentAndEmitsAnEvent() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.existsByRazorpayPaymentIdAndIdNot("pay_XYZ", null)).thenReturn(false);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentSuccessful(payment)).thenReturn(SUCCESS_MESSAGE);

        Payment settled = settlementService.markSuccessful(payment, "pay_XYZ");

        assertThat(settled.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(settled.getRazorpayPaymentId()).isEqualTo("pay_XYZ");
        assertThat(settled.getFailureReason()).isNull();
        verify(persistenceService).appendEvent(SUCCESS_MESSAGE);
    }

    @Test
    void anAlreadySuccessfulPaymentIsReturnedWithoutADuplicateEvent() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId("pay_XYZ");

        Payment settled = settlementService.markSuccessful(payment, "pay_XYZ");

        assertThat(settled).isSameAs(payment);
        verifyNoInteractions(paymentRepository, persistenceService, eventFactory);
    }

    @Test
    void aRazorpayPaymentIdCannotBeLinkedToTwoLocalPayments() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.existsByRazorpayPaymentIdAndIdNot("pay_XYZ", null)).thenReturn(true);

        assertThatThrownBy(() -> settlementService.markSuccessful(payment, "pay_XYZ"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(persistenceService, eventFactory);
    }

    @Test
    void aLateCaptureOverridesAnEarlierFailure() {
        Payment payment = payment(PaymentStatus.FAILED);
        payment.setFailureReason("Declined");

        when(paymentRepository.existsByRazorpayPaymentIdAndIdNot("pay_XYZ", null)).thenReturn(false);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentSuccessful(payment)).thenReturn(SUCCESS_MESSAGE);

        Payment settled = settlementService.markSuccessful(payment, "pay_XYZ");

        assertThat(settled.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(settled.getFailureReason()).isNull();
    }

    @Test
    void failureMarksThePaymentAndEmitsAnEvent() {
        Payment payment = payment(PaymentStatus.PENDING);

        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentFailed(payment, "Card declined")).thenReturn(FAILED_MESSAGE);

        Payment failed = settlementService.markFailed(payment, "Card declined");

        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Card declined");
        verify(persistenceService).appendEvent(FAILED_MESSAGE);
    }

    @Test
    void failureReasonIsCappedAtColumnLength() {
        Payment payment = payment(PaymentStatus.CREATED);
        String reason = "x".repeat(600);

        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentFailed(payment, reason.substring(0, 500))).thenReturn(FAILED_MESSAGE);

        settlementService.markFailed(payment, reason);

        assertThat(payment.getFailureReason()).hasSize(500);
    }

    @Test
    void missingFailureReasonFallsBackToADefault() {
        Payment payment = payment(PaymentStatus.CREATED);

        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentFailed(payment, "Payment failed")).thenReturn(FAILED_MESSAGE);

        settlementService.markFailed(payment, null);

        assertThat(payment.getFailureReason()).isEqualTo("Payment failed");
    }

    @Test
    void aSuccessfulPaymentIsNeverDowngradedByAFailure() {
        Payment payment = payment(PaymentStatus.SUCCESS);

        assertThat(settlementService.markFailed(payment, "late failure")).isSameAs(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verifyNoInteractions(paymentRepository, persistenceService, eventFactory);
    }

    @Test
    void aRepeatedFailureIsIdempotent() {
        Payment payment = payment(PaymentStatus.FAILED);

        settlementService.markFailed(payment, "again");

        verifyNoInteractions(paymentRepository, persistenceService, eventFactory);
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(7L);
        payment.setUserEmail("buyer@example.com");
        payment.setAmount(new BigDecimal("59.98"));
        payment.setCurrency("INR");
        payment.setStatus(status);
        payment.setRazorpayOrderId("order_ABC");
        return payment;
    }
}
