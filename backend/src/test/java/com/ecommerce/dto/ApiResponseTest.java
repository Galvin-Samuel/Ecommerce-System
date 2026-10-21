package com.ecommerce.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("ApiResponse.success(data) should set success true and default message")
    void testSuccessWithData() {
        ApiResponse<String> response = ApiResponse.success("test-payload");

        assertTrue(response.isSuccess());
        assertEquals("Operation successful", response.getMessage());
        assertEquals("test-payload", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("ApiResponse.success(message, data) should set custom message and data")
    void testSuccessWithMessageAndData() {
        ApiResponse<Integer> response = ApiResponse.success("Custom success", 42);

        assertTrue(response.isSuccess());
        assertEquals("Custom success", response.getMessage());
        assertEquals(42, response.getData());
    }

    @Test
    @DisplayName("ApiResponse.error(message) should set success false and null data")
    void testErrorWithMessage() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertEquals(null, response.getData());
    }

    @Test
    @DisplayName("ApiResponse should serialize properly to JSON with ISO-8601 timestamp")
    void testJsonSerialization() throws Exception {
        Instant fixedTime = Instant.parse("2026-10-21T10:00:00.000Z");
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                true,
                "Fetched user",
                Map.of("id", "123", "name", "Alice"),
                fixedTime
        );

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"Fetched user\""));
        assertTrue(json.contains("\"data\":{"));
        assertTrue(json.contains("\"timestamp\":\"2026-10-21T10:00:00.000Z\""));
    }
}

