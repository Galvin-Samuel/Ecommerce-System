package com.ecommerce.hashing;

/**
 * Functional interface for computing 64-bit hash values for consistent hashing.
 */
@FunctionalInterface
public interface HashFunction {
    long hash(String key);
}
