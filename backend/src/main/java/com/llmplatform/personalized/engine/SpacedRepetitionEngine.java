package com.llmplatform.personalized.engine;

/**
 * 间隔重复引擎接口
 * 实现基于艾宾浩斯遗忘曲线的记忆算法
 */
public interface SpacedRepetitionEngine {

    /**
     * 计算下次复习间隔时间
     *
     * @param masteryLevel 当前掌握度 (0-100)
     * @param reviewCount  已复习次数
     * @param consecutiveWrongCount 连续错误次数
     * @return 复习间隔（小时）
     */
    int calculateReviewInterval(int masteryLevel, int reviewCount, int consecutiveWrongCount);

    /**
     * 更新掌握度
     *
     * @param currentLevel 当前掌握度
     * @param isCorrect 本次是否答对
     * @return 新的掌握度
     */
    int updateMasteryLevel(int currentLevel, boolean isCorrect);

    /**
     * 判断是否已掌握
     *
     * @param masteryLevel 掌握度
     * @return 是否已掌握
     */
    boolean isMastered(int masteryLevel);

    /**
     * 判定掌握状态
     *
     * @param masteryLevel 掌握度
     * @return 状态: LEARNING, MASTERED, FORGOTTEN
     */
    String determineStatus(int masteryLevel);
}
