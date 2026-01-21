package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习档案实体
 * 存储用户的学习行为、偏好和表现的综合分析数据
 */
@Data
@TableName("learning_profiles")
public class LearningProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 首选学习时间段 JSON: {"morning": 0.3, "afternoon": 0.5, "evening": 0.2}
     */
    private String preferredLearningTimes;

    /**
     * 薄弱领域 JSON: [{"type": "noun", "errorRate": 0.45}, ...]
     */
    private String weakAreas;

    /**
     * 平均每日学习词汇数
     */
    private Double averageDailyWords;

    /**
     * 平均正确率
     */
    private Double averageAccuracy;

    /**
     * 学习速度趋势: FAST, NORMAL, SLOW
     */
    private String learningSpeedTrend;

    /**
     * 最后分析时间
     */
    private LocalDateTime lastAnalysisTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
