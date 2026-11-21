package com.ecommerce.payment.outbox;

import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentInitiatedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.repository.PaymentOutboxRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Class<?>> EVENT_TYPES = Map.of(
            "PaymentInitiated", PaymentInitiatedEvent.class,
            "PaymentSuccessful", PaymentSuccessfulEvent.class,
            "PaymentFailed", PaymentFailedEvent.class
    );

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentOutboxPublisher(
            PaymentOutboxRepository outboxRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {

        List<PaymentOutboxEvent> pending =
                outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();

        for (PaymentOutboxEvent outboxEvent : pending) {
            publish(outboxEvent);
        }
    }

    private void publish(PaymentOutboxEvent outboxEvent) {

        Class<?> eventType = EVENT_TYPES.get(outboxEvent.getEventType());

        if (eventType == null) {
            log.error("Unknown outbox event type {}", outboxEvent.getEventType());
            return;
        }

        try {
            Object event = MAPPER.readValue(outboxEvent.getPayload(), eventType);

            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventId(), event)
                    .get(10, TimeUnit.SECONDS);

            outboxEvent.setPublished(true);
            outboxEvent.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(outboxEvent);

        } catch (Exception exception) {
            log.warn("Could not publish outbox event {}: {}",
                    outboxEvent.getEventId(), exception.getMessage());
        }
    }
}
