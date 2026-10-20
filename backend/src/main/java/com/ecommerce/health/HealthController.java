package com.ecommerce.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller providing endpoints to verify backend application status
 * and external infrastructure dependency connectivity (MongoDB & Redis).
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final DependencyHealthService dependencyHealthService;

    public HealthController(
            @Value("${spring.application.name:ecommerce-backend}") String applicationName,
            DependencyHealthService dependencyHealthService) {
        this.applicationName = applicationName;
        this.dependencyHealthService = dependencyHealthService;
    }

    /**
     * Liveness endpoint to check if the application process is running.
     * Independent of external database or cache connectivity.
     *
     * @return HTTP 200 OK with operational status and application name
     */
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(new HealthResponse("UP", applicationName));
    }

    /**
     * Readiness endpoint to verify external database and cache connectivity.
     * Returns individual status for MongoDB and Redis.
     *
     * @return HTTP 200 OK if all dependencies are UP, or HTTP 503 SERVICE_UNAVAILABLE
     *         if any dependency is DOWN, with the diagnostic JSON payload.
     */
    @GetMapping("/dependencies")
    public ResponseEntity<DependencyHealthResponse> checkDependencyHealth() {
        DependencyHealthResponse response = dependencyHealthService.checkDependencies();
        HttpStatus status = response.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
