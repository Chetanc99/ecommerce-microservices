package com.ecommerce.common.events;

import java.math.BigDecimal;

public record OrderItemPayload(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
}
