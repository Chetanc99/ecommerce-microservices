package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.outbox.OutboxMessage;
import com.ecommerce.payment.outbox.PaymentEventFactory;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import com.ecommerce.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPersistenceServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentOutboxRepository outboxRepository;

    @Mock
    private PaymentEventFactory eventFactory;

    @InjectMocks
    private PaymentPersistenceService persistenceService;

    @Test
    void creatingAPaymentAlsoAppendsPaymentInitiatedToTheOutbox() {
        Payment payment = payment();
        OutboxMessage message = new OutboxMessage("evt-1", "PaymentInitiated",
                "payment.initiated", "{\"paymentId\":1}");

        when(paymentRepository.save(payment)).thenReturn(payment);
        when(eventFactory.paymentInitiated(payment)).thenReturn(message);

        assertThat(persistenceService.createPayment(payment)).isSameAs(payment);

        ArgumentCaptor<PaymentOutboxEvent> captor =
                ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        assertThat(captor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(captor.getValue().getEventType()).isEqualTo("PaymentInitiated");
        assertThat(captor.getValue().getTopic()).isEqualTo("payment.initiated");
        assertThat(captor.getValue().isPublished()).isFalse();
    }

    @Test
    void savingWithAnEventPersistsThePaymentAndTheSuppliedMessage() {
        Payment payment = payment();
        OutboxMessage message = new OutboxMessage("evt-2", "PaymentFailed",
                "payment.failed", "{}");

        when(paymentRepository.save(payment)).thenReturn(payment);

        persistenceService.saveWithEvent(payment, message);

        ArgumentCaptor<PaymentOutboxEvent> captor =
                ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        assertThat(captor.getValue().getEventId()).isEqualTo("evt-2");
        assertThat(captor.getValue().getTopic()).isEqualTo("payment.failed");
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
