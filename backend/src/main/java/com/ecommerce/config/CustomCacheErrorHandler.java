package com.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Custom error handler for Redis cache operations ensuring that Redis outages,
 * connection drops, or timeouts do not disrupt application reads from MongoDB.
 */
public class CustomCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache GET failed for cache '{}', key '{}': {}. Falling back to primary database.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis cache PUT failed for cache '{}', key '{}': {}. Continuing without cache update.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache EVICT failed for cache '{}', key '{}': {}. Continuing without cache eviction.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache CLEAR failed for cache '{}': {}. Continuing without cache clear.",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
