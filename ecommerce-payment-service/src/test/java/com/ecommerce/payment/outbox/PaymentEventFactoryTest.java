package com.ecommerce.payment.outbox;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentEventFactory eventFactory = new PaymentEventFactory();

    @Test
    void initiatedEventCarriesTheRazorpayOrderId() throws Exception {
        OutboxMessage message = eventFactory.paymentInitiated(payment());

        assertThat(message.eventType()).isEqualTo("PaymentInitiated");
        assertThat(message.topic()).isEqualTo(KafkaTopics.PAYMENT_INITIATED);

        JsonNode payload = MAPPER.readTree(message.payload());
        assertThat(payload.get("eventId").asText()).isEqualTo(message.eventId());
        assertThat(payload.get("orderId").asLong()).isEqualTo(1L);
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("59.98");
        assertThat(payload.get("razorpayOrderId").asText()).isEqualTo("order_ABC23");
        assertThat(Instant.parse(payload.get("occurredAt").asText())).isNotNull();
    }

    @Test
    void successfulEventCarriesTheRazorpayPaymentId() throws Exception {
        Payment payment = payment();
        payment.setRazorpayPaymentId("pay_XYZ23");

        OutboxMessage message = eventFactory.paymentSuccessful(payment);

        assertThat(message.eventType()).isEqualTo("PaymentSuccessful");
        assertThat(message.topic()).isEqualTo(KafkaTopics.PAYMENT_SUCCESSFUL);
        assertThat(MAPPER.readTree(message.payload()).get("razorpayPaymentId").asText())
                .isEqualTo("pay_XYZ23");
    }

    @Test
    void failedEventCarriesTheReason() throws Exception {
        OutboxMessage message = eventFactory.paymentFailed(payment(), "Card declined");

        assertThat(message.eventType()).isEqualTo("PaymentFailed");
        assertThat(message.topic()).isEqualTo(KafkaTopics.PAYMENT_FAILED);
        assertThat(MAPPER.readTree(message.payload()).get("reason").asText())
                .isEqualTo("Card declined");
    }

    @Test
    void everyEventGetsItsOwnIdentifier() {
        String first = eventFactory.paymentInitiated(payment()).eventId();
        String second = eventFactory.paymentInitiated(payment()).eventId();

        assertThat(first).isNotBlank().isNotEqualTo(second);
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
