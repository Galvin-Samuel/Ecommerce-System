package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detailed representation of an item in the shopping cart.
 */
public record CartItemResponse(
        String id,
        String productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Instant addedAt
) {
}
