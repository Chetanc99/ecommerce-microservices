package com.ecommerce.order.outbox;

import com.ecommerce.common.events.OrderCancelledEvent;
import com.ecommerce.common.events.OrderConfirmedEvent;
import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.order.entity.OrderOutboxEvent;
import com.ecommerce.order.repository.OrderOutboxRepository;

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
public class OrderOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Class<?>> EVENT_TYPES = Map.of(
            "OrderCreated", OrderCreatedEvent.class,
            "OrderConfirmed", OrderConfirmedEvent.class,
            "OrderCancelled", OrderCancelledEvent.class
    );

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderOutboxPublisher(
            OrderOutboxRepository outboxRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {

        List<OrderOutboxEvent> pending =
                outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();

        for (OrderOutboxEvent outboxEvent : pending) {
            publish(outboxEvent);
        }
    }

    private void publish(OrderOutboxEvent outboxEvent) {

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
