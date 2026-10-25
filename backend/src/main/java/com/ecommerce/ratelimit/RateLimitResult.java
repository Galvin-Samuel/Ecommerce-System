package com.ecommerce.ratelimit;

/**
 * Result of evaluating rate limit quota against Redis.
 */
public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        boolean degraded,
        String fallbackReason
) {
    public static RateLimitResult allowed(long limit, long remaining) {
        return new RateLimitResult(true, limit, remaining, 0, false, null);
    }

    public static RateLimitResult rejected(long limit, long retryAfterSeconds) {
        return new RateLimitResult(false, limit, 0, Math.max(1, retryAfterSeconds), false, null);
    }

    public static RateLimitResult degradedAllow(long limit, String reason) {
        return new RateLimitResult(true, limit, limit, 0, true, reason);
    }

    public static RateLimitResult degradedReject(long limit, long retryAfterSeconds, String reason) {
        return new RateLimitResult(false, limit, 0, Math.max(1, retryAfterSeconds), true, reason);
    }
}
