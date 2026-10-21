package com.ecommerce.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderModelTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid Order and OrderItem should pass validation")
    void validOrderShouldPassValidation() {
        OrderItem item = new OrderItem("prod1", "Keyboard", 1, new BigDecimal("49.99"), new BigDecimal("49.99"));
        Order order = new Order("user123", List.of(item), new BigDecimal("49.99"), OrderStatus.PENDING);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);
        assertTrue(violations.isEmpty(), "Expected no violations for valid order");
    }

    @Test
    @DisplayName("Order with empty order items list should fail validation")
    void orderWithEmptyItemsShouldFail() {
        Order order = new Order("user123", new ArrayList<>(), new BigDecimal("0.00"), OrderStatus.PENDING);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("orderItems")));
    }

    @Test
    @DisplayName("Order with negative total amount should fail validation")
    void orderWithNegativeTotalShouldFail() {
        OrderItem item = new OrderItem("prod1", "Keyboard", 1, new BigDecimal("49.99"), new BigDecimal("49.99"));
        Order order = new Order("user123", List.of(item), new BigDecimal("-10.00"), OrderStatus.PENDING);

        Set<ConstraintViolation<Order>> violations = validator.validate(order);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("totalAmount")));
    }

    @Test
    @DisplayName("OrderItem with invalid fields should fail validation")
    void orderItemWithInvalidFieldsShouldFail() {
        OrderItem item = new OrderItem("", " ", 0, new BigDecimal("-1.00"), new BigDecimal("-1.00"));

        Set<ConstraintViolation<OrderItem>> violations = validator.validate(item);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("unitPrice")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("subtotal")));
    }

    @Test
    @DisplayName("OrderStatus enum values should be complete")
    void orderStatusEnumValues() {
        assertEquals(6, OrderStatus.values().length);
        assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.valueOf("CONFIRMED"));
        assertEquals(OrderStatus.PROCESSING, OrderStatus.valueOf("PROCESSING"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.valueOf("SHIPPED"));
        assertEquals(OrderStatus.DELIVERED, OrderStatus.valueOf("DELIVERED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("Order properties, getters, setters, equals, and hashCode")
    void orderPropertiesAndEquals() {
        Instant now = Instant.now();
        OrderItem item1 = new OrderItem("p1", "Item1", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        OrderItem item2 = new OrderItem("p1", "Item1", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        Order o1 = new Order("ord1", "user1", List.of(item1), new BigDecimal("20.00"), OrderStatus.CONFIRMED, now, now);
        Order o2 = new Order("ord1", "user1", List.of(item1), new BigDecimal("20.00"), OrderStatus.CONFIRMED, now, now);

        assertEquals("ord1", o1.getId());
        assertEquals("user1", o1.getUserId());
        assertEquals(1, o1.getOrderItems().size());
        assertEquals(new BigDecimal("20.00"), o1.getTotalAmount());
        assertEquals(OrderStatus.CONFIRMED, o1.getStatus());
        assertEquals(now, o1.getCreatedAt());
        assertEquals(now, o1.getUpdatedAt());

        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
        assertNotNull(o1.toString());
        assertNotNull(item1.toString());
    }
}

