package com.ecommerce.order.service;

import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.ProcessedEvent;
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentEventHandler.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderPersistenceService persistenceService;
    private final OrderEventFactory eventFactory;
    private final ProductClient productClient;

    public OrderPaymentEventHandler(
            OrderRepository orderRepository,
            ProcessedEventRepository processedEventRepository,
            OrderPersistenceService persistenceService,
            OrderEventFactory eventFactory,
            ProductClient productClient) {

        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.persistenceService = persistenceService;
        this.eventFactory = eventFactory;
        this.productClient = productClient;
    }

    @Transactional
    public void handlePaymentSuccessful(PaymentSuccessfulEvent event) {

        if (alreadyProcessed(event.eventId())) {
            return;
        }

        Order order = findOrder(event.orderId());

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            persistenceService.appendEvent(eventFactory.orderConfirmed(order));
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            log.error("Payment succeeded for cancelled order {}", order.getId());
        }

        markProcessed(event.eventId());
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        if (alreadyProcessed(event.eventId())) {
            return;
        }

        Order order = findOrder(event.orderId());

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            releaseStock(order);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }

        markProcessed(event.eventId());
    }

    private boolean alreadyProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order " + orderId + " referenced by a payment event does not exist"
                ));
    }

    private void releaseStock(Order order) {
        order.getItems().forEach(item ->
                productClient.releaseStock(item.getProductId(), item.getQuantity()));
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId));
    }
}
