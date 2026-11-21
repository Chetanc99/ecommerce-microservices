package com.ecommerce.order.service;

import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.ProcessedEvent;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.outbox.OutboxMessage;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentEventHandlerTest {

    private static final String EVENT_ID = "event-123";
    private static final OutboxMessage CONFIRMED_MESSAGE =
            new OutboxMessage("evt-confirmed", "OrderConfirmed", "order.confirmed", "{}");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OrderPersistenceService persistenceService;

    @Mock
    private OrderEventFactory eventFactory;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderPaymentEventHandler handler;

    @Test
    void duplicatePaymentSuccessIsIgnored() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        handler.handlePaymentSuccessful(paymentSuccessful(99L));

        verifyNoInteractions(orderRepository, persistenceService, eventFactory);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void paymentSuccessConfirmsAnUnpaidOrder() {
        Order order = order(OrderStatus.PENDING_PAYMENT, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));
        when(eventFactory.orderConfirmed(order)).thenReturn(CONFIRMED_MESSAGE);

        handler.handlePaymentSuccessful(paymentSuccessful(1L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
        verify(persistenceService).appendEvent(CONFIRMED_MESSAGE);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicatePaymentSuccessOnAnAlreadyConfirmedOrderDoesNotReEmitOrderConfirmed() {
        Order order = order(OrderStatus.CONFIRMED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        handler.handlePaymentSuccessful(paymentSuccessful(1L));

        verify(persistenceService, never()).appendEvent(any());
        verify(orderRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentSuccessForACancelledOrderIsRecordedWithoutConfirmingIt() {
        Order order = order(OrderStatus.CANCELLED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        handler.handlePaymentSuccessful(paymentSuccessful(1L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(persistenceService, never()).appendEvent(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentFailureReleasesReservedStockAndMarksTheOrder() {
        Order order = order(OrderStatus.PENDING_PAYMENT, item(10L, 2), item(20L, 1));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        handler.handlePaymentFailed(paymentFailed(1L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(productClient).releaseStock(10L, 2);
        verify(productClient).releaseStock(20L, 1);
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicatePaymentFailureIsIgnored() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        handler.handlePaymentFailed(paymentFailed(1L));

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void paymentFailureOnAConfirmedOrderDoesNotReleaseStock() {
        Order order = order(OrderStatus.CONFIRMED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        handler.handlePaymentFailed(paymentFailed(1L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(productClient, never()).releaseStock(anyLong(), anyInt());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentFailurePropagatesWhenStockCannotBeReleasedSoTheEventCanBeRetried() {
        Order order = order(OrderStatus.PENDING_PAYMENT, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));
        doThrow(new InsufficientStockException("Product service unavailable"))
                .when(productClient).releaseStock(10L, 2);

        assertThatThrownBy(() -> handler.handlePaymentFailed(paymentFailed(1L)))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void unknownOrderIsRejected() {
        when(orderRepository.findWithItemsById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handlePaymentSuccessful(paymentSuccessful(55L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");

        verify(processedEventRepository, never()).save(any());
    }

    private PaymentSuccessfulEvent paymentSuccessful(Long orderId) {
        return new PaymentSuccessfulEvent(EVENT_ID, 5L, orderId, 7L, "buyer@example.com",
                new BigDecimal("10.00"), "pay_123", "2026-01-01T00:00:00Z");
    }

    private PaymentFailedEvent paymentFailed(Long orderId) {
        return new PaymentFailedEvent(EVENT_ID, 5L, orderId, 7L, "buyer@example.com",
                new BigDecimal("10.00"), "declined", "2026-01-01T00:00:00Z");
    }

    private OrderItem item(Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Product " + productId);
        item.setPrice(new BigDecimal("10.00"));
        item.setQuantity(quantity);
        return item;
    }

    private Order order(OrderStatus status, OrderItem... items) {
        Order order = new Order();
        order.setUserId(7L);
        order.setUserEmail("buyer@example.com");
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("10.00"));

        for (OrderItem item : items) {
            order.addItem(item);
        }

        return order;
    }
}
