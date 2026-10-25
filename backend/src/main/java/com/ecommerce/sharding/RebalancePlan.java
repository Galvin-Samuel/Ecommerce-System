package com.ecommerce.sharding;

import java.util.Collections;
import java.util.Map;

/**
 * Result of a shard rebalancing simulation showing migration impact.
 */
public record RebalancePlan(
        String action,
        String targetShardId,
        int totalKeysAnalyzed,
        int keysToMigrate,
        int keysUnchanged,
        double migrationPercentage,
        Map<String, MigrationDetail> itemMigrations
) {
    public record MigrationDetail(
            String partitionKey,
            String sourceShardId,
            String destinationShardId
    ) {
    }

    public static RebalancePlan of(
            String action,
            String targetShardId,
            int totalKeys,
            Map<String, MigrationDetail> migrations
    ) {
        int toMigrate = migrations.size();
        int unchanged = totalKeys - toMigrate;
        double percentage = totalKeys > 0 ? ((double) toMigrate / totalKeys) * 100.0 : 0.0;
        return new RebalancePlan(
                action,
                targetShardId,
                totalKeys,
                toMigrate,
                unchanged,
                Math.round(percentage * 10.0) / 10.0,
                Collections.unmodifiableMap(migrations)
        );
    }
}
