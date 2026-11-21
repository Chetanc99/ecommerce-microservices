package com.ecommerce.order.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
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
import com.ecommerce.order.outbox.OrderEventFactory;
import com.ecommerce.order.repository.OrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderPersistenceService persistenceService;
    private final OrderEventFactory eventFactory;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductClient productClient,
            OrderPersistenceService persistenceService,
            OrderEventFactory eventFactory) {

        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.persistenceService = persistenceService;
        this.eventFactory = eventFactory;
    }

    @Override
    public OrderResponse createOrder(JwtPrincipal principal, OrderRequest request) {

        List<ResolvedItem> resolvedItems = resolveItems(request);
        List<ResolvedItem> reservedItems = new ArrayList<>();

        try {
            for (ResolvedItem item : resolvedItems) {
                productClient.reserveStock(item.productId(), item.quantity());
                reservedItems.add(item);
            }

            Order order = buildOrder(principal, resolvedItems);

            return mapToResponse(persistenceService.saveNewOrder(order));

        } catch (RuntimeException exception) {
            releaseReservedStock(reservedItems);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, JwtPrincipal principal) {

        Order order = findOrder(orderId);

        if (!owns(order, principal) && !principal.hasRole("ADMIN")) {
            throw new OrderAccessDeniedException("You cannot view this order");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getCurrentUserOrders(Long userId, Pageable pageable) {

        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return PageResponse.from(orders, this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, JwtPrincipal principal) {

        Order order = findOrder(orderId);

        if (!owns(order, principal)) {
            throw new OrderAccessDeniedException("You cannot cancel this order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return mapToResponse(order);
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new OrderStateException("A paid order cannot be cancelled online");
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.getItems().forEach(item ->
                    productClient.releaseStock(item.getProductId(), item.getQuantity()));
        }

        order.setStatus(OrderStatus.CANCELLED);

        persistenceService.saveWithEvent(
                order,
                eventFactory.orderCancelled(order, "Cancelled by customer")
        );

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(Long orderId, Long userId) {

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException("You cannot access this order");
        }

        return new OrderSummaryResponse(
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalAmount(),
                order.getStatus().name(),
                mapItems(order)
        );
    }

    private List<ResolvedItem> resolveItems(OrderRequest request) {

        List<ResolvedItem> items = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.items()) {
            ProductResponse product = productClient.getProductById(itemRequest.productId());

            if (product.quantity() == null || product.quantity() < itemRequest.quantity()) {
                throw new InsufficientStockException(
                        "Product " + product.id() + " does not have enough stock"
                );
            }

            items.add(new ResolvedItem(product, itemRequest.quantity(), product.id()));
        }

        return items;
    }

    private Order buildOrder(JwtPrincipal principal, List<ResolvedItem> items) {

        Order order = new Order();
        order.setUserId(principal.userId());
        order.setUserEmail(principal.email() == null ? "" : principal.email());
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        BigDecimal total = BigDecimal.ZERO;

        for (ResolvedItem resolved : items) {
            ProductResponse product = resolved.product();

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.id());
            orderItem.setProductName(product.name());
            orderItem.setPrice(product.price());
            orderItem.setQuantity(resolved.quantity());

            order.addItem(orderItem);

            total = total.add(product.price()
                    .multiply(BigDecimal.valueOf(resolved.quantity())));
        }

        order.setTotalAmount(total);
        return order;
    }

    private void releaseReservedStock(List<ResolvedItem> items) {
        items.forEach(item ->
                productClient.releaseStock(item.productId(), item.quantity()));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
    }

    private boolean owns(Order order, JwtPrincipal principal) {
        return order.getUserId().equals(principal.userId());
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                mapItems(order)
        );
    }

    private List<OrderItemResponse> mapItems(Order order) {
        return order.getItems().stream()
                .map(this::mapItem)
                .toList();
    }

    private OrderItemResponse mapItem(OrderItem item) {
        BigDecimal subtotal = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

    private record ResolvedItem(ProductResponse product, int quantity, Long productId) {
    }
}
