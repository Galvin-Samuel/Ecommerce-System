package com.ecommerce.model;

/**
 * Enumeration representing the fulfillment lifecycle states of an order.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

