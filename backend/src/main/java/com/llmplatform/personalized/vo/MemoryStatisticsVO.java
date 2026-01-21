package com.llmplatform.personalized.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 记忆统计视图对象
 */
@Data
@Builder
public class MemoryStatisticsVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总词汇数
     */
    private Integer totalWords;

    /**
     * 已掌握词汇数
     */
    private Integer masteredWords;

    /**
     * 学习中词汇数
     */
    private Integer learningWords;

    /**
     * 已遗忘词汇数
     */
    private Integer forgottenWords;

    /**
     * 总复习次数
     */
    private Integer totalReviews;

    /**
     * 正确次数
     */
    private Integer correctCount;

    /**
     * 错误次数
     */
    private Integer wrongCount;

    /**
     * 正确率
     */
    private Double accuracyRate;

    /**
     * 待复习数量
     */
    private Integer pendingReviews;

    /**
     * 平均掌握度
     */
    private Double averageMasteryLevel;
}
