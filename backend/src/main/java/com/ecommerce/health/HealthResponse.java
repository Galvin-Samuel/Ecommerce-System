package com.ecommerce.health;

/**
 * Data transfer object representing the health check response.
 *
 * @param status      Current operational status of the service (e.g., "UP")
 * @param application Name of the backend application
 */
public record HealthResponse(String status, String application) {
}

