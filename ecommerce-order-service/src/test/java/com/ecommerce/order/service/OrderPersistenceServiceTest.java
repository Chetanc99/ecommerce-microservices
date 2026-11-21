package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderOutboxEvent;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.outbox.OutboxMessage;
import com.ecommerce.order.repository.OrderOutboxRepository;
import com.ecommerce.order.repository.OrderRepository;

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
class OrderPersistenceServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOutboxRepository outboxRepository;

    @Mock
    private OrderEventFactory eventFactory;

    @InjectMocks
    private OrderPersistenceService persistenceService;

    @Test
    void savingANewOrderAlsoAppendsOrderCreatedToTheOutbox() {
        Order order = order();
        OutboxMessage message = new OutboxMessage("evt-1", "OrderCreated",
                "order.created", "{\"orderId\":1}");

        when(orderRepository.save(order)).thenReturn(order);
        when(eventFactory.orderCreated(order)).thenReturn(message);

        assertThat(persistenceService.saveNewOrder(order)).isSameAs(order);

        ArgumentCaptor<OrderOutboxEvent> captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OrderOutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.getEventId()).isEqualTo("evt-1");
        assertThat(outboxEvent.getEventType()).isEqualTo("OrderCreated");
        assertThat(outboxEvent.getTopic()).isEqualTo("order.created");
        assertThat(outboxEvent.getPayload()).isEqualTo("{\"orderId\":1}");
        assertThat(outboxEvent.isPublished()).isFalse();
    }

    @Test
    void savingWithAnEventPersistsTheOrderAndTheSuppliedMessage() {
        Order order = order();
        OutboxMessage message = new OutboxMessage("evt-2", "OrderCancelled",
                "order.cancelled", "{\"reason\":\"customer\"}");

        persistenceService.saveWithEvent(order, message);

        verify(orderRepository).save(order);

        ArgumentCaptor<OrderOutboxEvent> captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        assertThat(captor.getValue().getEventId()).isEqualTo("evt-2");
        assertThat(captor.getValue().getTopic()).isEqualTo("order.cancelled");
        assertThat(captor.getValue().isPublished()).isFalse();
    }

    private Order order() {
        Order order = new Order();
        order.setUserId(7L);
        order.setUserEmail("buyer@example.com");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("10.00"));
        return order;
    }
}
