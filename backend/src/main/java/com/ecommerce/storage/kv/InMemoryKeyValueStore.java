package com.ecommerce.storage.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrency-safe, in-memory implementation of KeyValueStore.
 *
 * <p><b>Architecture & Consistency Guarantees:</b></p>
 * <ul>
 *   <li><b>Storage Engine:</b> Non-blocking {@link ConcurrentHashMap} providing linearizable,
 *       thread-safe read/write operations per key.</li>
 *   <li><b>Expiration Model:</b> Dual eviction strategy: lazy on-access eviction combined
 *       with on-demand active purging via {@link #cleanupExpired()}.</li>
 *   <li><b>Persistence Guarantee:</b> Strictly in-memory and volatile. Data is retained solely
 *       within the JVM heap and will be lost upon process restart. This component is completely
 *       isolated from persistent MongoDB collections (users, products, carts, orders).</li>
 * </ul>
 *
 * @param <K> Key type
 * @param <V> Value type
 */
@Component
public class InMemoryKeyValueStore<K, V> implements KeyValueStore<K, V> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKeyValueStore.class);

    private final ConcurrentHashMap<K, KeyValueEntry<V>> storage = new ConcurrentHashMap<>();

    @Override
    public void put(K key, V value) {
        validateKeyValue(key, value);
        storage.put(key, KeyValueEntry.persistent(value));
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        validateKeyValue(key, value);
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL duration must be strictly positive");
        }
        Instant expiresAt = Instant.now().plus(ttl);
        storage.put(key, KeyValueEntry.withTtl(value, expiresAt));
    }

    @Override
    public Optional<V> get(K key) {
        validateKey(key);
        KeyValueEntry<V> entry = storage.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (entry.isExpired(now)) {
            // Lazy eviction: remove expired entry
            storage.remove(key, entry);
            return Optional.empty();
        }

        return Optional.of(entry.value());
    }

    @Override
    public boolean containsKey(K key) {
        validateKey(key);
        KeyValueEntry<V> entry = storage.get(key);
        if (entry == null) {
            return false;
        }

        Instant now = Instant.now();
        if (entry.isExpired(now)) {
            storage.remove(key, entry);
            return false;
        }

        return true;
    }

    @Override
    public boolean delete(K key) {
        validateKey(key);
        return storage.remove(key) != null;
    }

    @Override
    public int size() {
        cleanupExpired();
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, V> results = new HashMap<>();
        for (K key : keys) {
            get(key).ifPresent(value -> results.put(key, value));
        }
        return results;
    }

    @Override
    public long cleanupExpired() {
        Instant now = Instant.now();
        long purgedCount = 0;
        for (Map.Entry<K, KeyValueEntry<V>> entry : storage.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                if (storage.remove(entry.getKey(), entry.getValue())) {
                    purgedCount++;
                }
            }
        }
        if (purgedCount > 0) {
            log.debug("Purged {} expired entries from InMemoryKeyValueStore", purgedCount);
        }
        return purgedCount;
    }

    private void validateKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Store key cannot be null");
        }
        if (key instanceof String str && str.isBlank()) {
            throw new IllegalArgumentException("Store key cannot be blank");
        }
    }

    private void validateKeyValue(K key, V value) {
        validateKey(key);
        if (value == null) {
            throw new IllegalArgumentException("Store value cannot be null");
        }
    }
}
