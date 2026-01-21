package com.llmplatform.personalized.engine.impl;

import com.llmplatform.personalized.engine.SpacedRepetitionEngine;
import org.springframework.stereotype.Component;

/**
 * 间隔重复引擎实现
 * 基于艾宾浩斯遗忘曲线的改进算法
 */
@Component
public class SpacedRepetitionEngineImpl implements SpacedRepetitionEngine {

    /**
     * 掌握度阈值：达到此值判定为已掌握
     */
    private static final int MASTERY_THRESHOLD = 80;

    /**
     * 答对增加掌握度
     */
    private static final int CORRECT_INCREMENT = 10;

    /**
     * 答错减少掌握度
     */
    private static final int WRONG_DECREMENT = 15;

    /**
     * 连续错误重置阈值
     */
    private static final int CONSECUTIVE_WRONG_RESET_THRESHOLD = 3;

    /**
     * 基础间隔时间（小时）
     */
    private static final int BASE_INTERVAL = 1;

    /**
     * 计算下次复习间隔时间
     *
     * 基于艾宾浩斯遗忘曲线的改进算法：
     * 复习间隔 = 基础间隔 × (1 + 掌握度/100)^复习次数
     *
     * 根据掌握度范围使用不同的增长因子：
     * - 掌握度 0-20: 间隔 = 1, 2, 4, 8小时... (因子2)
     * - 掌握度 21-40: 间隔 = 1, 3, 9, 27小时... (因子3)
     * - 掌握度 41-60: 间隔 = 1, 4, 16, 64小时... (因子4)
     * - 掌握度 61-80: 间隔 = 1, 6, 36, 216小时... (因子6)
     * - 掌握度 81-100: 间隔 = 1, 8, 64, 512小时... (因子8)
     *
     * @param masteryLevel 当前掌握度 (0-100)
     * @param reviewCount 已复习次数
     * @param consecutiveWrongCount 连续错误次数
     * @return 复习间隔（小时）
     */
    @Override
    public int calculateReviewInterval(int masteryLevel, int reviewCount, int consecutiveWrongCount) {
        // 连续3次答错，重置为最短间隔
        if (consecutiveWrongCount >= CONSECUTIVE_WRONG_RESET_THRESHOLD) {
            return BASE_INTERVAL;
        }

        // 计算增长因子
        double growthFactor = getGrowthFactor(masteryLevel);

        // 计算复习间隔: 基础间隔 × 增长因子^复习次数
        // 使用 Math.round 确保返回整数
        long interval = Math.round(BASE_INTERVAL * Math.pow(growthFactor, reviewCount));

        // 限制最大间隔为 24 * 30 = 720 小时（约30天）
        return (int) Math.min(interval, 720);
    }

    /**
     * 根据掌握度获取增长因子
     */
    private double getGrowthFactor(int masteryLevel) {
        if (masteryLevel <= 20) {
            return 2.0;  // 间隔翻倍
        } else if (masteryLevel <= 40) {
            return 3.0;  // 间隔3倍增长
        } else if (masteryLevel <= 60) {
            return 4.0;  // 间隔4倍增长
        } else if (masteryLevel <= 80) {
            return 6.0;  // 间隔6倍增长
        } else {
            return 8.0;  // 间隔8倍增长
        }
    }

    /**
     * 更新掌握度
     *
     * 规则：
     * - 答对：masteryLevel = min(100, masteryLevel + 10)
     * - 答错：masteryLevel = max(0, masteryLevel - 15)
     *
     * @param currentLevel 当前掌握度
     * @param isCorrect 本次是否答对
     * @return 新的掌握度
     */
    @Override
    public int updateMasteryLevel(int currentLevel, boolean isCorrect) {
        if (isCorrect) {
            return Math.min(100, currentLevel + CORRECT_INCREMENT);
        } else {
            return Math.max(0, currentLevel - WRONG_DECREMENT);
        }
    }

    /**
     * 判断是否已掌握
     *
     * @param masteryLevel 掌握度
     * @return 是否已掌握（掌握度 >= 80）
     */
    @Override
    public boolean isMastered(int masteryLevel) {
        return masteryLevel >= MASTERY_THRESHOLD;
    }

    /**
     * 判定掌握状态
     *
     * @param masteryLevel 掌握度
     * @return 状态: LEARNING, MASTERED, FORGOTTEN
     */
    @Override
    public String determineStatus(int masteryLevel) {
        if (masteryLevel >= MASTERY_THRESHOLD) {
            return "MASTERED";
        } else if (masteryLevel <= 0) {
            return "FORGOTTEN";
        } else {
            return "LEARNING";
        }
    }
}
