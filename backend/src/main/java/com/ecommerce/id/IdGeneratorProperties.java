package com.ecommerce.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the distributed unique ID generator.
 */
@ConfigurationProperties(prefix = "app.id-generator")
public class IdGeneratorProperties {

    /**
     * Whether the ID generator service is enabled.
     */
    private boolean enabled = true;

    /**
     * Datacenter identifier (0 - 31).
     */
    private long datacenterId = 1;

    /**
     * Worker / node identifier (0 - 31).
     */
    private long workerId = 1;

    /**
     * Custom epoch timestamp in milliseconds (default: 2026-01-01T00:00:00Z = 1767225600000L).
     */
    private long customEpochMs = SnowflakeIdGenerator.DEFAULT_CUSTOM_EPOCH;

    /**
     * Maximum tolerated backward clock drift in milliseconds before rejecting generation.
     */
    private long maxBackwardMs = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(long datacenterId) {
        this.datacenterId = datacenterId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public long getCustomEpochMs() {
        return customEpochMs;
    }

    public void setCustomEpochMs(long customEpochMs) {
        this.customEpochMs = customEpochMs;
    }

    public long getMaxBackwardMs() {
        return maxBackwardMs;
    }

    public void setMaxBackwardMs(long maxBackwardMs) {
        this.maxBackwardMs = maxBackwardMs;
    }
}
