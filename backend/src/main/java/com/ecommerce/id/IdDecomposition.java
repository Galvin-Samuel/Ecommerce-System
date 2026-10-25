package com.ecommerce.id;

import java.time.Instant;

/**
 * Decomposed components of a 64-bit Snowflake identifier.
 */
public record IdDecomposition(
        long id,
        String idString,
        long timestampMs,
        Instant instant,
        long datacenterId,
        long workerId,
        long sequence
) {
}
