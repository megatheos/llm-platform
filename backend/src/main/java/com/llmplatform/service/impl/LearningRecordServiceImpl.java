package com.llmplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.llmplatform.common.ActivityType;
import com.llmplatform.common.CacheConstants;
import com.llmplatform.dto.RecordQueryDTO;
import com.llmplatform.entity.DialogueSession;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Quiz;
import com.llmplatform.entity.Scenario;
import com.llmplatform.entity.Word;
import com.llmplatform.mapper.DialogueSessionMapper;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.QuizMapper;
import com.llmplatform.mapper.ScenarioMapper;
import com.llmplatform.mapper.WordMapper;
import com.llmplatform.service.LearningRecordService;
import com.llmplatform.util.CacheUtil;
import com.llmplatform.vo.LearningRecordVO;
import com.llmplatform.vo.LearningRecordsVO;
import com.llmplatform.vo.LearningStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Learning record service implementation
 * Handles tracking and retrieval of user learning activities
 * Includes caching for user statistics to improve performance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;
    private final WordMapper wordMapper;
    private final DialogueSessionMapper dialogueSessionMapper;
    private final QuizMapper quizMapper;
    private final ScenarioMapper scenarioMapper;
    private final CacheUtil cacheUtil;

    @Override
    public LearningRecordsVO getRecords(Long userId, RecordQueryDTO params) {
        // Build query with filters
        LambdaQueryWrapper<LearningRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningRecord::getUserId, userId);

        // Apply activity type filter
        if (params.getActivityType() != null && !params.getActivityType().isEmpty()) {
            queryWrapper.eq(LearningRecord::getActivityType, params.getActivityType());
        }

        // Apply date range filters
        if (params.getStartDate() != null) {
            queryWrapper.ge(LearningRecord::getActivityTime, params.getStartDate());
        }
        if (params.getEndDate() != null) {
            queryWrapper.le(LearningRecord::getActivityTime, params.getEndDate());
        }

        // Order by activity time descending (most recent first)
        queryWrapper.orderByDesc(LearningRecord::getActivityTime);

        // Pagination
        int page = params.getPage() != null ? params.getPage() : 1;
        int pageSize = params.getPageSize() != null ? params.getPageSize() : 20;
        Page<LearningRecord> pageRequest = new Page<>(page, pageSize);

        Page<LearningRecord> resultPage = learningRecordMapper.selectPage(pageRequest, queryWrapper);

        // Convert to VOs with activity details
        List<LearningRecordVO> recordVOs = resultPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        // Build response
        LearningRecordsVO response = new LearningRecordsVO();
        response.setRecords(recordVOs);
        response.setTotal(resultPage.getTotal());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages((int) Math.ceil((double) resultPage.getTotal() / pageSize));

        return response;
    }

    @Override
    public LearningStatisticsVO getStatistics(Long userId) {
        // Try to get from cache first
        String cacheKey = CacheConstants.statsKey(userId);
        LearningStatisticsVO cachedStats = cacheUtil.get(cacheKey, LearningStatisticsVO.class);
        if (cachedStats != null) {
            log.debug("Statistics found in cache for user: {}", userId);
            return cachedStats;
        }

        log.debug("Computing statistics for user: {}", userId);
        LearningStatisticsVO stats = computeStatistics(userId);

        // Cache the result
        cacheUtil.set(cacheKey, stats, CacheConstants.STATS_TTL_SECONDS);
        
        return stats;
    }

    /**
     * Compute learning statistics from database
     */
    private LearningStatisticsVO computeStatistics(Long userId) {
        LearningStatisticsVO stats = new LearningStatisticsVO();

        // Count by activity type
        stats.setTotalWordQueries(countByActivityType(userId, ActivityType.WORD_QUERY));
        stats.setTotalDialogueSessions(countByActivityType(userId, ActivityType.DIALOGUE));
        stats.setTotalQuizzes(countByActivityType(userId, ActivityType.QUIZ));

        // Total activities
        Long totalActivities = learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
        );
        stats.setTotalActivities(totalActivities);

        // Calculate average quiz score
        stats.setAverageQuizScore(calculateAverageQuizScore(userId));

        // Get first and last activity dates
        LearningRecord firstRecord = learningRecordMapper.selectOne(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .orderByAsc(LearningRecord::getActivityTime)
                .last("LIMIT 1")
        );
        if (firstRecord != null) {
            stats.setFirstActivityDate(firstRecord.getActivityTime());
        }

        LearningRecord lastRecord = learningRecordMapper.selectOne(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .orderByDesc(LearningRecord::getActivityTime)
                .last("LIMIT 1")
        );
        if (lastRecord != null) {
            stats.setLastActivityDate(lastRecord.getActivityTime());
        }

        // Activities in last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        stats.setActivitiesLast7Days(learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getActivityTime, sevenDaysAgo)
        ));

        // Activities in last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        stats.setActivitiesLast30Days(learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getActivityTime, thirtyDaysAgo)
        ));

        return stats;
    }

    @Override
    @Transactional
    public void recordWordQuery(Long userId, Long wordId) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.WORD_QUERY.name());
        record.setActivityId(wordId);
        record.setActivityTime(LocalDateTime.now());
        learningRecordMapper.insert(record);
        
        // Invalidate statistics cache
        invalidateStatisticsCache(userId);
        log.debug("Recorded word query: userId={}, wordId={}", userId, wordId);
    }

    @Override
    @Transactional
    public void recordDialogueSession(Long userId, DialogueSession session) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.DIALOGUE.name());
        record.setActivityId(session.getId());
        record.setActivityTime(session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now());
        learningRecordMapper.insert(record);
        
        // Invalidate statistics cache
        invalidateStatisticsCache(userId);
        log.debug("Recorded dialogue session: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    @Transactional
    public void recordQuizResult(Long userId, Quiz quiz) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.QUIZ.name());
        record.setActivityId(quiz.getId());
        record.setActivityTime(quiz.getCompletedAt() != null ? quiz.getCompletedAt() : LocalDateTime.now());
        learningRecordMapper.insert(record);
        
        // Invalidate statistics cache
        invalidateStatisticsCache(userId);
        log.debug("Recorded quiz result: userId={}, quizId={}", userId, quiz.getId());
    }

    /**
     * Invalidate statistics cache for a user when new activities are recorded
     */
    private void invalidateStatisticsCache(Long userId) {
        String cacheKey = CacheConstants.statsKey(userId);
        cacheUtil.delete(cacheKey);
        log.debug("Invalidated statistics cache for user: {}", userId);
    }

    /**
     * Count records by activity type for a user
     */
    private Long countByActivityType(Long userId, ActivityType activityType) {
        return learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getActivityType, activityType.name())
        );
    }

    /**
     * Calculate average quiz score for a user
     */
    private Double calculateAverageQuizScore(Long userId) {
        List<Quiz> completedQuizzes = quizMapper.selectList(
            new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getUserId, userId)
                .isNotNull(Quiz::getUserScore)
                .isNotNull(Quiz::getCompletedAt)
        );

        if (completedQuizzes.isEmpty()) {
            return 0.0;
        }

        double totalPercentage = completedQuizzes.stream()
            .filter(q -> q.getTotalScore() != null && q.getTotalScore() > 0)
            .mapToDouble(q -> (double) q.getUserScore() / q.getTotalScore() * 100)
            .sum();

        long validQuizCount = completedQuizzes.stream()
            .filter(q -> q.getTotalScore() != null && q.getTotalScore() > 0)
            .count();

        return validQuizCount > 0 ? totalPercentage / validQuizCount : 0.0;
    }

    /**
     * Convert LearningRecord entity to VO with activity details
     */
    private LearningRecordVO convertToVO(LearningRecord record) {
        LearningRecordVO vo = new LearningRecordVO();
        vo.setId(record.getId());
        vo.setActivityType(record.getActivityType());
        vo.setActivityId(record.getActivityId());
        vo.setActivityTime(record.getActivityTime());

        // Fetch activity-specific details
        vo.setActivityDetails(fetchActivityDetails(record));

        return vo;
    }

    /**
     * Fetch activity-specific details based on activity type
     */
    private Object fetchActivityDetails(LearningRecord record) {
        ActivityType type = ActivityType.valueOf(record.getActivityType());
        
        switch (type) {
            case WORD_QUERY:
                return fetchWordDetails(record.getActivityId());
            case DIALOGUE:
                return fetchDialogueDetails(record.getActivityId());
            case QUIZ:
                return fetchQuizDetails(record.getActivityId());
            default:
                return null;
        }
    }

    /**
     * Fetch word details for a word query record
     */
    private Map<String, Object> fetchWordDetails(Long wordId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("wordId", word.getId());
        details.put("word", word.getWord());
        details.put("sourceLang", word.getSourceLang());
        details.put("targetLang", word.getTargetLang());
        details.put("translation", word.getTranslation());
        return details;
    }

    /**
     * Fetch dialogue session details
     */
    private Map<String, Object> fetchDialogueDetails(Long sessionId) {
        DialogueSession session = dialogueSessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("sessionId", session.getId());
        details.put("scenarioId", session.getScenarioId());
        details.put("startedAt", session.getStartedAt());
        details.put("endedAt", session.getEndedAt());

        // Fetch scenario name
        Scenario scenario = scenarioMapper.selectById(session.getScenarioId());
        if (scenario != null) {
            details.put("scenarioName", scenario.getName());
            details.put("scenarioCategory", scenario.getCategory());
        }

        return details;
    }

    /**
     * Fetch quiz details
     */
    private Map<String, Object> fetchQuizDetails(Long quizId) {
        Quiz quiz = quizMapper.selectById(quizId);
        if (quiz == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("quizId", quiz.getId());
        details.put("difficulty", quiz.getDifficulty());
        details.put("totalScore", quiz.getTotalScore());
        details.put("userScore", quiz.getUserScore());
        details.put("completedAt", quiz.getCompletedAt());
        return details;
    }
}
