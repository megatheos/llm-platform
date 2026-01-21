package com.llmplatform.personalized.engine.impl;

import com.llmplatform.personalized.engine.LearningAnalyticsEngine;
import com.llmplatform.personalized.engine.PlanOptimizerEngine;
import com.llmplatform.personalized.entity.LearningProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 计划优化引擎实现
 * 生成和优化用户的学习计划
 */
@Slf4j
@Component
public class PlanOptimizerEngineImpl implements PlanOptimizerEngine {

    private static final int MIN_DAILY_TASKS = 10;
    private static final int MAX_DAILY_TASKS = 50;
    private static final double LOW_COMPLETION_THRESHOLD = 0.60;
    private static final double HIGH_COMPLETION_THRESHOLD = 0.90;
    private static final double LOW_ADJUSTMENT = 0.80;
    private static final double HIGH_ADJUSTMENT = 1.10;

    @Override
    public LearningPath generateLearningPath(String goalType, LocalDate targetDate, String currentLevel) {
        log.debug("生成学习路径: goalType={}, targetDate={}, currentLevel={}",
                goalType, targetDate, currentLevel);

        // 根据目标类型选择词汇集
        List<WordSet> wordSets = generateWordSetsForGoal(goalType, currentLevel);

        // 根据目标类型设置优先级
        List<String> priorities = generatePrioritiesForGoal(goalType);

        // 计算总词汇数
        int totalWords = wordSets.stream()
                .mapToInt(ws -> ws.wordIds().size())
                .sum();

        log.debug("学习路径生成完成: totalWords={}, wordSetsCount={}",
                totalWords, wordSets.size());

        return new LearningPath(wordSets, priorities, totalWords);
    }

    @Override
    public int calculateDailyTaskCount(LearningProfile profile, int remainingDays, int targetWordCount) {
        if (remainingDays <= 0) {
            log.warn("剩余天数无效: {}", remainingDays);
            return MIN_DAILY_TASKS;
        }

        if (targetWordCount <= 0) {
            log.warn("目标词汇数无效: {}", targetWordCount);
            return MIN_DAILY_TASKS;
        }

        // 基础每日任务量 = 目标词汇数 / 剩余天数
        int baseCount = targetWordCount / remainingDays;

        // 根据学习档案调整
        if (profile != null) {
            String speedTrend = profile.getLearningSpeedTrend();
            if ("FAST".equals(speedTrend)) {
                baseCount = (int) Math.ceil(baseCount * 1.2);
            } else if ("SLOW".equals(speedTrend)) {
                baseCount = (int) Math.floor(baseCount * 0.8);
            }

            Double avgAccuracy = profile.getAverageAccuracy();
            if (avgAccuracy != null && avgAccuracy < 0.6) {
                // 正确率低，减少任务量
                baseCount = (int) Math.floor(baseCount * 0.9);
            }
        }

        // 限制在合理范围内
        int dailyCount = Math.max(MIN_DAILY_TASKS, Math.min(MAX_DAILY_TASKS, baseCount));

        log.debug("每日任务量计算: target={}, days={}, profile={}, result={}",
                targetWordCount, remainingDays,
                profile != null ? profile.getLearningSpeedTrend() : "null",
                dailyCount);

        return dailyCount;
    }

    @Override
    public int adjustTaskDifficulty(double recentCompletionRate, int currentTaskCount) {
        if (currentTaskCount < MIN_DAILY_TASKS || currentTaskCount > MAX_DAILY_TASKS) {
            log.warn("当前任务量超出范围: {}", currentTaskCount);
            return currentTaskCount;
        }

        int adjustedCount;

        if (recentCompletionRate < LOW_COMPLETION_THRESHOLD) {
            // 完成率低，减少20%任务量
            adjustedCount = (int) Math.round(currentTaskCount * LOW_ADJUSTMENT);
            log.info("调整任务量（完成率低）: {} -> {}", currentTaskCount, adjustedCount);
        } else if (recentCompletionRate > HIGH_COMPLETION_THRESHOLD) {
            // 完成率高，增加10%任务量
            adjustedCount = (int) Math.round(currentTaskCount * HIGH_ADJUSTMENT);
            log.info("调整任务量（完成率高）: {} -> {}", currentTaskCount, adjustedCount);
        } else {
            // 完成率正常，保持不变
            adjustedCount = currentTaskCount;
            log.debug("任务量保持不变: {}", currentTaskCount);
        }

        // 确保在合理范围内
        return Math.max(MIN_DAILY_TASKS, Math.min(MAX_DAILY_TASKS, adjustedCount));
    }

    /**
     * 根据目标类型生成词汇集
     */
    private List<WordSet> generateWordSetsForGoal(String goalType, String currentLevel) {
        List<WordSet> wordSets = new ArrayList<>();

        switch (goalType.toUpperCase()) {
            case "EXAM" -> {
                wordSets.add(new WordSet("核心词汇", Arrays.asList(1L, 2L, 3L, 4L, 5L), "CORE"));
                wordSets.add(new WordSet("学术词汇", Arrays.asList(6L, 7L, 8L, 9L, 10L), "ACADEMIC"));
                wordSets.add(new WordSet("高频词", Arrays.asList(11L, 12L, 13L, 14L, 15L), "HIGH_FREQUENCY"));
                if ("ADVANCED".equals(currentLevel)) {
                    wordSets.add(new WordSet("复杂句型", Arrays.asList(16L, 17L, 18L), "COMPLEX"));
                }
            }
            case "TRAVEL" -> {
                wordSets.add(new WordSet("基础问候", Arrays.asList(1L, 2L, 3L), "GREETING"));
                wordSets.add(new WordSet("交通出行", Arrays.asList(4L, 5L, 6L), "TRANSPORTATION"));
                wordSets.add(new WordSet("酒店住宿", Arrays.asList(7L, 8L, 9L), "ACCOMMODATION"));
                wordSets.add(new WordSet("餐厅点餐", Arrays.asList(10L, 11L, 12L), "DINING"));
            }
            case "BUSINESS" -> {
                wordSets.add(new WordSet("商务邮件", Arrays.asList(1L, 2L, 3L, 4L), "EMAIL"));
                wordSets.add(new WordSet("会议用语", Arrays.asList(5L, 6L, 7L, 8L), "MEETING"));
                wordSets.add(new WordSet("谈判技巧", Arrays.asList(9L, 10L, 11L, 12L), "NEGOTIATION"));
                wordSets.add(new WordSet("行业术语", Arrays.asList(13L, 14L, 15L), "TERMINOLOGY"));
            }
            default -> {
                wordSets.add(new WordSet("日常会话", Arrays.asList(1L, 2L, 3L, 4L, 5L), "CONVERSATION"));
                wordSets.add(new WordSet("生活场景", Arrays.asList(6L, 7L, 8L, 9L, 10L), "LIFESTYLE"));
                wordSets.add(new WordSet("兴趣爱好", Arrays.asList(11L, 12L, 13L, 14L, 15L), "HOBBIES"));
            }
        }

        return wordSets;
    }

    /**
     * 根据目标类型生成优先级
     */
    private List<String> generatePrioritiesForGoal(String goalType) {
        return switch (goalType.toUpperCase()) {
            case "EXAM" -> Arrays.asList("CORE", "ACADEMIC", "HIGH_FREQUENCY", "COMPLEX");
            case "TRAVEL" -> Arrays.asList("GREETING", "TRANSPORTATION", "ACCOMMODATION", "DINING");
            case "BUSINESS" -> Arrays.asList("EMAIL", "MEETING", "TERMINOLOGY", "NEGOTIATION");
            default -> Arrays.asList("CONVERSATION", "LIFESTYLE", "HOBBIES");
        };
    }
}
