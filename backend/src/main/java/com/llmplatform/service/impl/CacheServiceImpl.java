package com.llmplatform.service.impl;

import com.llmplatform.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cache service implementation using Redis
 * Provides robust caching with TTL management and graceful error handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
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

    @Override
    public boolean set(String key, Object value, long ttlSeconds) {
        return set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                log.debug("Cache hit: key={}", key);
                return Optional.of((T) value);
            }
            log.debug("Cache miss: key={}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Object> get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit: key={}", key);
                return Optional.of(value);
            }
            log.debug("Cache miss: key={}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
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

    @Override
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }

    @Override
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

    @Override
    public long getExpire(String key, TimeUnit timeUnit) {
        try {
            Long ttl = redisTemplate.getExpire(key, timeUnit);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            log.error("Failed to get expiration for key: {}", key, e);
            return -2;
        }
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl, timeUnit);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("Cache setIfAbsent: key={}, success={}", key, success);
            return success;
        } catch (Exception e) {
            log.error("Failed to setIfAbsent for key: {}", key, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAndRefresh(String key, Class<T> clazz, long ttl, TimeUnit timeUnit) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                // Refresh TTL
                redisTemplate.expire(key, ttl, timeUnit);
                log.debug("Cache hit and refreshed: key={}, ttl={} {}", key, ttl, timeUnit);
                return Optional.of((T) value);
            }
            log.debug("Cache miss (no refresh): key={}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get and refresh cache for key: {}", key, e);
            return Optional.empty();
        }
    }
}
