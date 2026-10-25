package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates the complete user shopping cart state,
 * including item details, total quantities, and aggregate cart total.
 */
public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal totalPrice,
        int totalItems
) {
    public static CartResponse empty() {
        return new CartResponse(List.of(), BigDecimal.ZERO, 0);
    }
}
