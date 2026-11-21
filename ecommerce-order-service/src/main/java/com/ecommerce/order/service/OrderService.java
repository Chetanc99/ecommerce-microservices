package com.ecommerce.order.service;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.OrderSummaryResponse;
import com.ecommerce.order.dto.PageResponse;

import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(JwtPrincipal principal, OrderRequest request);

    OrderResponse getOrder(Long orderId, JwtPrincipal principal);

    PageResponse<OrderResponse> getCurrentUserOrders(Long userId, Pageable pageable);

    OrderResponse cancelOrder(Long orderId, JwtPrincipal principal);

    OrderSummaryResponse getOrderSummary(Long orderId, Long userId);
}
