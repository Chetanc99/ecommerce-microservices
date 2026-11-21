package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderSummaryResponse;
import com.ecommerce.order.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderSummaryResponse> getOrderSummary(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(orderService.getOrderSummary(orderId, userId));
    }
}
