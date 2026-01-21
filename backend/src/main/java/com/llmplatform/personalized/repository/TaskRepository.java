package com.llmplatform.personalized.repository;

import com.llmplatform.personalized.entity.DailyTask;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日任务数据访问仓库接口
 */
public interface TaskRepository {

    /**
     * 创建每日任务
     */
    DailyTask create(DailyTask task);

    /**
     * 更新每日任务
     */
    DailyTask update(DailyTask task);

    /**
     * 根据ID获取任务
     */
    Optional<DailyTask> findById(Long id);

    /**
     * 获取用户某日的所有任务
     */
    List<DailyTask> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * 获取用户某状态的任务
     */
    List<DailyTask> findByUserIdAndStatus(Long userId, String status);

    /**
     * 获取用户某计划的任务
     */
    List<DailyTask> findByPlanId(Long planId);

    /**
     * 获取用户某日期范围内的任务
     */
    List<DailyTask> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 删除任务
     */
    boolean deleteById(Long id);

    /**
     * 删除计划的所有任务
     */
    boolean deleteByPlanId(Long planId);
}
