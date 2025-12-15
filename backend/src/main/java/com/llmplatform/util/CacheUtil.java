package com.llmplatform.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Cache utility class for Redis operations
 * Provides convenient methods for caching with graceful error handling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Set a value with TTL in seconds
     * @param key cache key
     * @param value value to cache
     * @param ttlSeconds time to live in seconds
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Cache set: key={}, ttl={}s", key, ttlSeconds);
            return true;
        } catch (Exception e) {
            log.error("Failed to set cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Set a value with TTL using specified time unit
     * @param key cache key
     * @param value value to cache
     * @param ttl time to live
     * @param timeUnit time unit for TTL
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            log.debug("Cache set: key={}, ttl={} {}", key, ttl, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Failed to set cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Set a value without TTL (use with caution)
     * @param key cache key
     * @param value value to cache
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cache set (no TTL): key={}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to set cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get a value by key with type casting
     * @param key cache key
     * @param clazz expected class type
     * @return the cached value or null if not found/error
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                log.debug("Cache hit: key={}", key);
                return (T) value;
            }
            log.debug("Cache miss: key={}", key);
            return null;
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * Get a raw value by key
     * @param key cache key
     * @return the cached value or null if not found/error
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit: key={}", key);
            } else {
                log.debug("Cache miss: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * Delete a key from cache
     * @param key cache key
     * @return true if key was deleted, false otherwise
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            boolean deleted = Boolean.TRUE.equals(result);
            log.debug("Cache delete: key={}, deleted={}", key, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Check if a key exists in cache
     * @param key cache key
     * @return true if key exists, false otherwise
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check cache for key: {}", key, e);
            return false;
        }
    }

    /**
     * Set expiration time for a key in seconds
     * @param key cache key
     * @param ttlSeconds time to live in seconds
     * @return true if successful, false otherwise
     */
    public boolean expire(String key, long ttlSeconds) {
        try {
            Boolean result = redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("Cache expire: key={}, ttl={}s, success={}", key, ttlSeconds, success);
            return success;
        } catch (Exception e) {
            log.error("Failed to set expiration for key: {}", key, e);
            return false;
        }
    }

    /**
     * Set expiration time for a key with specified time unit
     * @param key cache key
     * @param ttl time to live
     * @param timeUnit time unit for TTL
     * @return true if successful, false otherwise
     */
    public boolean expire(String key, long ttl, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.expire(key, ttl, timeUnit);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("Cache expire: key={}, ttl={} {}, success={}", key, ttl, timeUnit, success);
            return success;
        } catch (Exception e) {
            log.error("Failed to set expiration for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for a key in seconds
     * @param key cache key
     * @return remaining TTL in seconds, -1 if no expiry, -2 if key doesn't exist, null on error
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get expiration for key: {}", key, e);
            return null;
        }
    }

    /**
     * Get remaining TTL for a key with specified time unit
     * @param key cache key
     * @param timeUnit time unit for returned TTL
     * @return remaining TTL, -1 if no expiry, -2 if key doesn't exist, null on error
     */
    public Long getExpire(String key, TimeUnit timeUnit) {
        try {
            return redisTemplate.getExpire(key, timeUnit);
        } catch (Exception e) {
            log.error("Failed to get expiration for key: {}", key, e);
            return null;
        }
    }

    /**
     * Set value only if key doesn't exist (for distributed locks)
     * @param key cache key
     * @param value value to cache
     * @param ttlSeconds time to live in seconds
     * @return true if set successfully, false if key already exists or error
     */
    public boolean setIfAbsent(String key, Object value, long ttlSeconds) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("Cache setIfAbsent: key={}, success={}", key, success);
            return success;
        } catch (Exception e) {
            log.error("Failed to setIfAbsent for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get value and refresh TTL if exists
     * @param key cache key
     * @param clazz expected class type
     * @param ttlSeconds new time to live in seconds
     * @return the cached value or null if not found/error
     */
    @SuppressWarnings("unchecked")
    public <T> T getAndRefresh(String key, Class<T> clazz, long ttlSeconds) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
                log.debug("Cache hit and refreshed: key={}, ttl={}s", key, ttlSeconds);
                return (T) value;
            }
            log.debug("Cache miss (no refresh): key={}", key);
            return null;
        } catch (Exception e) {
            log.error("Failed to get and refresh cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * Check if cache is available (Redis connection is working)
     * @return true if cache is available, false otherwise
     */
    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Cache is not available: {}", e.getMessage());
            return false;
        }
    }
}
