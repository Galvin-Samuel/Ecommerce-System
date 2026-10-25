package com.ecommerce.dto;

/**
 * Optional request payload when creating an order from a user cart.
 */
public record OrderRequest(
        String notes
) {
    public OrderRequest() {
        this(null);
    }
}
