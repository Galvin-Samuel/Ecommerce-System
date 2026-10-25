package com.ecommerce.sharding;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Logical shard descriptor representing a virtual partition or database shard.
 */
public class LogicalShard implements Comparable<LogicalShard> {

    public enum Status {
        ACTIVE,
        READ_ONLY,
        DRAINING,
        OFFLINE
    }

    private final String shardId;
    private final String connectionUri;
    private volatile Status status;
    private final Map<String, String> metadata;

    public LogicalShard(String shardId) {
        this(shardId, "mongodb://localhost:27017/ecommerce_" + shardId, Status.ACTIVE, Collections.emptyMap());
    }

    public LogicalShard(String shardId, String connectionUri, Status status, Map<String, String> metadata) {
        if (shardId == null || shardId.isBlank()) {
            throw new IllegalArgumentException("shardId cannot be null or blank");
        }
        this.shardId = shardId;
        this.connectionUri = connectionUri != null ? connectionUri : "mongodb://localhost:27017/ecommerce_" + shardId;
        this.status = status != null ? status : Status.ACTIVE;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public String getShardId() {
        return shardId;
    }

    public String getConnectionUri() {
        return connectionUri;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status != null ? status : Status.ACTIVE;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean isAvailable() {
        return status == Status.ACTIVE || status == Status.READ_ONLY;
    }

    @Override
    public String toString() {
        return shardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalShard that)) return false;
        return shardId.equals(that.shardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shardId);
    }

    @Override
    public int compareTo(LogicalShard o) {
        return this.shardId.compareTo(o.shardId);
    }
}
