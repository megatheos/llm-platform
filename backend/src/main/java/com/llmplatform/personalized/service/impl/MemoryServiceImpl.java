package com.llmplatform.personalized.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.engine.SpacedRepetitionEngine;
import com.llmplatform.personalized.repository.MemoryRepository;
import com.llmplatform.personalized.service.MemoryService;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记忆记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private static final String DUE_REVIEWS_CACHE_PREFIX = "memory:due_reviews:";
    private static final Duration DUE_REVIEWS_CACHE_TTL = Duration.ofHours(1);

    private final MemoryRepository memoryRepository;
    private final SpacedRepetitionEngine spacedRepetitionEngine;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public MemoryRecord createRecord(Long userId, Long wordId) {
        MemoryRecord record = new MemoryRecord();
        record.setUserId(userId);
        record.setWordId(wordId);
        record.setMasteryLevel(0);
        record.setReviewCount(0);
        record.setCorrectCount(0);
        record.setWrongCount(0);
        record.setStatus("LEARNING");
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());

        return memoryRepository.create(record);
    }

    @Override
    public MemoryRecord createOrGetRecord(Long userId, Long wordId) {
        return memoryRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> createRecord(userId, wordId));
    }

    @Override
    public MemoryRecord submitReview(Long recordId, boolean isCorrect) {
        MemoryRecord record = memoryRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("记忆记录不存在: " + recordId));

        // 更新掌握度
        int newMasteryLevel = spacedRepetitionEngine.updateMasteryLevel(
                record.getMasteryLevel(), isCorrect);
        record.setMasteryLevel(newMasteryLevel);

        // 更新正确/错误次数
        if (isCorrect) {
            record.setCorrectCount(record.getCorrectCount() + 1);
        } else {
            record.setWrongCount(record.getWrongCount() + 1);
        }

        // 更新复习次数
        record.setReviewCount(record.getReviewCount() + 1);

        // 更新状态
        record.setStatus(spacedRepetitionEngine.determineStatus(newMasteryLevel));

        // 计算下次复习间隔
        int consecutiveWrongCount = isCorrect ? 0 : record.getWrongCount();
        int intervalHours = spacedRepetitionEngine.calculateReviewInterval(
                newMasteryLevel, record.getReviewCount(), consecutiveWrongCount);

        // 更新复习时间和下次复习时间
        record.setLastReviewTime(LocalDateTime.now());
        record.setNextReviewTime(LocalDateTime.now().plusHours(intervalHours));
        record.setUpdatedTime(LocalDateTime.now());

        // 清除缓存
        clearDueReviewsCache(record.getUserId());

        return memoryRepository.update(record);
    }

    @Override
    public List<MemoryRecord> getDueReviews(Long userId, int limit) {
        String cacheKey = DUE_REVIEWS_CACHE_PREFIX + userId;

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<MemoryRecord> cached = (List<MemoryRecord>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取待复习记录, userId={}", userId);
            return cached.size() > limit ? cached.subList(0, limit) : cached;
        }

        // 从数据库获取
        LocalDateTime now = LocalDateTime.now();
        List<MemoryRecord> records = memoryRepository.findDueReviewsByUserId(userId, now, limit);

        // 放入缓存
        redisTemplate.opsForValue().set(cacheKey, records, DUE_REVIEWS_CACHE_TTL);
        log.debug("从数据库获取待复习记录并缓存, userId={}, count={}", userId, records.size());

        return records;
    }

    @Override
    public Page<MemoryRecord> getDueReviewsPage(Long userId, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        return memoryRepository.findDueReviewsPageByUserId(userId, now, page, size);
    }

    @Override
    public MemoryRecord getRecord(Long recordId) {
        return memoryRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("记忆记录不存在: " + recordId));
    }

    @Override
    public MemoryRecord getRecordByUserIdAndWordId(Long userId, Long wordId) {
        return memoryRepository.findByUserIdAndWordId(userId, wordId)
                .orElse(null);
    }

    @Override
    public MemoryStatisticsVO getStatistics(Long userId) {
        return memoryRepository.getStatisticsByUserId(userId);
    }

    @Override
    public List<MemoryRecord> getAllRecords(Long userId) {
        return memoryRepository.findAllByUserId(userId);
    }

    /**
     * 清除用户的待复习记录缓存
     */
    private void clearDueReviewsCache(Long userId) {
        String cacheKey = DUE_REVIEWS_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.debug("清除待复习记录缓存, userId={}", userId);
    }
}
