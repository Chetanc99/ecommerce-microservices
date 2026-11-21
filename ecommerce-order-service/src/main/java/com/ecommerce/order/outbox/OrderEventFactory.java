package com.ecommerce.order.outbox;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.OrderCancelledEvent;
import com.ecommerce.common.events.OrderConfirmedEvent;
import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.common.events.OrderItemPayload;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OrderEventFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public OutboxMessage orderCreated(Order order) {

        String eventId = UUID.randomUUID().toString();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalAmount(),
                items(order),
                Instant.now().toString()
        );

        return message(eventId, "OrderCreated", KafkaTopics.ORDER_CREATED, event);
    }

    public OutboxMessage orderConfirmed(Order order) {

        String eventId = UUID.randomUUID().toString();

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                eventId,
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalAmount(),
                Instant.now().toString()
        );

        return message(eventId, "OrderConfirmed", KafkaTopics.ORDER_CONFIRMED, event);
    }

    public OutboxMessage orderCancelled(Order order, String reason) {

        String eventId = UUID.randomUUID().toString();

        OrderCancelledEvent event = new OrderCancelledEvent(
                eventId,
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalAmount(),
                reason,
                Instant.now().toString()
        );

        return message(eventId, "OrderCancelled", KafkaTopics.ORDER_CANCELLED, event);
    }

    private List<OrderItemPayload> items(Order order) {
        return order.getItems().stream()
                .map(this::item)
                .toList();
    }

    private OrderItemPayload item(OrderItem item) {
        return new OrderItemPayload(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }

    private OutboxMessage message(String eventId, String eventType, String topic, Object event) {
        try {
            return new OutboxMessage(eventId, eventType, topic, MAPPER.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize " + eventType + " event", exception);
        }
    }
}
