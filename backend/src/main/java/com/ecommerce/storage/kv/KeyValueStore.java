package com.ecommerce.storage.kv;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Generic contract for key-value storage systems supporting basic CRUD,
 * key existence checks, batch queries, and optional TTL expiration.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface KeyValueStore<K, V> {

    /**
     * Stores a key-value pair without expiration.
     */
    void put(K key, V value);

    /**
     * Stores a key-value pair with a specified time-to-live.
     */
    void put(K key, V value, Duration ttl);

    /**
     * Retrieves the value associated with the specified key, returning empty
     * if the key does not exist or has expired.
     */
    Optional<V> get(K key);

    /**
     * Checks if a valid, unexpired key exists in the store.
     */
    boolean containsKey(K key);

    /**
     * Removes the key-value pair associated with the specified key.
     *
     * @return true if an entry was removed, false otherwise
     */
    boolean delete(K key);

    /**
     * Returns the number of unexpired entries currently stored.
     */
    int size();

    /**
     * Purges all entries from the store.
     */
    void clear();

    /**
     * Retrieves values for a batch of keys.
     */
    Map<K, V> getAll(Collection<K> keys);

    /**
     * Actively evicts all expired keys and returns the number of purged items.
     */
    long cleanupExpired();
}
