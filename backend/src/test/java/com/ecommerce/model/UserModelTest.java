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

class UserModelTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid User should pass validation")
    void validUserShouldPassValidation() {
        User user = new User("Alice Doe", "alice@example.com", "$2a$12$hashedpwd", UserRole.CUSTOMER);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Expected no validation violations for valid user");
    }

    @Test
    @DisplayName("User with blank name, invalid email, blank password and null role should fail validation")
    void invalidUserShouldProduceViolations() {
        User user = new User();
        user.setName("");
        user.setEmail("not-a-valid-email");
        user.setPasswordHash(" ");
        user.setRole(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("passwordHash")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("role")));
    }

    @Test
    @DisplayName("UserRole enum should contain expected roles")
    void userRoleEnumValues() {
        assertEquals(3, UserRole.values().length);
        assertEquals(UserRole.CUSTOMER, UserRole.valueOf("CUSTOMER"));
        assertEquals(UserRole.SELLER, UserRole.valueOf("SELLER"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    @DisplayName("User getters, setters, equals, and hashCode should behave correctly")
    void userPropertiesAndEquals() {
        Instant now = Instant.now();
        User u1 = new User("id1", "Bob", "bob@example.com", "hash1", UserRole.SELLER, now, now);
        User u2 = new User("id1", "Bob", "bob@example.com", "hash1", UserRole.SELLER, now, now);

        assertEquals("id1", u1.getId());
        assertEquals("Bob", u1.getName());
        assertEquals("bob@example.com", u1.getEmail());
        assertEquals("hash1", u1.getPasswordHash());
        assertEquals(UserRole.SELLER, u1.getRole());
        assertEquals(now, u1.getCreatedAt());
        assertEquals(now, u1.getUpdatedAt());

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertNotNull(u1.toString());
    }
}

