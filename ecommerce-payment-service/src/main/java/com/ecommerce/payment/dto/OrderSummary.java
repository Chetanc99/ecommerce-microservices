package com.ecommerce.payment.dto;

import java.math.BigDecimal;

public record OrderSummary(
        Long id,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        String status
) {
}
