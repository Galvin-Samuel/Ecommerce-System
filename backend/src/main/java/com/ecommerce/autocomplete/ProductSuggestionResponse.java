package com.ecommerce.autocomplete;

import java.math.BigDecimal;

/**
 * Lightweight DTO representing a product search suggestion.
 */
public record ProductSuggestionResponse(
        String productId,
        String name,
        String category,
        BigDecimal price,
        String highlight
) {
}
