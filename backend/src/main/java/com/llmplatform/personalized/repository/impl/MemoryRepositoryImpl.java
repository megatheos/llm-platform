package com.llmplatform.personalized.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.mapper.MemoryRecordMapper;
import com.llmplatform.personalized.repository.MemoryRepository;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记忆记录数据访问仓库实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemoryRepositoryImpl extends ServiceImpl<MemoryRecordMapper, MemoryRecord>
        implements MemoryRepository {

    private final MemoryRecordMapper memoryRecordMapper;

    @Override
    public MemoryRecord create(MemoryRecord record) {
        memoryRecordMapper.insert(record);
        log.debug("Created memory record: id={}, userId={}, wordId={}",
                record.getId(), record.getUserId(), record.getWordId());
        return record;
    }

    @Override
    public MemoryRecord update(MemoryRecord record) {
        memoryRecordMapper.updateById(record);
        log.debug("Updated memory record: id={}", record.getId());
        return record;
    }

    @Override
    public Optional<MemoryRecord> findById(Long id) {
        return Optional.ofNullable(memoryRecordMapper.selectById(id));
    }

    @Override
    public Optional<MemoryRecord> findByUserIdAndWordId(Long userId, Long wordId) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId)
                .eq(MemoryRecord::getWordId, wordId)
                .last("LIMIT 1");
        return Optional.ofNullable(memoryRecordMapper.selectOne(wrapper));
    }

    @Override
    public List<MemoryRecord> findDueReviewsByUserId(Long userId, LocalDateTime currentTime, int limit) {
        // Query for records where next_review_time is null or <= current time
        // Order by: next_review_time ASC (overdue first), mastery_level ASC (lower mastery first)
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId)
                .isNotNull(MemoryRecord::getNextReviewTime)
                .le(MemoryRecord::getNextReviewTime, currentTime)
                .orderByAsc(MemoryRecord::getNextReviewTime)
                .orderByAsc(MemoryRecord::getMasteryLevel)
                .last("LIMIT " + limit);

        List<MemoryRecord> records = memoryRecordMapper.selectList(wrapper);
        log.debug("Found {} due reviews for user {}", records.size(), userId);
        return records;
    }

    @Override
    public Page<MemoryRecord> findDueReviewsPageByUserId(Long userId, LocalDateTime currentTime, int page, int size) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId)
                .isNotNull(MemoryRecord::getNextReviewTime)
                .le(MemoryRecord::getNextReviewTime, currentTime)
                .orderByAsc(MemoryRecord::getNextReviewTime)
                .orderByAsc(MemoryRecord::getMasteryLevel);

        Page<MemoryRecord> pageParam = new Page<>(page, size);
        return memoryRecordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<MemoryRecord> findAllByUserId(Long userId) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId)
                .orderByDesc(MemoryRecord::getUpdatedTime);
        return memoryRecordMapper.selectList(wrapper);
    }

    @Override
    public MemoryStatisticsVO getStatisticsByUserId(Long userId) {
        List<MemoryRecord> allRecords = findAllByUserId(userId);

        if (allRecords.isEmpty()) {
            return MemoryStatisticsVO.builder()
                    .userId(userId)
                    .totalWords(0)
                    .masteredWords(0)
                    .learningWords(0)
                    .forgottenWords(0)
                    .totalReviews(0)
                    .correctCount(0)
                    .wrongCount(0)
                    .accuracyRate(0.0)
                    .pendingReviews(0)
                    .averageMasteryLevel(0.0)
                    .build();
        }

        int totalWords = allRecords.size();
        int masteredWords = (int) allRecords.stream()
                .filter(r -> "MASTERED".equals(r.getStatus()))
                .count();
        int learningWords = (int) allRecords.stream()
                .filter(r -> "LEARNING".equals(r.getStatus()))
                .count();
        int forgottenWords = (int) allRecords.stream()
                .filter(r -> "FORGOTTEN".equals(r.getStatus()))
                .count();

        int totalReviews = allRecords.stream()
                .mapToInt(MemoryRecord::getReviewCount)
                .sum();
        int correctCount = allRecords.stream()
                .mapToInt(MemoryRecord::getCorrectCount)
                .sum();
        int wrongCount = allRecords.stream()
                .mapToInt(MemoryRecord::getWrongCount)
                .sum();

        double accuracyRate = totalReviews > 0
                ? (double) correctCount / totalReviews * 100
                : 0.0;

        long pendingReviews = allRecords.stream()
                .filter(r -> r.getNextReviewTime() != null
                        && r.getNextReviewTime().isBefore(LocalDateTime.now()))
                .count();

        double averageMasteryLevel = allRecords.stream()
                .mapToInt(MemoryRecord::getMasteryLevel)
                .average()
                .orElse(0.0);

        log.debug("Calculated statistics for user {}: total={}, mastered={}, accuracy={}%",
                userId, totalWords, masteredWords, String.format("%.2f", accuracyRate));

        return MemoryStatisticsVO.builder()
                .userId(userId)
                .totalWords(totalWords)
                .masteredWords(masteredWords)
                .learningWords(learningWords)
                .forgottenWords(forgottenWords)
                .totalReviews(totalReviews)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .accuracyRate(Math.round(accuracyRate * 100) / 100.0)
                .pendingReviews((int) pendingReviews)
                .averageMasteryLevel(Math.round(averageMasteryLevel * 100) / 100.0)
                .build();
    }

    @Override
    public long countMasteredByUserId(Long userId) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId)
                .eq(MemoryRecord::getStatus, "MASTERED");
        return memoryRecordMapper.selectCount(wrapper);
    }

    @Override
    public long countTotalByUserId(Long userId) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId);
        return memoryRecordMapper.selectCount(wrapper);
    }

    @Override
    public int deleteByUserId(Long userId) {
        LambdaQueryWrapper<MemoryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryRecord::getUserId, userId);
        int deleted = memoryRecordMapper.delete(wrapper);
        log.debug("Deleted {} memory records for user {}", deleted, userId);
        return deleted;
    }
}
