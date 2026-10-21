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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductModelTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid Product should pass validation")
    void validProductShouldPassValidation() {
        Product product = new Product(
                "Wireless Headphones",
                "Noise-cancelling over-ear headphones",
                "Electronics",
                new BigDecimal("99.99"),
                50,
                "https://example.com/headphones.jpg",
                "seller123"
        );

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty(), "Expected no violations for valid product");
    }

    @Test
    @DisplayName("Product with negative price or negative stock should fail validation")
    void productWithNegativeValuesShouldFail() {
        Product product = new Product(
                "Wireless Headphones",
                "Description",
                "Electronics",
                new BigDecimal("-5.00"),
                -1,
                "https://example.com/img.jpg",
                "seller123"
        );

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("stockQuantity")));
    }

    @Test
    @DisplayName("Product with blank mandatory fields should fail validation")
    void productWithBlankMandatoryFieldsShouldFail() {
        Product product = new Product();
        product.setName("");
        product.setCategory(" ");
        product.setSellerId("");

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sellerId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("stockQuantity")));
    }

    @Test
    @DisplayName("Product properties, getters, setters, equals, and hashCode")
    void productPropertiesAndEquals() {
        Instant now = Instant.now();
        Product p1 = new Product("prod1", "Mouse", "Optical mouse", "Electronics",
                new BigDecimal("19.99"), 100, "url", "seller1", now, now);
        Product p2 = new Product("prod1", "Mouse", "Optical mouse", "Electronics",
                new BigDecimal("19.99"), 100, "url", "seller1", now, now);

        assertEquals("prod1", p1.getId());
        assertEquals("Mouse", p1.getName());
        assertEquals("Optical mouse", p1.getDescription());
        assertEquals("Electronics", p1.getCategory());
        assertEquals(new BigDecimal("19.99"), p1.getPrice());
        assertEquals(100, p1.getStockQuantity());
        assertEquals("url", p1.getImageUrl());
        assertEquals("seller1", p1.getSellerId());
        assertEquals(now, p1.getCreatedAt());
        assertEquals(now, p1.getUpdatedAt());

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotNull(p1.toString());
    }
}

