package com.ecommerce.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Distributed sliding-window rate limiter powered by Redis and atomic Lua scripts.
 */
@Service
public class RedisRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);

    private static final String SLIDING_WINDOW_LUA = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowMs = tonumber(ARGV[2])
            local maxRequests = tonumber(ARGV[3])
            local memberId = ARGV[4]

            local clearBefore = now - windowMs
            redis.call('ZREMRANGEBYSCORE', key, '-inf', clearBefore)

            local currentCount = redis.call('ZCARD', key)

            if currentCount < maxRequests then
                redis.call('ZADD', key, now, memberId)
                redis.call('PEXPIRE', key, windowMs)
                return {1, maxRequests - currentCount - 1, 0}
            else
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retryAfterSec = 1
                if oldest and #oldest >= 2 then
                    local oldestScore = tonumber(oldest[2])
                    local waitMs = (oldestScore + windowMs) - now
                    if waitMs > 0 then
                        retryAfterSec = math.ceil(waitMs / 1000)
                    end
                end
                return {0, 0, retryAfterSec}
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final RateLimitingProperties properties;
    private final DefaultRedisScript<List> rateLimitScript;

    public RedisRateLimiterService(StringRedisTemplate redisTemplate, RateLimitingProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.rateLimitScript = new DefaultRedisScript<>(SLIDING_WINDOW_LUA, List.class);
    }

    public RateLimitResult checkRateLimit(String clientId, String category) {
        RateLimitingProperties.RuleConfig rule = properties.getRuleForCategory(category);
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed(rule.getLimit(), rule.getLimit());
        }

        String key = "rate_limit:" + category + ":" + clientId;
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = rule.getWindowSeconds() * 1000L;
        long maxRequests = rule.getLimit();
        String memberId = nowMs + ":" + UUID.randomUUID();

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(nowMs),
                    String.valueOf(windowMs),
                    String.valueOf(maxRequests),
                    memberId
            );

            if (result != null && result.size() >= 3) {
                boolean allowed = result.get(0) == 1L;
                long remaining = result.get(1);
                long retryAfter = result.get(2);

                if (allowed) {
                    return RateLimitResult.allowed(maxRequests, remaining);
                } else {
                    return RateLimitResult.rejected(maxRequests, retryAfter);
                }
            }

            log.warn("Unexpected Redis Lua script result for key '{}'. Falling back to safe policy.", key);
            return handleFallback(rule);

        } catch (Exception ex) {
            log.warn("Redis rate-limiting error for key '{}': {}. Invoking fallback policy.", key, ex.getMessage());
            return handleFallback(rule);
        }
    }

    private RateLimitResult handleFallback(RateLimitingProperties.RuleConfig rule) {
        if (properties.getFallbackPolicy() == RateLimitingProperties.FallbackPolicy.FAIL_CLOSED) {
            log.error("Rate limiter configured with FAIL_CLOSED: rejecting request due to Redis unavailability.");
            return RateLimitResult.degradedReject(rule.getLimit(), rule.getWindowSeconds(), "redis-unavailable");
        } else {
            log.warn("Rate limiter configured with FAIL_OPEN: permitting request with degraded header.");
            return RateLimitResult.degradedAllow(rule.getLimit(), "redis-unavailable");
        }
    }
}
