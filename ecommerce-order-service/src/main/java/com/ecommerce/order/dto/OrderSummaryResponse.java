package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderSummaryResponse(
        Long id,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        String status,
        List<OrderItemResponse> items
) {
}
