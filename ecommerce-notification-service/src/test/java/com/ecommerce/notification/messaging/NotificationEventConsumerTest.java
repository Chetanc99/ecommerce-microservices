package com.ecommerce.notification.messaging;

import com.ecommerce.common.events.OrderCancelledEvent;
import com.ecommerce.common.events.OrderConfirmedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.notification.email.NotificationMessage;
import com.ecommerce.notification.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    private static final String EVENT_ID = "evt-1";
    private static final String EMAIL = "buyer@example.com";
    private static final String OCCURRED_AT = "2026-01-01T00:00:00Z";

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void orderConfirmedEventBecomesAConfirmationEmail() {
        consumer.onOrderConfirmed(new OrderConfirmedEvent(
                EVENT_ID, 42L, 7L, EMAIL, new BigDecimal("59.98"), OCCURRED_AT));

        NotificationMessage message = capture();

        assertThat(message.eventId()).isEqualTo(EVENT_ID);
        assertThat(message.type()).isEqualTo("ORDER_CONFIRMED");
        assertThat(message.recipient()).isEqualTo(EMAIL);
        assertThat(message.subject()).isEqualTo("Order #42 confirmed");
        assertThat(message.body())
                .contains("order #42")
                .contains("INR 59.98");
    }

    @Test
    void paymentSuccessfulEventBecomesAReceiptEmail() {
        consumer.onPaymentSuccessful(new PaymentSuccessfulEvent(
                EVENT_ID, 5L, 42L, 7L, EMAIL, new BigDecimal("59.98"),
                "pay_XYZ789", OCCURRED_AT));

        NotificationMessage message = capture();

        assertThat(message.type()).isEqualTo("PAYMENT_SUCCESSFUL");
        assertThat(message.subject()).isEqualTo("Payment received for order #42");
        assertThat(message.body())
                .contains("INR 59.98")
                .contains("order #42")
                .contains("pay_XYZ789");
    }

    @Test
    void paymentFailedEventBecomesARetryEmailWithTheReason() {
        consumer.onPaymentFailed(new PaymentFailedEvent(
                EVENT_ID, 5L, 42L, 7L, EMAIL, new BigDecimal("59.98"),
                "Card declined", OCCURRED_AT));

        NotificationMessage message = capture();

        assertThat(message.type()).isEqualTo("PAYMENT_FAILED");
        assertThat(message.subject()).isEqualTo("Payment failed for order #42");
        assertThat(message.body())
                .contains("could not be completed")
                .contains("Card declined");
    }

    @Test
    void cancelledEventBecomesACancellationEmailWithTheReason() {
        consumer.onOrderCancelled(new OrderCancelledEvent(
                EVENT_ID, 42L, 7L, EMAIL, new BigDecimal("59.98"),
                "Cancelled by customer", OCCURRED_AT));

        NotificationMessage message = capture();

        assertThat(message.type()).isEqualTo("ORDER_CANCELLED");
        assertThat(message.subject()).isEqualTo("Order #42 cancelled");
        assertThat(message.recipient()).isEqualTo(EMAIL);
        assertThat(message.body())
                .contains("has been cancelled")
                .contains("Cancelled by customer");
    }

    private NotificationMessage capture() {
        ArgumentCaptor<NotificationMessage> captor =
                ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService).deliver(captor.capture());
        return captor.getValue();
    }
}
