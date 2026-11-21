package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderOutboxEvent;
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.outbox.OutboxMessage;
import com.ecommerce.order.repository.OrderOutboxRepository;
import com.ecommerce.order.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final OrderEventFactory eventFactory;

    public OrderPersistenceService(
            OrderRepository orderRepository,
            OrderOutboxRepository outboxRepository,
            OrderEventFactory eventFactory) {

        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.eventFactory = eventFactory;
    }

    @Transactional
    public Order saveNewOrder(Order order) {

        Order savedOrder = orderRepository.save(order);
        append(outboxRepository, eventFactory.orderCreated(savedOrder));

        return savedOrder;
    }

    @Transactional
    public void saveWithEvent(Order order, OutboxMessage message) {

        orderRepository.save(order);
        append(outboxRepository, message);
    }

    public void appendEvent(OutboxMessage message) {
        append(outboxRepository, message);
    }

    private void append(OrderOutboxRepository repository, OutboxMessage message) {

        OrderOutboxEvent outboxEvent = new OrderOutboxEvent();
        outboxEvent.setEventId(message.eventId());
        outboxEvent.setEventType(message.eventType());
        outboxEvent.setTopic(message.topic());
        outboxEvent.setPayload(message.payload());

        repository.save(outboxEvent);
    }
}
