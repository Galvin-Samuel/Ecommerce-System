package com.ecommerce.health;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for verifying the connectivity and operational health
 * of external infrastructure dependencies (MongoDB and Redis).
 */
@Service
public class DependencyHealthService {

    private static final Logger log = LoggerFactory.getLogger(DependencyHealthService.class);

    private final MongoTemplate mongoTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String applicationName;

    public DependencyHealthService(
            MongoTemplate mongoTemplate,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${spring.application.name:ecommerce-backend}") String applicationName) {
        this.mongoTemplate = mongoTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.applicationName = applicationName;
    }

    /**
     * Inspects connectivity to all external dependencies and aggregates their health statuses.
     *
     * @return Aggregate health response with individual dependency statuses
     */
    public DependencyHealthResponse checkDependencies() {
        String mongoStatus = checkMongoHealth();
        String redisStatus = checkRedisHealth();
        return new DependencyHealthResponse(applicationName, mongoStatus, redisStatus);
    }

    /**
     * Executes a lightweight ping command against MongoDB.
     *
     * @return "UP" if ping command succeeds with ok=1.0, otherwise "DOWN"
     */
    public String checkMongoHealth() {
        try {
            if (mongoTemplate == null) {
                return "DOWN";
            }
            Document pingCommand = new Document("ping", 1);
            Document result = mongoTemplate.executeCommand(pingCommand);
            Object okValue = result.get("ok");
            if (okValue instanceof Number number && number.doubleValue() == 1.0) {
                return "UP";
            }
            log.warn("MongoDB ping command did not return ok=1. Result: {}", result);
            return "DOWN";
        } catch (Exception ex) {
            log.warn("MongoDB connectivity check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }

    /**
     * Executes a lightweight ping command against Redis.
     *
     * @return "UP" if Redis responds with PONG, otherwise "DOWN"
     */
    public String checkRedisHealth() {
        try {
            if (redisConnectionFactory == null) {
                return "DOWN";
            }
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                String pingResult = connection.ping();
                if ("PONG".equalsIgnoreCase(pingResult)) {
                    return "UP";
                }
                log.warn("Redis ping did not return PONG. Result: {}", pingResult);
                return "DOWN";
            }
        } catch (Exception ex) {
            log.warn("Redis connectivity check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}

