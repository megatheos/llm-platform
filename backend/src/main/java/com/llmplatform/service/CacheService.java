package com.llmplatform.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cache service interface for Redis operations
 * Provides generic cache get/set/delete methods with TTL management
 */
public interface CacheService {

    /**
     * Set a value with TTL
     * @param key cache key
     * @param value value to cache
     * @param ttl time to live
     * @param timeUnit time unit for TTL
     * @return true if successful, false otherwise
     */
    boolean set(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * Set a value with TTL in seconds
     * @param key cache key
     * @param value value to cache
     * @param ttlSeconds time to live in seconds
     * @return true if successful, false otherwise
     */
    boolean set(String key, Object value, long ttlSeconds);

    /**
     * Get a value by key
     * @param key cache key
     * @param clazz expected class type
     * @return Optional containing the value if found, empty otherwise
     */
    <T> Optional<T> get(String key, Class<T> clazz);

    /**
     * Get a raw value by key
     * @param key cache key
     * @return Optional containing the value if found, empty otherwise
     */
    Optional<Object> get(String key);

    /**
     * Delete a key
     * @param key cache key
     * @return true if key was deleted, false otherwise
     */
    boolean delete(String key);

    /**
     * Check if a key exists
     * @param key cache key
     * @return true if key exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Update TTL for an existing key
     * @param key cache key
     * @param ttl new time to live
     * @param timeUnit time unit for TTL
     * @return true if successful, false otherwise
     */
    boolean expire(String key, long ttl, TimeUnit timeUnit);

    /**
     * Get remaining TTL for a key
     * @param key cache key
     * @param timeUnit time unit for returned TTL
     * @return remaining TTL, -1 if key has no expiry, -2 if key doesn't exist
     */
    long getExpire(String key, TimeUnit timeUnit);

    /**
     * Set value only if key doesn't exist (for distributed locks)
     * @param key cache key
     * @param value value to cache
     * @param ttl time to live
     * @param timeUnit time unit for TTL
     * @return true if set successfully, false if key already exists
     */
    boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * Refresh cache - get value and reset TTL if exists
     * @param key cache key
     * @param clazz expected class type
     * @param ttl new time to live
     * @param timeUnit time unit for TTL
     * @return Optional containing the value if found, empty otherwise
     */
    <T> Optional<T> getAndRefresh(String key, Class<T> clazz, long ttl, TimeUnit timeUnit);
}
