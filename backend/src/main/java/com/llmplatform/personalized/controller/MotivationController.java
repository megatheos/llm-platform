package com.llmplatform.personalized.controller;

import com.llmplatform.common.Result;
import com.llmplatform.personalized.entity.Achievement;
import com.llmplatform.personalized.entity.LearningStreak;
import com.llmplatform.personalized.entity.UserAchievement;
import com.llmplatform.personalized.service.MotivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 激励系统控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/motivation")
@RequiredArgsConstructor
public class MotivationController extends BaseController {

    private final MotivationService motivationService;

    /**
     * 获取学习连续记录
     * GET /api/motivation/streak
     */
    @GetMapping("/streak")
    public Result<LearningStreak> getStreak() {
        LearningStreak streak = motivationService.getStreak(requireUserId());
        return Result.success(streak != null ? streak : new LearningStreak());
    }

    /**
     * 更新学习连续记录
     * POST /api/motivation/streak
     */
    @PostMapping("/streak")
    public Result<LearningStreak> updateStreak(
            @RequestParam(required = false) String dateStr) {

        Long userId = requireUserId();
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        LearningStreak streak = motivationService.updateStreak(userId, date);

        List<Achievement> newAchievements = motivationService.checkAndGrantAchievements(userId);
        if (!newAchievements.isEmpty()) {
            log.info("用户解锁新成就: userId={}, achievements={}",
                    userId, newAchievements.stream().map(Achievement::getCode).toList());
        }

        return Result.success(streak);
    }

    /**
     * 获取用户成就列表
     * GET /api/motivation/achievements
     */
    @GetMapping("/achievements")
    public Result<List<UserAchievement>> getUserAchievements() {
        List<UserAchievement> achievements = motivationService.getUserAchievements(requireUserId());
        return Result.success(achievements);
    }

    /**
     * 获取所有可用成就
     * GET /api/motivation/achievements/all
     */
    @GetMapping("/achievements/all")
    public Result<List<Achievement>> getAllAchievements() {
        List<Achievement> achievements = motivationService.getAllAchievements();
        return Result.success(achievements);
    }

    /**
     * 检查新成就
     * POST /api/motivation/check-achievements
     */
    @PostMapping("/check-achievements")
    public Result<List<Achievement>> checkAchievements() {
        List<Achievement> newAchievements = motivationService.checkAndGrantAchievements(requireUserId());
        return Result.success(newAchievements);
    }

    /**
     * 获取进度可视化
     * GET /api/motivation/visualization
     */
    @GetMapping("/visualization")
    public Result<Map<String, Object>> getVisualization() {
        Map<String, Object> visualization = motivationService.getProgressVisualization(requireUserId());
        return Result.success(visualization);
    }

    /**
     * 初始化系统默认成就
     * POST /api/motivation/initialize
     */
    @PostMapping("/initialize")
    public Result<Map<String, Object>> initialize() {
        motivationService.initializeDefaultAchievements();
        List<Achievement> achievements = motivationService.getAllAchievements();
        return Result.success(Map.of(
                "message", "成就初始化完成",
                "count", achievements.size()
        ));
    }
}
