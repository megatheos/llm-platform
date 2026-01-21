package com.llmplatform.personalized.unit;

import com.llmplatform.personalized.engine.PlanOptimizerEngine;
import com.llmplatform.personalized.entity.DailyTask;
import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.entity.StudyPlan;
import com.llmplatform.personalized.repository.PlanRepository;
import com.llmplatform.personalized.repository.ProfileRepository;
import com.llmplatform.personalized.repository.TaskRepository;
import com.llmplatform.personalized.service.impl.PlanGenerationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PlanGenerationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanGenerationServiceUnitTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PlanOptimizerEngine planOptimizerEngine;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PlanGenerationServiceImpl planGenerationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long userId = 1L;
    private StudyPlan testPlan;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        planGenerationService = new PlanGenerationServiceImpl(
                planRepository,
                taskRepository,
                profileRepository,
                planOptimizerEngine,
                redisTemplate,
                objectMapper
        );

        testPlan = new StudyPlan();
        testPlan.setId(1L);
        testPlan.setUserId(userId);
        testPlan.setGoalType("DAILY");
        testPlan.setTargetDate(LocalDate.now().plusDays(30));
        testPlan.setDailyTaskCount(20);
        testPlan.setCurrentPhase("BEGINNER");
        testPlan.setCompletionRate(0.0);
        testPlan.setAdjustmentHistory("[]");
    }

    @Test
    @DisplayName("创建学习计划")
    void testCreateStudyPlan() {
        LearningProfile profile = new LearningProfile();
        profile.setLearningSpeedTrend("NORMAL");

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(planOptimizerEngine.calculateDailyTaskCount(any(), anyInt(), anyInt())).thenReturn(25);
        when(planRepository.create(any(StudyPlan.class))).thenAnswer(inv -> {
            StudyPlan plan = inv.getArgument(0);
            plan.setId(1L);
            return plan;
        });
        when(taskRepository.findByUserIdAndDate(eq(userId), any())).thenReturn(Collections.emptyList());
        when(taskRepository.create(any(DailyTask.class))).thenAnswer(inv -> {
            DailyTask task = inv.getArgument(0);
            task.setId(1L);
            return task;
        });

        StudyPlan result = planGenerationService.createStudyPlan(
                userId, "DAILY", LocalDate.now().plusDays(30), 1000);

        assertNotNull(result);
        assertEquals("DAILY", result.getGoalType());
        assertEquals(25, result.getDailyTaskCount());
        verify(planRepository).create(any(StudyPlan.class));
    }

    @Test
    @DisplayName("获取当前计划")
    void testGetCurrentPlan_FromCache() {
        when(valueOperations.get(anyString())).thenReturn(testPlan);

        StudyPlan result = planGenerationService.getCurrentPlan(userId);

        assertNotNull(result);
        assertEquals(testPlan.getId(), result.getId());
        verify(planRepository, never()).findCurrentByUserId(any());
    }

    @Test
    @DisplayName("获取当前计划 - 缓存未命中")
    void testGetCurrentPlan_CacheMiss() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(planRepository.findCurrentByUserId(userId)).thenReturn(Optional.of(testPlan));

        StudyPlan result = planGenerationService.getCurrentPlan(userId);

        assertNotNull(result);
        verify(planRepository).findCurrentByUserId(userId);
        verify(valueOperations).set(anyString(), eq(testPlan), any());
    }

    @Test
    @DisplayName("生成每日任务 - 已有任务时不生成")
    void testGenerateDailyTasks_AlreadyExists() {
        DailyTask existingTask = new DailyTask();
        existingTask.setId(1L);
        existingTask.setTaskType("VOCABULARY");

        when(planRepository.findCurrentByUserId(userId)).thenReturn(Optional.of(testPlan));
        when(taskRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(List.of(existingTask));

        List<DailyTask> result = planGenerationService.generateDailyTasks(userId, LocalDate.now());

        assertEquals(1, result.size());
        verify(taskRepository, never()).create(any());
    }

    @Test
    @DisplayName("完成任务")
    void testCompleteTask() {
        DailyTask task = new DailyTask();
        task.setId(1L);
        task.setUserId(userId);
        task.setTotalItems(10);
        task.setCompletedItems(5);
        task.setStatus("IN_PROGRESS");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any(DailyTask.class))).thenAnswer(inv -> inv.getArgument(0));

        DailyTask result = planGenerationService.completeTask(1L, 10);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(10, result.getCompletedItems());
        assertNotNull(result.getCompletedTime());
    }

    @Test
    @DisplayName("部分完成任务")
    void testCompleteTask_Partial() {
        DailyTask task = new DailyTask();
        task.setId(1L);
        task.setTotalItems(10);
        task.setCompletedItems(5);
        task.setStatus("PENDING");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any(DailyTask.class))).thenAnswer(inv -> inv.getArgument(0));

        DailyTask result = planGenerationService.completeTask(1L, 7);

        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(7, result.getCompletedItems());
        assertNull(result.getCompletedTime());
    }

    @Test
    @DisplayName("获取完成率")
    void testGetCompletionRate() {
        DailyTask task1 = new DailyTask();
        task1.setTotalItems(10);
        task1.setCompletedItems(5);

        DailyTask task2 = new DailyTask();
        task2.setTotalItems(10);
        task2.setCompletedItems(10);

        when(taskRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(List.of(task1, task2));

        double rate = planGenerationService.getCompletionRate(userId, LocalDate.now());

        assertEquals(0.75, rate);
    }

    @Test
    @DisplayName("获取完成率 - 无任务")
    void testGetCompletionRate_NoTasks() {
        when(taskRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        double rate = planGenerationService.getCompletionRate(userId, LocalDate.now());

        assertEquals(0.0, rate);
    }

    @Test
    @DisplayName("获取最近完成率")
    void testGetRecentCompletionRate() {
        DailyTask task1 = new DailyTask();
        task1.setTotalItems(10);
        task1.setCompletedItems(8);

        when(taskRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
                .thenReturn(List.of(task1));

        double rate = planGenerationService.getRecentCompletionRate(userId, 7);

        assertEquals(0.8, rate);
    }

    @Test
    @DisplayName("调整计划 - 完成率低")
    void testAdjustPlan_LowCompletion() {
        when(planRepository.findCurrentByUserId(userId)).thenReturn(Optional.of(testPlan));
        when(planOptimizerEngine.adjustTaskDifficulty(anyDouble(), anyInt())).thenReturn(16);
        when(planRepository.update(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());

        StudyPlan result = planGenerationService.adjustPlan(userId);

        assertNotNull(result);
        verify(planRepository).update(any(StudyPlan.class));
        assertEquals(16, result.getDailyTaskCount());
    }

    @Test
    @DisplayName("调整计划 - 完成率高")
    void testAdjustPlan_HighCompletion() {
        testPlan.setDailyTaskCount(15);
        when(planRepository.findCurrentByUserId(userId)).thenReturn(Optional.of(testPlan));
        when(planOptimizerEngine.adjustTaskDifficulty(anyDouble(), anyInt())).thenReturn(17);
        when(planRepository.update(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());

        StudyPlan result = planGenerationService.adjustPlan(userId);

        assertNotNull(result);
        verify(planRepository).update(any(StudyPlan.class));
        assertEquals(17, result.getDailyTaskCount());
    }
}
