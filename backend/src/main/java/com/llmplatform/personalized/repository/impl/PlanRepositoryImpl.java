package com.llmplatform.personalized.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.personalized.entity.StudyPlan;
import com.llmplatform.personalized.mapper.StudyPlanMapper;
import com.llmplatform.personalized.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 学习计划数据访问仓库实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PlanRepositoryImpl implements PlanRepository {

    private final StudyPlanMapper studyPlanMapper;

    @Override
    public StudyPlan create(StudyPlan plan) {
        plan.setCreatedTime(LocalDateTime.now());
        plan.setUpdatedTime(LocalDateTime.now());
        studyPlanMapper.insert(plan);
        log.debug("创建学习计划成功, id={}", plan.getId());
        return plan;
    }

    @Override
    public StudyPlan update(StudyPlan plan) {
        plan.setUpdatedTime(LocalDateTime.now());
        studyPlanMapper.updateById(plan);
        log.debug("更新学习计划成功, id={}", plan.getId());
        return plan;
    }

    @Override
    public Optional<StudyPlan> findById(Long id) {
        return Optional.ofNullable(studyPlanMapper.selectById(id));
    }

    @Override
    public Optional<StudyPlan> findCurrentByUserId(Long userId) {
        // 获取用户最近创建的计划（未过期的）
        StudyPlan plan = studyPlanMapper.selectOne(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreatedTime)
                        .last("LIMIT 1")
        );
        return Optional.ofNullable(plan);
    }

    @Override
    public boolean deleteById(Long id) {
        return studyPlanMapper.deleteById(id) > 0;
    }

    @Override
    public void deleteByUserId(Long userId) {
        studyPlanMapper.delete(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
        );
        log.debug("删除用户所有学习计划, userId={}", userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return studyPlanMapper.selectCount(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
        ) > 0;
    }
}
