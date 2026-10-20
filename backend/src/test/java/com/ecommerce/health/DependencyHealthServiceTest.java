package com.ecommerce.health;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying the operational health checks and graceful degradation logic
 * inside DependencyHealthService without requiring external servers.
 */
@ExtendWith(MockitoExtension.class)
class DependencyHealthServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private DependencyHealthService dependencyHealthService;

    @BeforeEach
    void setUp() {
        dependencyHealthService = new DependencyHealthService(
                mongoTemplate,
                redisConnectionFactory,
                "ecommerce-backend"
        );
    }

    @Test
    @DisplayName("Should report all UP when MongoDB returns ok=1 and Redis responds PONG")
    void shouldReportAllUpWhenDependenciesHealthy() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        DependencyHealthResponse response = dependencyHealthService.checkDependencies();

        assertEquals("ecommerce-backend", response.application());
        assertEquals("UP", response.mongodb());
        assertEquals("UP", response.redis());
        assertTrue(response.isHealthy());
    }

    @Test
    @DisplayName("Should report MongoDB DOWN gracefully when MongoDB ping throws an exception")
    void shouldReportMongoDownOnException() {
        when(mongoTemplate.executeCommand(any(Document.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        DependencyHealthResponse response = dependencyHealthService.checkDependencies();

        assertEquals("DOWN", response.mongodb());
        assertEquals("UP", response.redis());
        assertFalse(response.isHealthy());
    }

    @Test
    @DisplayName("Should report Redis DOWN gracefully when Redis ping throws an exception")
    void shouldReportRedisDownOnException() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        DependencyHealthResponse response = dependencyHealthService.checkDependencies();

        assertEquals("UP", response.mongodb());
        assertEquals("DOWN", response.redis());
        assertFalse(response.isHealthy());
    }

    @Test
    @DisplayName("Should handle null MongoTemplate or RedisConnectionFactory gracefully")
    void shouldHandleNullDependenciesGracefully() {
        DependencyHealthService serviceWithNulls = new DependencyHealthService(null, null, "ecommerce-backend");

        DependencyHealthResponse response = serviceWithNulls.checkDependencies();

        assertEquals("DOWN", response.mongodb());
        assertEquals("DOWN", response.redis());
        assertFalse(response.isHealthy());
    }
}

