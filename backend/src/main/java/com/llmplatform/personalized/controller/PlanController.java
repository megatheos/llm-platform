package com.llmplatform.personalized.controller;

import com.llmplatform.common.Result;
import com.llmplatform.personalized.entity.DailyTask;
import com.llmplatform.personalized.entity.StudyPlan;
import com.llmplatform.personalized.service.PlanGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习计划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController extends BaseController {

    private final PlanGenerationService planGenerationService;

    /**
     * 创建学习计划
     * POST /api/plans
     */
    @PostMapping
    public Result<StudyPlan> createPlan(@RequestBody Map<String, Object> request) {
        Long userId = requireUserId();

        String goalType = getString(request, "goalType");
        String targetDateStr = getString(request, "targetDate");
        int targetWordCount = getInt(request, "targetWordCount", 1000);

        if (goalType == null || targetDateStr == null) {
            return Result.fail("goalType 和 targetDate 不能为空");
        }

        LocalDate targetDate = LocalDate.parse(targetDateStr);
        StudyPlan plan = planGenerationService.createStudyPlan(userId, goalType, targetDate, targetWordCount);
        return Result.success(plan);
    }

    /**
     * 获取当前计划
     * GET /api/plans/current
     */
    @GetMapping("/current")
    public Result<StudyPlan> getCurrentPlan() {
        StudyPlan plan = planGenerationService.getCurrentPlan(requireUserId());
        if (plan == null) {
            return Result.fail("用户没有学习计划");
        }
        return Result.success(plan);
    }

    /**
     * 触发计划调整
     * PUT /api/plans/adjust
     */
    @PutMapping("/adjust")
    public Result<StudyPlan> adjustPlan() {
        StudyPlan plan = planGenerationService.adjustPlan(requireUserId());
        return Result.success(plan);
    }

    /**
     * 获取每日任务
     * GET /api/plans/daily-tasks
     */
    @GetMapping("/daily-tasks")
    public Result<List<DailyTask>> getDailyTasks(
            @RequestParam(required = false) String dateStr) {

        Long userId = requireUserId();
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        List<DailyTask> tasks = planGenerationService.generateDailyTasks(userId, date);
        return Result.success(tasks);
    }

    /**
     * 完成任务
     * POST /api/plans/tasks/{id}/complete
     */
    @PostMapping("/tasks/{id}/complete")
    public Result<DailyTask> completeTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        int completedItems = getInt(request, "completedItems", 0);
        DailyTask task = planGenerationService.completeTask(id, completedItems);
        return Result.success(task);
    }

    /**
     * 获取完成率
     * GET /api/plans/completion-rate
     */
    @GetMapping("/completion-rate")
    public Result<Map<String, Object>> getCompletionRate(
            @RequestParam(defaultValue = "7") int days) {

        Long userId = requireUserId();
        double rate = planGenerationService.getRecentCompletionRate(userId, days);
        return Result.success(Map.of("userId", userId, "days", days, "completionRate", rate));
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
