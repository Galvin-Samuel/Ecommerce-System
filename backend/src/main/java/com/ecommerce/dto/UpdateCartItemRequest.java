package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for updating the quantity of an existing shopping cart item.
 */
public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive and greater than zero")
        Integer quantity
) {
}
