package com.ecommerce.hashing;

import java.util.Objects;

/**
 * Representation of a logical node in the consistent hashing ring.
 */
public class HashRingNode implements Comparable<HashRingNode> {

    private final String nodeId;
    private final String host;
    private final int port;
    private final int weight;

    public HashRingNode(String nodeId) {
        this(nodeId, "localhost", 0, 1);
    }

    public HashRingNode(String nodeId, String host, int port) {
        this(nodeId, host, port, 1);
    }

    public HashRingNode(String nodeId, String host, int port, int weight) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId cannot be null or blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be strictly positive");
        }
        this.nodeId = nodeId;
        this.host = host != null ? host : "localhost";
        this.port = port;
        this.weight = weight;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashRingNode that)) return false;
        return nodeId.equals(that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public int compareTo(HashRingNode o) {
        return this.nodeId.compareTo(o.nodeId);
    }
}
