package com.ecommerce.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service simulating and planning shard rebalancing operations when
 * topology scales up or down.
 */
@Service
public class ShardRebalanceService {

    private static final Logger log = LoggerFactory.getLogger(ShardRebalanceService.class);

    private final ShardRouter shardRouter;

    public ShardRebalanceService(ShardRouter shardRouter) {
        this.shardRouter = shardRouter;
    }

    /**
     * Simulates adding a new logical shard and computes precisely which keys
     * would be migrated to the new shard without changing current routing.
     */
    public RebalancePlan simulateAddShard(LogicalShard newShard, List<String> sampleKeys) {
        if (newShard == null || sampleKeys == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        // Record current routing
        Map<String, String> currentAssignments = new HashMap<>();
        for (String key : sampleKeys) {
            currentAssignments.put(key, shardRouter.routeKey(key).getShardId());
        }

        // Temporarily register shard
        shardRouter.registerShard(newShard);

        Map<String, RebalancePlan.MigrationDetail> migrations = new HashMap<>();
        try {
            for (String key : sampleKeys) {
                String newShardId = shardRouter.routeKey(key).getShardId();
                String oldShardId = currentAssignments.get(key);

                if (!oldShardId.equals(newShardId)) {
                    migrations.put(key, new RebalancePlan.MigrationDetail(key, oldShardId, newShardId));
                }
            }
        } finally {
            // Revert temporary registration to keep simulation non-destructive
            shardRouter.deregisterShard(newShard.getShardId());
        }

        log.info("Simulated adding shard '{}' across {} keys: {} keys migrated ({:.1f}%)",
                newShard.getShardId(), sampleKeys.size(), migrations.size(),
                sampleKeys.isEmpty() ? 0 : (migrations.size() * 100.0 / sampleKeys.size()));

        return RebalancePlan.of("ADD_SHARD", newShard.getShardId(), sampleKeys.size(), migrations);
    }

    /**
     * Simulates removing a shard and determines where its keys would be relocated.
     */
    public RebalancePlan simulateRemoveShard(String shardIdToRemove, List<String> sampleKeys) {
        LogicalShard target = shardRouter.getShard(shardIdToRemove);
        if (target == null) {
            throw new IllegalArgumentException("Shard '" + shardIdToRemove + "' is not registered");
        }

        Map<String, String> currentAssignments = new HashMap<>();
        for (String key : sampleKeys) {
            currentAssignments.put(key, shardRouter.routeKey(key).getShardId());
        }

        // Temporarily deregister shard
        shardRouter.deregisterShard(shardIdToRemove);

        Map<String, RebalancePlan.MigrationDetail> migrations = new HashMap<>();
        try {
            for (String key : sampleKeys) {
                String oldShardId = currentAssignments.get(key);
                if (shardIdToRemove.equals(oldShardId)) {
                    String destinationShardId = shardRouter.routeKey(key).getShardId();
                    migrations.put(key, new RebalancePlan.MigrationDetail(key, oldShardId, destinationShardId));
                }
            }
        } finally {
            // Re-register shard to preserve original state
            shardRouter.registerShard(target);
        }

        log.info("Simulated removing shard '{}' across {} keys: {} keys relocated",
                shardIdToRemove, sampleKeys.size(), migrations.size());

        return RebalancePlan.of("REMOVE_SHARD", shardIdToRemove, sampleKeys.size(), migrations);
    }
}
