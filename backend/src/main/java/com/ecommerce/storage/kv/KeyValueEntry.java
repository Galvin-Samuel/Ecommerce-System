package com.ecommerce.storage.kv;

import java.time.Instant;

/**
 * Value container wrapping a stored object with creation and expiration metadata.
 *
 * @param <V> Stored value type
 */
public record KeyValueEntry<V>(
        V value,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public static <V> KeyValueEntry<V> persistent(V value) {
        return new KeyValueEntry<>(value, Instant.now(), null);
    }

    public static <V> KeyValueEntry<V> withTtl(V value, Instant expiresAt) {
        return new KeyValueEntry<>(value, Instant.now(), expiresAt);
    }
}
