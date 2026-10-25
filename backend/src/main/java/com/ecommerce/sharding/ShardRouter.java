package com.ecommerce.sharding;

import com.ecommerce.hashing.ConsistentHashRing;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes data partition keys to logical database shards using consistent hashing.
 */
@Component
public class ShardRouter {

    private final ConsistentHashRing<LogicalShard> hashRing;
    private final Map<String, LogicalShard> registeredShards = new ConcurrentHashMap<>();

    public ShardRouter() {
        this(150);
    }

    public ShardRouter(int virtualNodes) {
        this.hashRing = new ConsistentHashRing<>(virtualNodes);
        // Default cluster topology with 3 logical shards
        registerShard(new LogicalShard("shard-0"));
        registerShard(new LogicalShard("shard-1"));
        registerShard(new LogicalShard("shard-2"));
    }

    public synchronized void registerShard(LogicalShard shard) {
        if (shard == null) {
            throw new IllegalArgumentException("Shard cannot be null");
        }
        registeredShards.put(shard.getShardId(), shard);
        hashRing.addNode(shard);
    }

    public synchronized boolean deregisterShard(String shardId) {
        if (shardId == null) {
            return false;
        }
        LogicalShard shard = registeredShards.remove(shardId);
        if (shard != null) {
            return hashRing.removeNode(shard);
        }
        return false;
    }

    /**
     * Determines the primary target shard for the specified partition key.
     *
     * @param partitionKey User ID, Order ID, or Product ID
     * @return Assigned LogicalShard
     * @throws ShardUnavailableException if assigned shard is offline
     */
    public LogicalShard routeKey(String partitionKey) {
        if (partitionKey == null || partitionKey.isBlank()) {
            throw new IllegalArgumentException("Partition key cannot be null or blank");
        }
        if (hashRing.isEmpty()) {
            throw new IllegalStateException("No shards are currently registered in the cluster topology");
        }

        LogicalShard shard = hashRing.getNode(partitionKey)
                .orElseThrow(() -> new IllegalStateException("Failed to resolve shard for key: " + partitionKey));

        if (!shard.isAvailable()) {
            throw new ShardUnavailableException(shard.getShardId(), "Shard status is " + shard.getStatus());
        }

        return shard;
    }

    /**
     * Locates primary and replica shards for high availability or multi-region routing.
     */
    public List<LogicalShard> routeKeyWithReplicas(String partitionKey, int count) {
        if (partitionKey == null || partitionKey.isBlank()) {
            throw new IllegalArgumentException("Partition key cannot be null or blank");
        }
        return hashRing.getNodes(partitionKey, count);
    }

    public Collection<LogicalShard> getRegisteredShards() {
        return Collections.unmodifiableCollection(registeredShards.values());
    }

    public LogicalShard getShard(String shardId) {
        return registeredShards.get(shardId);
    }

    public int getShardCount() {
        return registeredShards.size();
    }
}
