package com.ecommerce.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardRouterTest {

    private ShardRouter router;

    @BeforeEach
    void setUp() {
        router = new ShardRouter(150);
    }

    @Test
    @DisplayName("Should route identical partition key deterministically to the same logical shard")
    void shouldRouteDeterministically() {
        String userId = "usr_acc_884192";
        LogicalShard initial = router.routeKey(userId);
        assertNotNull(initial);

        for (int i = 0; i < 200; i++) {
            assertEquals(initial, router.routeKey(userId));
        }
    }

    @Test
    @DisplayName("Should distribute distinct partition keys across multiple logical shards")
    void shouldDistributeKeysAcrossShards() {
        Set<String> hitShards = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            LogicalShard shard = router.routeKey("user_key_" + i);
            hitShards.add(shard.getShardId());
        }

        // All 3 default shards (shard-0, shard-1, shard-2) must receive traffic
        assertEquals(3, hitShards.size());
        assertTrue(hitShards.contains("shard-0"));
        assertTrue(hitShards.contains("shard-1"));
        assertTrue(hitShards.contains("shard-2"));
    }

    @Test
    @DisplayName("Should throw ShardUnavailableException when target shard is OFFLINE")
    void shouldThrowWhenShardOffline() {
        LogicalShard shard0 = router.getShard("shard-0");
        shard0.setStatus(LogicalShard.Status.OFFLINE);

        // Find a key that maps to shard-0
        String keyForShard0 = null;
        for (int i = 0; i < 1000; i++) {
            String candidate = "test_key_" + i;
            try {
                if ("shard-0".equals(router.routeKey(candidate).getShardId())) {
                    // unexpected since it's offline
                }
            } catch (ShardUnavailableException e) {
                keyForShard0 = candidate;
                assertEquals("shard-0", e.getShardId());
                break;
            }
        }
        assertNotNull(keyForShard0, "Should have encountered offline shard-0");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no shards are registered")
    void shouldThrowWhenRingIsEmpty() {
        router.deregisterShard("shard-0");
        router.deregisterShard("shard-1");
        router.deregisterShard("shard-2");

        assertEquals(0, router.getShardCount());
        assertThrows(IllegalStateException.class, () -> router.routeKey("any_key"));
    }

    @Test
    @DisplayName("Should reject null or blank partition keys")
    void shouldRejectInvalidPartitionKeys() {
        assertThrows(IllegalArgumentException.class, () -> router.routeKey(null));
        assertThrows(IllegalArgumentException.class, () -> router.routeKey(""));
        assertThrows(IllegalArgumentException.class, () -> router.routeKey("   "));
    }

    @Test
    @DisplayName("Should return preference list with replicas for failover and high availability")
    void shouldRouteKeyWithReplicas() {
        List<LogicalShard> replicas = router.routeKeyWithReplicas("order_99812", 2);
        assertEquals(2, replicas.size());
        // Verify distinct shards in preference list
        assertEquals(2, replicas.stream().distinct().count());
    }
}
