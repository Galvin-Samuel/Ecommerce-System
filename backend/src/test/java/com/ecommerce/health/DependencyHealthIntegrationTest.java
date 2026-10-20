package com.ecommerce.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests verifying live connectivity against actual MongoDB and Redis containers.
 *
 * Prerequisites:
 *   1. Docker Desktop daemon must be active.
 *   2. The containers must be started via Docker Compose:
 *      docker compose -f docker/docker-compose.yml up -d
 *   3. Run with environment variable RUN_INTEGRATION_TESTS=true:
 *      $env:RUN_INTEGRATION_TESTS="true"; mvn test -Dtest=DependencyHealthIntegrationTest
 */
@SpringBootTest
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class DependencyHealthIntegrationTest {

    @Autowired
    private DependencyHealthService dependencyHealthService;

    @Test
    @DisplayName("Verify live dependency connectivity when Docker Compose services are active")
    void testLiveDependencyHealth() {
        DependencyHealthResponse response = dependencyHealthService.checkDependencies();
        assertNotNull(response);
        assertEquals("UP", response.mongodb(), "MongoDB container should be reachable");
        assertEquals("UP", response.redis(), "Redis container should be reachable");
    }
}

