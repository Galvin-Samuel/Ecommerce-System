package com.ecommerce.storage.kv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryKeyValueStoreTest {

    private InMemoryKeyValueStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new InMemoryKeyValueStore<>();
    }

    @Test
    @DisplayName("Should put and retrieve key-value entry successfully")
    void shouldStoreAndRetrieveValue() {
        store.put("session_123", "{ \"user\": \"alice\" }");

        Optional<String> value = store.get("session_123");
        assertTrue(value.isPresent());
        assertEquals("{ \"user\": \"alice\" }", value.get());
        assertTrue(store.containsKey("session_123"));
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should overwrite existing key with updated value")
    void shouldUpdateExistingKey() {
        store.put("theme", "light");
        assertEquals("light", store.get("theme").orElse(null));

        store.put("theme", "dark");
        assertEquals("dark", store.get("theme").orElse(null));
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should delete key and verify it no longer exists")
    void shouldDeleteKeySuccessfully() {
        store.put("temp_token", "xyz-789");
        assertTrue(store.containsKey("temp_token"));

        boolean deleted = store.delete("temp_token");
        assertTrue(deleted);
        assertFalse(store.containsKey("temp_token"));
        assertTrue(store.get("temp_token").isEmpty());

        // Deleting non-existent key returns false
        assertFalse(store.delete("temp_token"));
    }

    @Test
    @DisplayName("Should return empty optional for missing key")
    void shouldHandleMissingKey() {
        Optional<String> missing = store.get("non_existent_key");
        assertTrue(missing.isEmpty());
        assertFalse(store.containsKey("non_existent_key"));
    }

    @Test
    @DisplayName("Should reject null and blank keys and null values")
    void shouldValidateInputs() {
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "val"));
        assertThrows(IllegalArgumentException.class, () -> store.put("", "val"));
        assertThrows(IllegalArgumentException.class, () -> store.put("   ", "val"));
        assertThrows(IllegalArgumentException.class, () -> store.put("key", null));

        assertThrows(IllegalArgumentException.class, () -> store.get(null));
        assertThrows(IllegalArgumentException.class, () -> store.get(""));

        assertThrows(IllegalArgumentException.class, () -> store.put("key", "val", Duration.ofSeconds(-5)));
        assertThrows(IllegalArgumentException.class, () -> store.put("key", "val", Duration.ZERO));
    }

    @Test
    @DisplayName("Should expire entry after TTL and perform lazy on-access eviction")
    void shouldExpireEntryAfterTtl() throws InterruptedException {
        store.put("ephemeral_code", "998811", Duration.ofMillis(100));

        // Immediately available
        assertTrue(store.containsKey("ephemeral_code"));
        assertEquals("998811", store.get("ephemeral_code").orElse(null));

        // Wait for TTL to expire
        Thread.sleep(150);

        // Lazy eviction on get / containsKey
        assertFalse(store.containsKey("ephemeral_code"));
        assertTrue(store.get("ephemeral_code").isEmpty());
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("Should actively purge expired entries with cleanupExpired()")
    void shouldActivelyPurgeExpiredEntries() throws InterruptedException {
        store.put("k1", "v1", Duration.ofMillis(100));
        store.put("k2", "v2", Duration.ofMillis(100));
        store.put("persistent_k", "v_pers");

        Thread.sleep(150);

        long purged = store.cleanupExpired();
        assertEquals(2, purged);
        assertEquals(1, store.size());
        assertEquals("v_pers", store.get("persistent_k").orElse(null));
    }

    @Test
    @DisplayName("Should retrieve batch of keys with getAll()")
    void shouldRetrieveBatchOfKeys() {
        store.put("k1", "v1");
        store.put("k2", "v2");
        store.put("k3", "v3");

        Map<String, String> results = store.getAll(List.of("k1", "k3", "missing"));
        assertEquals(2, results.size());
        assertEquals("v1", results.get("k1"));
        assertEquals("v3", results.get("k3"));
        assertFalse(results.containsKey("missing"));
    }

    @Test
    @DisplayName("Should handle highly concurrent reads, writes, and updates safely")
    void shouldHandleConcurrentAccess() throws InterruptedException {
        int threads = 10;
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger writeSuccesses = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "shared_key_" + (i % 50);
                        String value = "val_" + threadId + "_" + i;
                        store.put(key, value);
                        if (store.get(key).isPresent()) {
                            writeSuccesses.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threads * operationsPerThread, writeSuccesses.get());
        assertTrue(store.size() <= 50);
    }
}
