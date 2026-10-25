package com.ecommerce.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitingProperties properties;
    private RedisRateLimiterService service;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        service = new RedisRateLimiterService(redisTemplate, properties);
    }

    @Test
    @DisplayName("Should allow request when Lua script returns allowed=1")
    void shouldAllowRequestWhenUnderLimit() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(1L, 9L, 0L));

        RateLimitResult result = service.checkRateLimit("user:101", "auth");

        assertTrue(result.allowed());
        assertEquals(10L, result.limit());
        assertEquals(9L, result.remaining());
        assertEquals(0L, result.retryAfterSeconds());
        assertFalse(result.degraded());
    }

    @Test
    @DisplayName("Should reject request when Lua script returns allowed=0 with retry-after")
    void shouldRejectRequestWhenLimitExceeded() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(0L, 0L, 35L));

        RateLimitResult result = service.checkRateLimit("user:101", "auth");

        assertFalse(result.allowed());
        assertEquals(10L, result.limit());
        assertEquals(0L, result.remaining());
        assertEquals(35L, result.retryAfterSeconds());
        assertFalse(result.degraded());
    }

    @Test
    @DisplayName("Should bypass Redis when rate limiting is disabled")
    void shouldBypassWhenDisabled() {
        properties.setEnabled(false);

        RateLimitResult result = service.checkRateLimit("user:101", "auth");

        assertTrue(result.allowed());
        assertFalse(result.degraded());
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should degrade gracefully with FAIL_OPEN when Redis is unreachable")
    void shouldFailOpenWhenRedisThrowsException() {
        properties.setFallbackPolicy(RateLimitingProperties.FallbackPolicy.FAIL_OPEN);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        RateLimitResult result = service.checkRateLimit("user:101", "auth");

        assertTrue(result.allowed());
        assertTrue(result.degraded());
        assertEquals("redis-unavailable", result.fallbackReason());
    }

    @Test
    @DisplayName("Should block request with FAIL_CLOSED when Redis is unreachable")
    void shouldFailClosedWhenRedisThrowsException() {
        properties.setFallbackPolicy(RateLimitingProperties.FallbackPolicy.FAIL_CLOSED);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        RateLimitResult result = service.checkRateLimit("user:101", "auth");

        assertFalse(result.allowed());
        assertTrue(result.degraded());
        assertTrue(result.retryAfterSeconds() > 0);
        assertEquals("redis-unavailable", result.fallbackReason());
    }
}
