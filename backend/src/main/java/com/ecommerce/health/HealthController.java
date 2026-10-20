package com.ecommerce.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller to verify backend service availability.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;

    public HealthController(
            @Value("${spring.application.name:ecommerce-backend}") String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Endpoint to check the application health status.
     *
     * @return HTTP 200 OK with operational status and application name
     */
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(new HealthResponse("UP", applicationName));
    }
}

