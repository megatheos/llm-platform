package com.llmplatform.personalized.engine.impl;

import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.engine.LearningAnalyticsEngine;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 学习分析引擎实现
 * 分析用户的学习行为和表现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningAnalyticsEngineImpl implements LearningAnalyticsEngine {

    private static final double WEAK_AREA_THRESHOLD = 0.40;
    private static final String MORNING = "morning";
    private static final String AFTERNOON = "afternoon";
    private static final String EVENING = "evening";

    private final MemoryRepository memoryRepository;

    @Override
    public Map<String, Double> analyzeTimePreferences(List<LearningRecord> records) {
        if (records == null || records.isEmpty()) {
            return createEmptyTimePreferences();
        }

        int morningCount = 0;
        int afternoonCount = 0;
        int eveningCount = 0;
        int totalRecords = 0;

        for (LearningRecord record : records) {
            LocalDateTime activityTime = record.getActivityTime();
            if (activityTime != null) {
                int hour = activityTime.getHour();
                if (hour >= 5 && hour < 12) {
                    morningCount++;
                } else if (hour >= 12 && hour < 18) {
                    afternoonCount++;
                } else {
                    eveningCount++;
                }
                totalRecords++;
            }
        }

        if (totalRecords == 0) {
            return createEmptyTimePreferences();
        }

        Map<String, Double> preferences = new HashMap<>();
        preferences.put(MORNING, (double) morningCount / totalRecords);
        preferences.put(AFTERNOON, (double) afternoonCount / totalRecords);
        preferences.put(EVENING, (double) eveningCount / totalRecords);

        log.debug("学习时间偏好分析完成: morning={}, afternoon={}, evening={}",
                String.format("%.2f", preferences.get(MORNING)),
                String.format("%.2f", preferences.get(AFTERNOON)),
                String.format("%.2f", preferences.get(EVENING)));

        return preferences;
    }

    @Override
    public List<WeakArea> identifyWeakAreas(List<LearningRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 按活动类型分组统计错误率
        Map<String, Integer> totalByType = new HashMap<>();
        Map<String, Integer> wrongByType = new HashMap<>();

        // 由于LearningRecord没有正确率字段，我们从MemoryRecord获取
        // 这里使用userId从MemoryRecord统计
        if (!records.isEmpty()) {
            Long userId = records.get(0).getUserId();
            List<MemoryRecord> memoryRecords = memoryRepository.findAllByUserId(userId);

            // 按词汇类型分组（这里简化为按状态分组）
            // 实际实现中应该根据词汇的实际类型分类
            int forgottenCount = 0;
            int learningCount = 0;

            for (MemoryRecord record : memoryRecords) {
                if ("FORGOTTEN".equals(record.getStatus())) {
                    forgottenCount++;
                } else if ("LEARNING".equals(record.getStatus())) {
                    learningCount++;
                }
            }

            int total = memoryRecords.size();
            if (total > 0) {
                double forgottenRate = (double) forgottenCount / total;
                double learningRate = (double) learningCount / total;

                List<WeakArea> weakAreas = new ArrayList<>();

                // FORGOTTEN状态超过40%视为薄弱领域
                if (forgottenRate > WEAK_AREA_THRESHOLD) {
                    weakAreas.add(new WeakArea("FORGOTTEN", forgottenRate));
                }

                // LEARNING状态超过60%视为需要加强
                if (learningRate > 0.6) {
                    weakAreas.add(new WeakArea("LEARNING", learningRate));
                }

                return weakAreas;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public double calculateLearningSpeed(List<LearningRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        // 计算学习天数
        Set<LocalDate> learningDays = new HashSet<>();
        for (LearningRecord record : records) {
            if (record.getActivityTime() != null) {
                learningDays.add(record.getActivityTime().toLocalDate());
            }
        }

        if (learningDays.isEmpty()) {
            return 0.0;
        }

        // 计算每日平均学习记录数
        double averagePerDay = (double) records.size() / learningDays.size();

        log.debug("学习速度计算: records={}, days={}, average={}",
                records.size(), learningDays.size(), String.format("%.2f", averagePerDay));

        return averagePerDay;
    }

    @Override
    public ProgressCurve generateProgressCurve(Long userId, int days) {
        if (days <= 0) {
            days = 30; // 默认30天
        }

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        // 生成日期列表
        List<String> dates = new ArrayList<>();
        List<Integer> cumulativeWords = new ArrayList<>();
        List<Integer> masteredWords = new ArrayList<>();
        List<Double> accuracyRates = new ArrayList<>();

        LocalDate currentDate = startTime.toLocalDate();
        int cumulative = 0;
        int mastered = 0;
        int totalReviews = 0;
        int totalCorrect = 0;

        while (!currentDate.isAfter(endTime.toLocalDate())) {
            dates.add(currentDate.toString());

            // 获取该日期的记忆记录统计
            // 简化实现：使用MemoryRepository获取用户统计
            try {
                var stats = memoryRepository.getStatisticsByUserId(userId);
                if (stats != null) {
                    cumulative = stats.getTotalWords() != null ? stats.getTotalWords() : cumulative;
                    mastered = stats.getMasteredWords() != null ? stats.getMasteredWords() : mastered;
                    totalReviews = stats.getTotalReviews() != null ? stats.getTotalReviews() : totalReviews;
                    totalCorrect = stats.getCorrectCount() != null ? stats.getCorrectCount() : totalCorrect;
                }
            } catch (Exception e) {
                log.warn("获取用户统计数据失败, userId={}", userId, e);
            }

            cumulativeWords.add(cumulative);
            masteredWords.add(mastered);

            double accuracy = totalReviews > 0 ? (double) totalCorrect / totalReviews : 0.0;
            accuracyRates.add(accuracy);

            currentDate = currentDate.plusDays(1);
        }

        log.debug("进度曲线生成完成: days={}, latestCumulative={}, latestMastered={}",
                days, cumulative, mastered);

        return new ProgressCurve(dates, cumulativeWords, masteredWords, accuracyRates);
    }

    /**
     * 创建空的时间偏好分布
     */
    private Map<String, Double> createEmptyTimePreferences() {
        Map<String, Double> preferences = new HashMap<>();
        preferences.put(MORNING, 0.0);
        preferences.put(AFTERNOON, 0.0);
        preferences.put(EVENING, 0.0);
        return preferences;
    }
}
