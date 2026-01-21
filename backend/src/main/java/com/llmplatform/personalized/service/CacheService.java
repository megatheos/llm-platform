package com.llmplatform.personalized.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 提供缓存操作和降级逻辑
 * 验证需求：6.2, 7.3, 7.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration DUE_REVIEWS_TTL = Duration.ofHours(1);
    private static final Duration PROFILE_TTL = Duration.ofHours(24);
    private static final Duration PLAN_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存键命名规范
     */
    public static String buildKey(String prefix, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object part : parts) {
            sb.append(":").append(part);
        }
        return sb.toString();
    }

    /**
     * 获取缓存值（带降级逻辑）
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            if (!redisTemplate.hasKey(key)) {
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }

            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }

            log.debug("Cache hit for key: {}", key);
            return Optional.of(type.cast(value));

        } catch (Exception e) {
            log.warn("Cache read failed for key {}: {}", key, e.getMessage());
            return Optional.empty(); // Return empty (fallback to DB)
        }
    }

    /**
     * 设置缓存值
     */
    public <T> boolean set(String key, T value, Duration ttl) {
        try {
            if (ttl == null) {
                ttl = DEFAULT_TTL;
            }
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache set for key: {} with TTL: {}s", key, ttl.getSeconds());
            return true;
        } catch (Exception e) {
            log.error("Cache write failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Cache delete for key: {}, result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Cache delete failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Cache exists check failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 获取TTL
     */
    public Long getTtl(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache TTL check failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 设置用户学习档案缓存
     */
    public <T> boolean setUserProfile(Long userId, T profile) {
        String key = buildKey("learning:profile", userId);
        return set(key, profile, PROFILE_TTL);
    }

    /**
     * 获取用户学习档案缓存
     */
    public <T> Optional<T> getUserProfile(Long userId, Class<T> type) {
        String key = buildKey("learning:profile", userId);
        return get(key, type);
    }

    /**
     * 设置用户学习计划缓存
     */
    public <T> boolean setUserPlan(Long userId, T plan) {
        String key = buildKey("learning:plan", userId);
        return set(key, plan, PLAN_TTL);
    }

    /**
     * 获取用户学习计划缓存
     */
    public <T> Optional<T> getUserPlan(Long userId, Class<T> type) {
        String key = buildKey("learning:plan", userId);
        return get(key, type);
    }

    /**
     * 设置待复习列表缓存（短TTL）
     */
    public <T> boolean setDueReviews(Long userId, T reviews) {
        String key = buildKey("memory:due_reviews", userId);
        return set(key, reviews, DUE_REVIEWS_TTL);
    }

    /**
     * 获取待复习列表缓存
     */
    public <T> Optional<T> getDueReviews(Long userId, Class<T> type) {
        String key = buildKey("memory:due_reviews", userId);
        return get(key, type);
    }

    /**
     * 设置用户连续学习记录缓存
     */
    public <T> boolean setUserStreak(Long userId, T streak) {
        String key = buildKey("learning:streak", userId);
        return set(key, streak, PROFILE_TTL);
    }

    /**
     * 获取用户连续学习记录缓存
     */
    public <T> Optional<T> getUserStreak(Long userId, Class<T> type) {
        String key = buildKey("learning:streak", userId);
        return get(key, type);
    }

    /**
     * 清除用户所有缓存
     */
    public void invalidateUserCache(Long userId) {
        delete(buildKey("learning:profile", userId));
        delete(buildKey("learning:plan", userId));
        delete(buildKey("learning:streak", userId));
        delete(buildKey("memory:due_reviews", userId));
        log.info("Invalidated all cache for user: {}", userId);
    }

    /**
     * 缓存降级 - 当Redis不可用时返回默认值
     */
    public <T> T getWithFallback(String key, Class<T> type, T fallbackValue) {
        return get(key, type).orElse(fallbackValue);
    }

    /**
     * 原子递增
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Cache increment failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }
}
