package com.ecommerce.order.outbox;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderEventFactory eventFactory = new OrderEventFactory();

    @Test
    void orderCreatedEventCarriesOrderSnapshotAndLineItems() throws Exception {
        Order order = order();

        OutboxMessage message = eventFactory.orderCreated(order);

        assertThat(message.eventType()).isEqualTo("OrderCreated");
        assertThat(message.topic()).isEqualTo(KafkaTopics.ORDER_CREATED);
        assertThat(message.eventId()).isNotBlank();

        JsonNode payload = MAPPER.readTree(message.payload());
        assertThat(payload.get("eventId").asText()).isEqualTo(message.eventId());
        assertThat(payload.get("userId").asLong()).isEqualTo(7L);
        assertThat(payload.get("userEmail").asText()).isEqualTo("buyer@example.com");
        assertThat(payload.get("totalAmount").decimalValue())
                .isEqualByComparingTo("59.98");
        assertThat(payload.get("items")).hasSize(2);
        assertThat(payload.get("items").get(0).get("productId").asLong()).isEqualTo(10L);
        assertThat(payload.get("items").get(0).get("quantity").asInt()).isEqualTo(2);
        assertThat(payload.get("items").get(0).get("unitPrice").decimalValue())
                .isEqualByComparingTo("19.99");
        assertThat(Instant.parse(payload.get("occurredAt").asText())).isNotNull();
    }

    @Test
    void orderCancelledEventIncludesTheReason() throws Exception {
        OutboxMessage message = eventFactory.orderCancelled(order(), "Cancelled by customer");

        assertThat(message.eventType()).isEqualTo("OrderCancelled");
        assertThat(message.topic()).isEqualTo(KafkaTopics.ORDER_CANCELLED);

        JsonNode payload = MAPPER.readTree(message.payload());
        assertThat(payload.get("reason").asText()).isEqualTo("Cancelled by customer");
        assertThat(payload.get("totalAmount").decimalValue()).isEqualByComparingTo("59.98");
    }

    @Test
    void orderConfirmedEventUsesTheConfirmedTopic() throws Exception {
        OutboxMessage message = eventFactory.orderConfirmed(order());

        assertThat(message.eventType()).isEqualTo("OrderConfirmed");
        assertThat(message.topic()).isEqualTo(KafkaTopics.ORDER_CONFIRMED);
        assertThat(MAPPER.readTree(message.payload()).get("orderId").isNull()).isTrue();
    }

    @Test
    void everyEventGetsItsOwnIdentifier() {
        Order order = order();

        String first = eventFactory.orderCreated(order).eventId();
        String second = eventFactory.orderCreated(order).eventId();

        assertThat(first).isNotBlank().isNotEqualTo(second);
    }

    private Order order() {
        Order order = new Order();
        order.setUserId(7L);
        order.setUserEmail("buyer@example.com");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("59.98"));
        order.addItem(item(10L, "Keyboard", "19.99", 2));
        order.addItem(item(20L, "Mouse Pad", "20.00", 1));
        return order;
    }

    private OrderItem item(Long productId, String name, String price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(name);
        item.setPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
