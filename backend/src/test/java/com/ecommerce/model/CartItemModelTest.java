package com.ecommerce.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartItemModelTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid CartItem should pass validation")
    void validCartItemShouldPassValidation() {
        CartItem cartItem = new CartItem("user123", "prod456", 2);
        Set<ConstraintViolation<CartItem>> violations = validator.validate(cartItem);
        assertTrue(violations.isEmpty(), "Expected no violations for valid cart item");
    }

    @Test
    @DisplayName("CartItem with zero or negative quantity should fail validation")
    void cartItemNonPositiveQuantityShouldFail() {
        CartItem item1 = new CartItem("user123", "prod456", 0);
        CartItem item2 = new CartItem("user123", "prod456", -3);

        Set<ConstraintViolation<CartItem>> v1 = validator.validate(item1);
        Set<ConstraintViolation<CartItem>> v2 = validator.validate(item2);

        assertFalse(v1.isEmpty());
        assertFalse(v2.isEmpty());
        assertTrue(v1.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        assertTrue(v2.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    @DisplayName("CartItem with blank userId or productId should fail validation")
    void cartItemBlankIdsShouldFail() {
        CartItem item = new CartItem("", " ", 1);

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
    }

    @Test
    @DisplayName("CartItem properties, getters, setters, equals, and hashCode")
    void cartItemPropertiesAndEquals() {
        Instant now = Instant.now();
        CartItem c1 = new CartItem("c1", "user1", "prod1", 5, now);
        CartItem c2 = new CartItem("c1", "user1", "prod1", 5, now);

        assertEquals("c1", c1.getId());
        assertEquals("user1", c1.getUserId());
        assertEquals("prod1", c1.getProductId());
        assertEquals(5, c1.getQuantity());
        assertEquals(now, c1.getAddedAt());

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotNull(c1.toString());
    }
}

