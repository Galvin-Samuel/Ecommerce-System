package com.ecommerce.dto;

import java.math.BigDecimal;

/**
 * Representation of an individual purchased item snapshot within an order.
 */
public record OrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
