package com.ecommerce.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String category,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
