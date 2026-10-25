package com.ecommerce.hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Ketama-style hash function utilizing MD5 / SHA-256 digest to compute
 * uniform 64-bit integer points along the circular hash ring.
 */
public class KetamaHashFunction implements HashFunction {

    private final String algorithm;

    public KetamaHashFunction() {
        this("MD5");
    }

    public KetamaHashFunction(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public long hash(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Hashing key cannot be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // Extract 8 bytes to construct a 64-bit signed/unsigned integer
            long h = 0;
            for (int i = 0; i < 8 && i < digest.length; i++) {
                h = (h << 8) | (digest[i] & 0xFFL);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Configured digest algorithm not found: " + algorithm, e);
        }
    }
}
