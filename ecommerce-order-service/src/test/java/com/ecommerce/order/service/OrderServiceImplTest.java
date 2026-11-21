package com.ecommerce.order.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.OrderSummaryResponse;
import com.ecommerce.order.dto.PageResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.OrderAccessDeniedException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.OrderStateException;
import com.ecommerce.order.exception.ProductUnavailableException;
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.outbox.OutboxMessage;
import com.ecommerce.order.repository.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final JwtPrincipal BUYER =
            new JwtPrincipal(7L, "buyer@example.com", List.of("USER"));

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderPersistenceService persistenceService;

    @Mock
    private OrderEventFactory eventFactory;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderReservesStockAndComputesTotalsFromProductData() {
        when(productClient.getProductById(10L))
                .thenReturn(product(10L, "Keyboard", "19.99", 50));
        when(productClient.getProductById(20L))
                .thenReturn(product(20L, "Mouse Pad", "5.50", 50));
        when(persistenceService.saveNewOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(BUYER, request(
                new OrderItemRequest(10L, 2),
                new OrderItemRequest(20L, 3)));

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.totalAmount()).isEqualByComparingTo("56.48");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).productName()).isEqualTo("Keyboard");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("39.98");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo("16.50");

        verify(productClient).reserveStock(10L, 2);
        verify(productClient).reserveStock(20L, 3);
    }

    @Test
    void createOrderStoresBuyerIdentityFromTokenNotFromRequest() {
        when(productClient.getProductById(10L))
                .thenReturn(product(10L, "Keyboard", "19.99", 5));
        when(persistenceService.saveNewOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(BUYER, request(new OrderItemRequest(10L, 1)));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(persistenceService).saveNewOrder(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getUserEmail()).isEqualTo("buyer@example.com");
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void createOrderRejectsQuantityAboveAvailableStockBeforeReserving() {
        when(productClient.getProductById(10L))
                .thenReturn(product(10L, "Keyboard", "19.99", 1));

        assertThatThrownBy(() -> orderService.createOrder(BUYER,
                request(new OrderItemRequest(10L, 2))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("does not have enough stock");

        verify(productClient, never()).reserveStock(anyLong(), anyInt());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void createOrderDoesNotReserveAnythingWhenALookupFails() {
        when(productClient.getProductById(10L))
                .thenReturn(product(10L, "Keyboard", "19.99", 10));
        when(productClient.getProductById(20L))
                .thenThrow(new ProductUnavailableException("Product 20 is not available"));

        assertThatThrownBy(() -> orderService.createOrder(BUYER, request(
                new OrderItemRequest(10L, 1),
                new OrderItemRequest(20L, 1))))
                .isInstanceOf(ProductUnavailableException.class);

        verify(productClient, never()).reserveStock(anyLong(), anyInt());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void createOrderReleasesAlreadyReservedStockWhenALaterReservationFails() {
        when(productClient.getProductById(10L))
                .thenReturn(product(10L, "Keyboard", "19.99", 10));
        when(productClient.getProductById(20L))
                .thenReturn(product(20L, "Mouse Pad", "5.50", 10));
        doNothing().when(productClient).reserveStock(10L, 2);
        doThrow(new InsufficientStockException("Product 20 does not have enough stock"))
                .when(productClient).reserveStock(20L, 1);

        assertThatThrownBy(() -> orderService.createOrder(BUYER, request(
                new OrderItemRequest(10L, 2),
                new OrderItemRequest(20L, 1))))
                .isInstanceOf(InsufficientStockException.class);

        verify(productClient).releaseStock(10L, 2);
        verify(productClient, never()).releaseStock(20L, 1);
        verifyNoInteractions(persistenceService);
    }

    @Test
    void getOrderHidesOtherBuyersOrdersFromNormalUsers() {
        when(orderRepository.findWithItemsById(1L))
                .thenReturn(Optional.of(order(7L, OrderStatus.PENDING_PAYMENT, item(10L, 1))));

        JwtPrincipal other = new JwtPrincipal(8L, "other@example.com", List.of("USER"));

        assertThatThrownBy(() -> orderService.getOrder(1L, other))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void getOrderAllowsTheOwnerAndAdministrators() {
        Order stored = order(7L, OrderStatus.CONFIRMED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));

        assertThat(orderService.getOrder(1L, BUYER).status()).isEqualTo("CONFIRMED");

        JwtPrincipal admin = new JwtPrincipal(99L, "admin@example.com", List.of("USER", "ADMIN"));
        assertThat(orderService.getOrder(1L, admin).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void getOrderFailsForUnknownId() {
        when(orderRepository.findWithItemsById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(404L, BUYER))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getCurrentUserOrdersReturnsOnlyTheRequestedUsersPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(orderRepository.findByUserId(7L, pageable)).thenReturn(
                new PageImpl<>(List.of(order(7L, OrderStatus.CONFIRMED, item(10L, 1))),
                        pageable, 1));

        PageResponse<OrderResponse> page = orderService.getCurrentUserOrders(7L, pageable);

        assertThat(page.content()).singleElement()
                .extracting(OrderResponse::userId)
                .isEqualTo(7L);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void cancellingAnUnpaidOrderReleasesStockAndEmitsAnEvent() {
        Order stored = order(7L, OrderStatus.PENDING_PAYMENT,
                item(10L, 2), item(20L, 1));
        OutboxMessage message = new OutboxMessage("evt-1", "OrderCancelled",
                "order.cancelled", "{}");

        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));
        when(eventFactory.orderCancelled(stored, "Cancelled by customer")).thenReturn(message);

        OrderResponse response = orderService.cancelOrder(1L, BUYER);

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(productClient).releaseStock(10L, 2);
        verify(productClient).releaseStock(20L, 1);
        verify(persistenceService).saveWithEvent(stored, message);
    }

    @Test
    void cancellingAnAlreadyCancelledOrderIsIdempotent() {
        Order stored = order(7L, OrderStatus.CANCELLED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));

        assertThat(orderService.cancelOrder(1L, BUYER).status()).isEqualTo("CANCELLED");

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
        verify(persistenceService, never()).saveWithEvent(any(), any());
    }

    @Test
    void paidOrdersCannotBeCancelledOnline() {
        Order stored = order(7L, OrderStatus.CONFIRMED, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, BUYER))
                .isInstanceOf(OrderStateException.class)
                .hasMessageContaining("paid order");

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
        verify(persistenceService, never()).saveWithEvent(any(), any());
    }

    @Test
    void cancellingSomeoneElsesOrderIsDenied() {
        Order stored = order(8L, OrderStatus.PENDING_PAYMENT, item(10L, 2));
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, BUYER))
                .isInstanceOf(OrderAccessDeniedException.class);

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    void cancellingAPaymentFailedOrderDoesNotReleaseStockTwice() {
        Order stored = order(7L, OrderStatus.PAYMENT_FAILED, item(10L, 2));
        OutboxMessage message = new OutboxMessage("evt-2", "OrderCancelled",
                "order.cancelled", "{}");

        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(stored));
        when(eventFactory.orderCancelled(stored, "Cancelled by customer")).thenReturn(message);

        assertThat(orderService.cancelOrder(1L, BUYER).status()).isEqualTo("CANCELLED");

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
        verify(persistenceService).saveWithEvent(stored, message);
    }

    @Test
    void orderSummaryIsOnlyVisibleToTheBuyer() {
        when(orderRepository.findWithItemsById(1L))
                .thenReturn(Optional.of(order(7L, OrderStatus.PENDING_PAYMENT, item(10L, 2))));

        OrderSummaryResponse summary = orderService.getOrderSummary(1L, 7L);

        assertThat(summary.userId()).isEqualTo(7L);
        assertThat(summary.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(summary.items()).singleElement()
                .extracting(com.ecommerce.order.dto.OrderItemResponse::productId)
                .isEqualTo(10L);

        assertThatThrownBy(() -> orderService.getOrderSummary(1L, 8L))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    private OrderRequest request(OrderItemRequest... items) {
        return new OrderRequest(List.of(items));
    }

    private ProductResponse product(Long id, String name, String price, int quantity) {
        return new ProductResponse(id, name, "Description", new BigDecimal(price), quantity,
                "Electronics", null);
    }

    private com.ecommerce.order.entity.OrderItem item(Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Product " + productId);
        item.setPrice(new BigDecimal("10.00"));
        item.setQuantity(quantity);
        return item;
    }

    private Order order(Long userId, OrderStatus status, OrderItem... items) {

        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail("buyer@example.com");
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("10.00"));

        for (OrderItem item : items) {
            order.addItem(item);
        }

        return order;
    }
}
