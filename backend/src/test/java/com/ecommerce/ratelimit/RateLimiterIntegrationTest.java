package com.ecommerce.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RateLimiterIntegrationTest {

    @Autowired
    private RedisRateLimiterService rateLimiterService;

    @Autowired
    private RateLimitingProperties properties;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String testCategory;

    @BeforeEach
    void setUp() {
        testCategory = "test-" + UUID.randomUUID().toString().substring(0, 8);
        properties.getRules().put(testCategory, new RateLimitingProperties.RuleConfig(5, 60));
    }

    @AfterEach
    void tearDown() {
        Set<String> keys = redisTemplate.keys("rate_limit:" + testCategory + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Should decrement remaining quota for sequential requests under limit")
    void shouldDecrementRemainingQuotaSequentially() {
        String clientId = "client-seq-" + UUID.randomUUID();

        RateLimitResult r1 = rateLimiterService.checkRateLimit(clientId, testCategory);
        assertTrue(r1.allowed());
        assertEquals(5L, r1.limit());
        assertEquals(4L, r1.remaining());

        RateLimitResult r2 = rateLimiterService.checkRateLimit(clientId, testCategory);
        assertTrue(r2.allowed());
        assertEquals(3L, r2.remaining());

        RateLimitResult r3 = rateLimiterService.checkRateLimit(clientId, testCategory);
        assertTrue(r3.allowed());
        assertEquals(2L, r3.remaining());
    }

    @Test
    @DisplayName("Should block requests and provide positive retry-after once limit is exceeded")
    void shouldBlockWhenLimitExceeded() {
        String clientId = "client-burst-" + UUID.randomUUID();

        // Exhaust limit of 5
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimiterService.checkRateLimit(clientId, testCategory);
            assertTrue(result.allowed(), "Request " + (i + 1) + " should be allowed");
        }

        // 6th request must be rejected
        RateLimitResult rejected = rateLimiterService.checkRateLimit(clientId, testCategory);
        assertFalse(rejected.allowed(), "6th request must be rejected");
        assertEquals(0L, rejected.remaining());
        assertTrue(rejected.retryAfterSeconds() > 0, "Retry-after must be greater than zero");
    }

    @Test
    @DisplayName("Should maintain strict rate-limit isolation between different clients")
    void shouldIsolateDifferentClients() {
        String clientA = "client-a-" + UUID.randomUUID();
        String clientB = "client-b-" + UUID.randomUUID();

        // Exhaust Client A quota
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.checkRateLimit(clientA, testCategory).allowed());
        }
        assertFalse(rateLimiterService.checkRateLimit(clientA, testCategory).allowed());

        // Client B must still have full quota
        RateLimitResult resultB = rateLimiterService.checkRateLimit(clientB, testCategory);
        assertTrue(resultB.allowed());
        assertEquals(4L, resultB.remaining());
    }

    @Test
    @DisplayName("Should atomically enforce limit with zero race conditions under concurrent requests")
    void shouldEnforceLimitUnderConcurrentAccess() throws InterruptedException {
        String clientId = "client-concurrent-" + UUID.randomUUID();
        int totalRequests = 25;
        int maxAllowed = 5;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RateLimitResult res = rateLimiterService.checkRateLimit(clientId, testCategory);
                    if (res.allowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(maxAllowed, allowedCount.get(), "Exactly " + maxAllowed + " requests must be permitted");
        assertEquals(totalRequests - maxAllowed, rejectedCount.get(), "Remaining requests must be rejected");
    }

    @Test
    @DisplayName("Should automatically refill quota after sliding window expires")
    void shouldRefillQuotaAfterWindowExpires() throws InterruptedException {
        String shortWindowCat = "short-win-" + UUID.randomUUID().toString().substring(0, 6);
        // 2 requests per 1 second
        properties.getRules().put(shortWindowCat, new RateLimitingProperties.RuleConfig(2, 1));
        String clientId = "client-expiring-" + UUID.randomUUID();

        assertTrue(rateLimiterService.checkRateLimit(clientId, shortWindowCat).allowed());
        assertTrue(rateLimiterService.checkRateLimit(clientId, shortWindowCat).allowed());
        assertFalse(rateLimiterService.checkRateLimit(clientId, shortWindowCat).allowed());

        // Wait for 1.2 seconds for window to slide past
        Thread.sleep(1200);

        RateLimitResult refilled = rateLimiterService.checkRateLimit(clientId, shortWindowCat);
        assertTrue(refilled.allowed(), "Request after window expiry must be allowed");
    }
}
