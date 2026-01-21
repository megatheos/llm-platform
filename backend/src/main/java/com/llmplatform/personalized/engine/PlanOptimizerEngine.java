package com.llmplatform.personalized.engine;

import com.llmplatform.personalized.entity.LearningProfile;

import java.time.LocalDate;
import java.util.List;

/**
 * 计划优化引擎接口
 * 生成和优化用户的学习计划
 */
public interface PlanOptimizerEngine {

    /**
     * 生成学习路径
     *
     * @param goalType 目标类型 (EXAM, TRAVEL, BUSINESS, DAILY)
     * @param targetDate 目标日期
     * @param currentLevel 当前水平 (BEGINNER, INTERMEDIATE, ADVANCED)
     * @return 学习路径
     */
    LearningPath generateLearningPath(String goalType, LocalDate targetDate, String currentLevel);

    /**
     * 计算每日任务量
     *
     * @param profile 学习档案
     * @param remainingDays 剩余天数
     * @param targetWordCount 目标词汇数
     * @return 每日任务数量 (10-50)
     */
    int calculateDailyTaskCount(LearningProfile profile, int remainingDays, int targetWordCount);

    /**
     * 调整任务难度
     *
     * @param recentCompletionRate 最近完成率
     * @param currentTaskCount 当前任务量
     * @return 调整后的任务量
     */
    int adjustTaskDifficulty(double recentCompletionRate, int currentTaskCount);

    /**
     * 学习路径内部类
     */
    record LearningPath(
            List<WordSet> wordSets,
            List<String> priorities,
            int totalWords
    ) {}

    /**
     * 词汇集内部类
     */
    record WordSet(String name, List<Long> wordIds, String category) {}
}
