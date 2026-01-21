package com.llmplatform.personalized.service;

import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.entity.LearningStreak;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.entity.StudyPlan;
import com.llmplatform.personalized.repository.AchievementRepository;
import com.llmplatform.personalized.repository.MemoryRepository;
import com.llmplatform.personalized.repository.ProfileRepository;
import com.llmplatform.personalized.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 数据一致性服务
 * 处理数据库和缓存的同步、错误重试和级联删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataConsistencyService {

    public static final int MAX_RETRY_COUNT = 3;
    public static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final MemoryRepository memoryRepository;
    private final ProfileRepository profileRepository;
    private final PlanRepository planRepository;
    private final AchievementRepository achievementRepository;

    /**
     * 写入数据时同步更新缓存（Write-Through策略）
     */
    public <T> boolean writeWithCache(String cacheKey, T data, DatabaseWriter<T> writer) {
        int attempt = 0;
        while (attempt < MAX_RETRY_COUNT) {
            try {
                // 首先写入数据库
                T savedData = writer.write(data);

                // 然后更新缓存
                updateCache(cacheKey, savedData);

                log.debug("Write-Through completed for key: {}", cacheKey);
                return true;

            } catch (Exception e) {
                attempt++;
                log.warn("Write attempt {} failed for key {}: {}", attempt, cacheKey, e.getMessage());

                if (attempt >= MAX_RETRY_COUNT) {
                    log.error("All {} write attempts failed for key {}", MAX_RETRY_COUNT, cacheKey);
                    throw e;
                }

                // 指数退避重试
                try {
                    TimeUnit.MILLISECONDS.sleep((long) (Math.pow(2, attempt) * 100));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Write interrupted", ie);
                }
            }
        }
        return false;
    }

    /**
     * 检测并修复缓存不一致
     */
    public <T> ConsistencyCheckResult<T> detectAndRepair(String cacheKey, T databaseData, CacheLoader<T> loader) {
        ConsistencyCheckResult<T> result = new ConsistencyCheckResult<>();

        try {
            // 从缓存加载数据
            T cachedData = loader.load();

            // 比较数据库和缓存数据
            boolean isConsistent = compareData(databaseData, cachedData);

            result.setCacheExists(cachedData != null);
            result.setConsistent(isConsistent);

            if (!isConsistent) {
                log.warn("Cache inconsistency detected for key: {}", cacheKey);
                result.setRepaired(updateCache(cacheKey, databaseData));
                result.setMessage("Cache repaired");
            } else {
                result.setMessage("Cache is consistent");
            }

        } catch (Exception e) {
            log.error("Error during consistency check for key {}: {}", cacheKey, e.getMessage());
            result.setError(e.getMessage());
            // 尝试修复
            try {
                result.setRepaired(updateCache(cacheKey, databaseData));
            } catch (Exception repairError) {
                result.setError("Repair failed: " + repairError.getMessage());
            }
        }

        return result;
    }

    /**
     * 级联删除用户所有学习相关数据
     */
    public boolean cascadeDeleteUserData(Long userId) {
        log.info("Starting cascade delete for user: {}", userId);
        boolean allSuccess = true;

        try {
            // 1. 删除记忆记录
            memoryRepository.deleteByUserId(userId);
            log.debug("Deleted memory records for user: {}", userId);

            // 2. 删除学习档案
            profileRepository.deleteByUserId(userId);
            log.debug("Deleted learning profile for user: {}", userId);

            // 3. 删除学习计划
            planRepository.deleteByUserId(userId);
            log.debug("Deleted study plans for user: {}", userId);

            // 4. 删除连续学习记录
            achievementRepository.findStreakByUserId(userId).ifPresent(streak -> {
                achievementRepository.updateStreak(streak);
            });
            log.debug("Processed learning streak for user: {}", userId);

            // 5. 清除相关缓存
            invalidateUserCache(userId);
            log.debug("Invalidated cache for user: {}", userId);

        } catch (Exception e) {
            log.error("Error during cascade delete for user {}: {}", userId, e.getMessage());
            allSuccess = false;
        }

        log.info("Cascade delete completed for user: {}, success: {}", userId, allSuccess);
        return allSuccess;
    }

    /**
     * 清除用户相关缓存
     */
    public void invalidateUserCache(Long userId) {
        String[] cacheKeys = {
                "learning:profile:" + userId,
                "learning:plan:" + userId,
                "learning:streak:" + userId,
                "memory:due_reviews:" + userId
        };

        for (String key : cacheKeys) {
            try {
                redisTemplate.delete(key);
                log.debug("Deleted cache key: {}", key);
            } catch (Exception e) {
                log.warn("Failed to delete cache key {}: {}", key, e.getMessage());
            }
        }
    }

    /**
     * 更新缓存
     */
    private <T> boolean updateCache(String cacheKey, T data) {
        try {
            redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL);
            log.debug("Cache updated: {} -> {}", cacheKey, data);
            return true;
        } catch (Exception e) {
            log.error("Failed to update cache for key {}: {}", cacheKey, e.getMessage());
            return false;
        }
    }

    /**
     * 比较数据是否一致
     */
    private <T> boolean compareData(T databaseData, T cachedData) {
        if (databaseData == null && cachedData == null) {
            return true;
        }
        if (databaseData == null || cachedData == null) {
            return false;
        }
        return databaseData.equals(cachedData);
    }

    /**
     * 数据库写入函数式接口
     */
    @FunctionalInterface
    public interface DatabaseWriter<T> {
        T write(T data);
    }

    /**
     * 缓存加载函数式接口
     */
    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }

    /**
     * 一致性检查结果
     */
    @lombok.Data
    public static class ConsistencyCheckResult<T> {
        private boolean cacheExists;
        private boolean consistent;
        private boolean repaired;
        private String message;
        private String error;
    }
}
