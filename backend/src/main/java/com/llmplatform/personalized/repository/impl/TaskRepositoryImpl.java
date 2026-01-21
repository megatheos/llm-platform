package com.llmplatform.personalized.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.personalized.entity.DailyTask;
import com.llmplatform.personalized.mapper.DailyTaskMapper;
import com.llmplatform.personalized.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 每日任务数据访问仓库实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final DailyTaskMapper dailyTaskMapper;

    @Override
    public DailyTask create(DailyTask task) {
        task.setCreatedTime(LocalDateTime.now());
        dailyTaskMapper.insert(task);
        log.debug("创建每日任务成功, id={}", task.getId());
        return task;
    }

    @Override
    public DailyTask update(DailyTask task) {
        dailyTaskMapper.updateById(task);
        log.debug("更新每日任务成功, id={}", task.getId());
        return task;
    }

    @Override
    public Optional<DailyTask> findById(Long id) {
        return Optional.ofNullable(dailyTaskMapper.selectById(id));
    }

    @Override
    public List<DailyTask> findByUserIdAndDate(Long userId, LocalDate date) {
        return dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
                        .eq(DailyTask::getTaskDate, date)
        );
    }

    @Override
    public List<DailyTask> findByUserIdAndStatus(Long userId, String status) {
        return dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
                        .eq(DailyTask::getStatus, status)
        );
    }

    @Override
    public List<DailyTask> findByPlanId(Long planId) {
        return dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getPlanId, planId)
                        .orderByAsc(DailyTask::getTaskDate)
        );
    }

    @Override
    public List<DailyTask> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
                        .ge(DailyTask::getTaskDate, startDate)
                        .le(DailyTask::getTaskDate, endDate)
                        .orderByAsc(DailyTask::getTaskDate)
        );
    }

    @Override
    public boolean deleteById(Long id) {
        return dailyTaskMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByPlanId(Long planId) {
        return dailyTaskMapper.delete(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getPlanId, planId)
        ) > 0;
    }
}
