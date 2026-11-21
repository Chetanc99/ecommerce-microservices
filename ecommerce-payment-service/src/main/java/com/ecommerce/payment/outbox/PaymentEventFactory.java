package com.ecommerce.payment.outbox;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentInitiatedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.payment.entity.Payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentEventFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public OutboxMessage paymentInitiated(Payment payment) {

        String eventId = UUID.randomUUID().toString();

        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                eventId,
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getUserEmail(),
                payment.getAmount(),
                payment.getRazorpayOrderId(),
                Instant.now().toString()
        );

        return message(eventId, "PaymentInitiated", KafkaTopics.PAYMENT_INITIATED, event);
    }

    public OutboxMessage paymentSuccessful(Payment payment) {

        String eventId = UUID.randomUUID().toString();

        PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(
                eventId,
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getUserEmail(),
                payment.getAmount(),
                payment.getRazorpayPaymentId(),
                Instant.now().toString()
        );

        return message(eventId, "PaymentSuccessful", KafkaTopics.PAYMENT_SUCCESSFUL, event);
    }

    public OutboxMessage paymentFailed(Payment payment, String reason) {

        String eventId = UUID.randomUUID().toString();

        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getUserEmail(),
                payment.getAmount(),
                reason,
                Instant.now().toString()
        );

        return message(eventId, "PaymentFailed", KafkaTopics.PAYMENT_FAILED, event);
    }

    private OutboxMessage message(String eventId, String eventType, String topic, Object event) {
        try {
            return new OutboxMessage(eventId, eventType, topic, MAPPER.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize " + eventType + " event", exception);
        }
    }
}
