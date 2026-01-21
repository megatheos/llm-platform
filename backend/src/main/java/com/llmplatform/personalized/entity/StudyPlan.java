package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划实体
 * 存储用户的学习目标和计划详情
 */
@Data
@TableName("study_plans")
public class StudyPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 目标类型: EXAM, TRAVEL, BUSINESS, DAILY
     */
    private String goalType;

    /**
     * 目标日期
     */
    private LocalDate targetDate;

    /**
     * 每日任务数量
     */
    private Integer dailyTaskCount;

    /**
     * 当前阶段: BEGINNER, INTERMEDIATE, ADVANCED
     */
    private String currentPhase;

    /**
     * 完成率
     */
    private Double completionRate;

    /**
     * 调整历史 JSON: [{"date": "2024-01-15", "reason": "完成率低", "change": -20%}]
     */
    private String adjustmentHistory;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
