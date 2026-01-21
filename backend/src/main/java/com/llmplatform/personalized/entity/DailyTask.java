package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日任务实体
 * 存储用户每日的学习任务详情
 */
@Data
@TableName("daily_tasks")
public class DailyTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long userId;

    /**
     * 任务日期
     */
    private LocalDate taskDate;

    /**
     * 任务类型: VOCABULARY, GRAMMAR, DIALOGUE, REVIEW
     */
    private String taskType;

    /**
     * 任务内容 JSON: [{"wordId": 1, "type": "vocabulary"}, ...]
     */
    private String content;

    /**
     * 状态: PENDING, IN_PROGRESS, COMPLETED
     */
    private String status;

    /**
     * 总项目数
     */
    private Integer totalItems;

    /**
     * 完成项目数
     */
    private Integer completedItems;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
}
