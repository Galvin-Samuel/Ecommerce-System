package com.ecommerce.health;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Data transfer object representing the operational status of external dependencies.
 *
 * @param application Name of the backend application
 * @param mongodb     Operational status of MongoDB ("UP" or "DOWN")
 * @param redis       Operational status of Redis ("UP" or "DOWN")
 */
public record DependencyHealthResponse(
        String application,
        String mongodb,
        String redis
) {
    /**
     * Checks if all dependencies are currently UP.
     * Ignored by Jackson so only application, mongodb, and redis appear in JSON.
     *
     * @return true if both MongoDB and Redis are UP, false otherwise
     */
    @JsonIgnore
    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(mongodb) && "UP".equalsIgnoreCase(redis);
    }
}

