package com.llmplatform.personalized.repository;

import com.llmplatform.personalized.entity.StudyPlan;

import java.util.Optional;

/**
 * 学习计划数据访问仓库接口
 */
public interface PlanRepository {

    /**
     * 创建学习计划
     */
    StudyPlan create(StudyPlan plan);

    /**
     * 更新学习计划
     */
    StudyPlan update(StudyPlan plan);

    /**
     * 根据ID获取计划
     */
    Optional<StudyPlan> findById(Long id);

    /**
     * 获取用户当前有效的学习计划
     */
    Optional<StudyPlan> findCurrentByUserId(Long userId);

    /**
     * 删除计划
     */
    boolean deleteById(Long id);

    /**
     * 根据用户ID删除所有计划
     */
    void deleteByUserId(Long userId);

    /**
     * 检查用户是否有计划
     */
    boolean existsByUserId(Long userId);
}
