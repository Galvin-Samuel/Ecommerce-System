package com.ecommerce.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardRebalanceIntegrationTest {

    private ShardRouter router;
    private ShardRebalanceService rebalanceService;

    @BeforeEach
    void setUp() {
        router = new ShardRouter(150);
        rebalanceService = new ShardRebalanceService(router);
    }

    @Test
    @DisplayName("Should simulate adding a 4th shard and prove that only ~25% of keys migrate directly to the new shard")
    void shouldSimulateAddingShardWithLimitedRemapping() {
        int keyCount = 2000;
        List<String> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add("customer_account_id_" + i);
        }

        LogicalShard newShard = new LogicalShard("shard-3");
        RebalancePlan plan = rebalanceService.simulateAddShard(newShard, keys);

        assertNotNull(plan);
        assertEquals("ADD_SHARD", plan.action());
        assertEquals("shard-3", plan.targetShardId());
        assertEquals(keyCount, plan.totalKeysAnalyzed());

        // Moving from 3 shards to 4 shards: theoretical optimal migration is 1/4 = 25% (500 keys).
        // Consistent hashing guarantees between 18% and 32% keys migrate.
        assertTrue(plan.migrationPercentage() >= 18.0 && plan.migrationPercentage() <= 32.0,
                "Expected migration near 25%, but got: " + plan.migrationPercentage() + "%");

        // Every single migrated key MUST have destination == shard-3
        for (RebalancePlan.MigrationDetail detail : plan.itemMigrations().values()) {
            assertEquals("shard-3", detail.destinationShardId(), "All migrated keys must target the new shard");
            assertFalse("shard-3".equals(detail.sourceShardId()), "Source cannot be the new shard");
        }

        // Verify that original router topology was safely restored and untouched
        assertEquals(3, router.getShardCount());
        assertFalse(router.getRegisteredShards().contains(newShard));
    }

    @Test
    @DisplayName("Should simulate removing a shard and reassign only its keys clockwise")
    void shouldSimulateRemovingShard() {
        int keyCount = 1000;
        List<String> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add("order_record_" + i);
        }

        RebalancePlan plan = rebalanceService.simulateRemoveShard("shard-1", keys);

        assertNotNull(plan);
        assertEquals("REMOVE_SHARD", plan.action());
        assertEquals("shard-1", plan.targetShardId());

        // Only keys previously assigned to shard-1 should migrate
        for (RebalancePlan.MigrationDetail detail : plan.itemMigrations().values()) {
            assertEquals("shard-1", detail.sourceShardId(), "Only keys from shard-1 should migrate");
            assertTrue("shard-0".equals(detail.destinationShardId()) || "shard-2".equals(detail.destinationShardId()));
        }

        // Verify that shard-1 was preserved in router after simulation
        assertEquals(3, router.getShardCount());
        assertNotNull(router.getShard("shard-1"));
    }
}
