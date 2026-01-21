package com.llmplatform.personalized.service;

import com.llmplatform.personalized.entity.DailyTask;
import com.llmplatform.personalized.entity.StudyPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 计划生成服务接口
 */
public interface PlanGenerationService {

    /**
     * 创建学习计划
     *
     * @param userId 用户ID
     * @param goalType 目标类型 (EXAM, TRAVEL, BUSINESS, DAILY)
     * @param targetDate 目标日期
     * @param targetWordCount 目标词汇数
     * @return 创建的学习计划
     */
    StudyPlan createStudyPlan(Long userId, String goalType, LocalDate targetDate, int targetWordCount);

    /**
     * 生成每日任务
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 每日任务列表
     */
    List<DailyTask> generateDailyTasks(Long userId, LocalDate date);

    /**
     * 动态调整计划
     *
     * @param userId 用户ID
     * @return 调整后的计划
     */
    StudyPlan adjustPlan(Long userId);

    /**
     * 获取当前计划
     *
     * @param userId 用户ID
     * @return 当前计划
     */
    StudyPlan getCurrentPlan(Long userId);

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param completedItems 完成的项目数
     * @return 更新后的任务
     */
    DailyTask completeTask(Long taskId, int completedItems);

    /**
     * 获取用户某日的任务完成率
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 完成率 (0.0-1.0)
     */
    double getCompletionRate(Long userId, LocalDate date);

    /**
     * 计算最近N天的平均完成率
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 平均完成率
     */
    double getRecentCompletionRate(Long userId, int days);
}
