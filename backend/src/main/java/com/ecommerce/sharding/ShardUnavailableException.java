package com.ecommerce.sharding;

import com.ecommerce.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an assigned logical shard is marked OFFLINE or unreachable.
 */
public class ShardUnavailableException extends ApiException {

    private final String shardId;

    public ShardUnavailableException(String shardId, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Logical shard '" + shardId + "' is currently unavailable: " + message);
        this.shardId = shardId;
    }

    public String getShardId() {
        return shardId;
    }
}
