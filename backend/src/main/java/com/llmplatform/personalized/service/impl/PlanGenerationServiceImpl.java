package com.llmplatform.personalized.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.personalized.engine.PlanOptimizerEngine;
import com.llmplatform.personalized.entity.DailyTask;
import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.entity.StudyPlan;
import com.llmplatform.personalized.repository.PlanRepository;
import com.llmplatform.personalized.repository.ProfileRepository;
import com.llmplatform.personalized.repository.TaskRepository;
import com.llmplatform.personalized.service.PlanGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计划生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationServiceImpl implements PlanGenerationService {

    private static final String PLAN_CACHE_PREFIX = "learning:plan:";
    private static final String DAILY_TASKS_CACHE_PREFIX = "learning:daily_tasks:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final ProfileRepository profileRepository;
    private final PlanOptimizerEngine planOptimizerEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public StudyPlan createStudyPlan(Long userId, String goalType, LocalDate targetDate, int targetWordCount) {
        log.debug("创建学习计划: userId={}, goalType={}, targetDate={}, targetWordCount={}",
                userId, goalType, targetDate, targetWordCount);

        // 获取学习档案
        LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);

        // 计算剩余天数和每日任务量
        int remainingDays = (int) (targetDate.toEpochDay() - LocalDate.now().toEpochDay());
        int dailyTaskCount = planOptimizerEngine.calculateDailyTaskCount(profile, remainingDays, targetWordCount);

        // 确定当前阶段
        String currentPhase = determineCurrentPhase(profile);

        // 创建计划
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setGoalType(goalType);
        plan.setTargetDate(targetDate);
        plan.setDailyTaskCount(dailyTaskCount);
        plan.setCurrentPhase(currentPhase);
        plan.setCompletionRate(0.0);
        plan.setAdjustmentHistory("[]");

        StudyPlan savedPlan = planRepository.create(plan);

        // 生成今日和明日的任务
        generateDailyTasks(userId, LocalDate.now(), savedPlan.getId());
        generateDailyTasks(userId, LocalDate.now().plusDays(1), savedPlan.getId());

        // 清除缓存
        clearPlanCache(userId);

        log.info("学习计划创建成功: planId={}, dailyTaskCount={}", savedPlan.getId(), dailyTaskCount);
        return savedPlan;
    }

    @Override
    public List<DailyTask> generateDailyTasks(Long userId, LocalDate date) {
        StudyPlan plan = planRepository.findCurrentByUserId(userId).orElse(null);
        if (plan == null) {
            log.warn("用户没有学习计划, userId={}", userId);
            return List.of();
        }
        return generateDailyTasks(userId, date, plan.getId());
    }

    private List<DailyTask> generateDailyTasks(Long userId, LocalDate date, Long planId) {
        // 检查是否已有任务
        List<DailyTask> existing = taskRepository.findByUserIdAndDate(userId, date);
        if (!existing.isEmpty()) {
            log.debug("当日任务已存在, userId={}, date={}", userId, date);
            return existing;
        }

        // 获取学习档案（用于薄弱领域分析）
        LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);

        // 生成不同类型的任务
        List<DailyTask> tasks = new ArrayList<>();

        // 词汇学习任务（主要任务）
        DailyTask vocabTask = createTask(planId, userId, date, "VOCABULARY", 10);
        tasks.add(vocabTask);

        // 复习任务
        DailyTask reviewTask = createTask(planId, userId, date, "REVIEW", 5);
        tasks.add(reviewTask);

        // 如果有薄弱领域，添加专项任务
        if (hasWeakAreas(profile)) {
            DailyTask weakAreaTask = createTask(planId, userId, date, "WEAK_AREA", 3);
            tasks.add(weakAreaTask);
        }

        log.debug("生成每日任务完成: userId={}, date={}, count={}", userId, date, tasks.size());
        return tasks;
    }

    private DailyTask createTask(Long planId, Long userId, LocalDate date, String taskType, int itemCount) {
        DailyTask task = new DailyTask();
        task.setPlanId(planId);
        task.setUserId(userId);
        task.setTaskDate(date);
        task.setTaskType(taskType);
        task.setStatus("PENDING");
        task.setTotalItems(itemCount);
        task.setCompletedItems(0);

        // 生成任务内容
        List<Map<String, Object>> content = generateTaskContent(taskType, itemCount);
        try {
            task.setContent(objectMapper.writeValueAsString(content));
        } catch (JsonProcessingException e) {
            log.error("序列化任务内容失败", e);
            task.setContent("[]");
        }

        return taskRepository.create(task);
    }

    private List<Map<String, Object>> generateTaskContent(String taskType, int count) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            content.add(Map.of(
                    "itemId", i + 1,
                    "wordId", (long) (Math.random() * 1000),
                    "type", taskType
            ));
        }
        return content;
    }

    private boolean hasWeakAreas(LearningProfile profile) {
        if (profile == null || profile.getWeakAreas() == null) {
            return false;
        }
        try {
            List<Map<String, Object>> weakAreas = objectMapper.readValue(
                    profile.getWeakAreas(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return !weakAreas.isEmpty();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public StudyPlan adjustPlan(Long userId) {
        StudyPlan plan = planRepository.findCurrentByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户没有学习计划"));

        // 计算最近7天的完成率
        double recentRate = getRecentCompletionRate(userId, 7);

        // 获取当前每日任务量
        int currentCount = plan.getDailyTaskCount();

        // 调整任务量
        int adjustedCount = planOptimizerEngine.adjustTaskDifficulty(recentRate, currentCount);

        // 记录调整历史
        String reason = recentRate < 0.6 ? "完成率低" : (recentRate > 0.9 ? "完成率高" : "正常");
        int change = adjustedCount - currentCount;
        String changeStr = (change >= 0 ? "+" : "") + change + "%";

        // 更新计划
        plan.setDailyTaskCount(adjustedCount);
        plan.setCompletionRate(recentRate);
        addAdjustmentHistory(plan, reason, changeStr);

        StudyPlan updatedPlan = planRepository.update(plan);
        clearPlanCache(userId);

        log.info("计划调整完成: userId={}, oldCount={}, newCount={}, reason={}",
                userId, currentCount, adjustedCount, reason);
        return updatedPlan;
    }

    private void addAdjustmentHistory(StudyPlan plan, String reason, String change) {
        try {
            List<Map<String, String>> history;
            if (plan.getAdjustmentHistory() != null && !plan.getAdjustmentHistory().equals("[]")) {
                history = objectMapper.readValue(
                        plan.getAdjustmentHistory(),
                        new TypeReference<List<Map<String, String>>>() {}
                );
            } else {
                history = new ArrayList<>();
            }

            history.add(0, Map.of(
                    "date", LocalDate.now().toString(),
                    "reason", reason,
                    "change", change
            ));

            // 只保留最近10条记录
            if (history.size() > 10) {
                history = history.subList(0, 10);
            }

            plan.setAdjustmentHistory(objectMapper.writeValueAsString(history));
        } catch (JsonProcessingException e) {
            log.error("更新调整历史失败", e);
        }
    }

    @Override
    public StudyPlan getCurrentPlan(Long userId) {
        // 尝试从缓存获取
        String cacheKey = PLAN_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof StudyPlan) {
                return (StudyPlan) cached;
            }
        } catch (Exception e) {
            log.warn("获取计划缓存失败", e);
        }

        // 从数据库获取
        StudyPlan plan = planRepository.findCurrentByUserId(userId).orElse(null);
        if (plan != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, plan, CACHE_TTL);
            } catch (Exception e) {
                log.warn("缓存计划失败", e);
            }
        }

        return plan;
    }

    @Override
    public DailyTask completeTask(Long taskId, int completedItems) {
        DailyTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        task.setCompletedItems(completedItems);
        task.setStatus(completedItems >= task.getTotalItems() ? "COMPLETED" : "IN_PROGRESS");

        if (task.getStatus().equals("COMPLETED")) {
            task.setCompletedTime(LocalDateTime.now());
        }

        DailyTask updatedTask = taskRepository.update(task);

        // 清除相关缓存
        clearDailyTasksCache(task.getUserId(), task.getTaskDate());

        return updatedTask;
    }

    @Override
    public double getCompletionRate(Long userId, LocalDate date) {
        List<DailyTask> tasks = taskRepository.findByUserIdAndDate(userId, date);
        if (tasks.isEmpty()) {
            return 0.0;
        }

        int totalItems = tasks.stream().mapToInt(DailyTask::getTotalItems).sum();
        int completedItems = tasks.stream().mapToInt(DailyTask::getCompletedItems).sum();

        return totalItems > 0 ? (double) completedItems / totalItems : 0.0;
    }

    @Override
    public double getRecentCompletionRate(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<DailyTask> tasks = taskRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        if (tasks.isEmpty()) {
            return 0.0;
        }

        int totalItems = tasks.stream().mapToInt(DailyTask::getTotalItems).sum();
        int completedItems = tasks.stream().mapToInt(DailyTask::getCompletedItems).sum();

        return totalItems > 0 ? (double) completedItems / totalItems : 0.0;
    }

    private String determineCurrentPhase(LearningProfile profile) {
        if (profile == null) {
            return "BEGINNER";
        }
        Integer masteredWords = profile.getAverageDailyWords() != null ?
                (int) (profile.getAverageDailyWords() * 30) : 0;

        if (masteredWords < 100) {
            return "BEGINNER";
        } else if (masteredWords < 500) {
            return "INTERMEDIATE";
        } else {
            return "ADVANCED";
        }
    }

    private void clearPlanCache(Long userId) {
        String cacheKey = PLAN_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }

    private void clearDailyTasksCache(Long userId, LocalDate date) {
        String cacheKey = DAILY_TASKS_CACHE_PREFIX + userId + ":" + date;
        redisTemplate.delete(cacheKey);
    }
}
