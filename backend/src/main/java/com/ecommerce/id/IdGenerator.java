package com.ecommerce.id;

import java.util.List;

/**
 * Contract for a distributed unique identifier generator.
 */
public interface IdGenerator {

    /**
     * Generates the next unique 64-bit identifier.
     *
     * @return a unique positive 64-bit integer
     */
    long nextId();

    /**
     * Generates the next unique identifier as a decimal String.
     *
     * @return unique ID string
     */
    default String nextIdString() {
        return String.valueOf(nextId());
    }

    /**
     * Generates a batch of unique identifiers sequentially.
     *
     * @param count number of IDs to generate
     * @return list of unique IDs
     */
    List<Long> nextIdBatch(int count);

    /**
     * Decomposes a 64-bit identifier into its constituent parts (timestamp, node, sequence).
     *
     * @param id identifier to decompose
     * @return decomposed representation
     */
    IdDecomposition parseId(long id);

    /**
     * Gets the assigned worker/node identifier.
     */
    long getWorkerId();

    /**
     * Gets the assigned datacenter identifier.
     */
    long getDatacenterId();
}
