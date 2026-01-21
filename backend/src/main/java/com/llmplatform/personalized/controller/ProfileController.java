package com.llmplatform.personalized.controller;

import com.llmplatform.common.Result;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.service.ProfileAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学习档案控制器
 * 提供学习档案的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController extends BaseController {

    private final ProfileAnalysisService profileAnalysisService;

    /**
     * 获取学习档案
     * GET /api/profile
     */
    @GetMapping
    public Result<LearningProfile> getProfile() {
        LearningProfile profile = profileAnalysisService.getOrCreateProfile(requireUserId());
        return Result.success(profile);
    }

    /**
     * 触发档案分析
     * POST /api/profile/analyze
     */
    @PostMapping("/analyze")
    public Result<LearningProfile> analyzeProfile(@RequestBody Map<String, Object> request) {
        Long userId = requireUserId();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recordData = (List<Map<String, Object>>) request.get("records");
        List<LearningRecord> records = convertToLearningRecords(recordData);

        LearningProfile profile = profileAnalysisService.updateProfile(userId, records);
        return Result.success(profile);
    }

    /**
     * 获取学习洞察报告
     * GET /api/profile/insights
     */
    @GetMapping("/insights")
    public Result<Map<String, Object>> getInsights() {
        Long userId = requireUserId();
        Map<String, Object> insights = profileAnalysisService.generateInsightReport(userId);

        if (insights.containsKey("error")) {
            return Result.fail(insights.get("error").toString());
        }

        return Result.success(insights);
    }

    /**
     * 获取学习时间偏好
     * GET /api/profile/time-preferences
     */
    @GetMapping("/time-preferences")
    public Result<Map<String, Double>> getTimePreferences() {
        Map<String, Double> preferences = profileAnalysisService.analyzeTimePreferences(requireUserId());
        return Result.success(preferences);
    }

    /**
     * 获取薄弱领域
     * GET /api/profile/weak-areas
     */
    @GetMapping("/weak-areas")
    public Result<List<Map<String, Object>>> getWeakAreas() {
        List<Map<String, Object>> weakAreas = profileAnalysisService.identifyWeakAreas(requireUserId());
        return Result.success(weakAreas);
    }

    /**
     * 获取学习速度趋势
     * GET /api/profile/speed-trend
     */
    @GetMapping("/speed-trend")
    public Result<Map<String, Object>> getSpeedTrend() {
        Long userId = requireUserId();
        String trend = profileAnalysisService.getLearningSpeedTrend(userId);
        return Result.success(Map.of("userId", userId, "speedTrend", trend));
    }

    @SuppressWarnings("unchecked")
    private List<LearningRecord> convertToLearningRecords(List<Map<String, Object>> recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return List.of();
        }

        return recordData.stream()
                .map(this::convertToLearningRecord)
                .toList();
    }

    private LearningRecord convertToLearningRecord(Map<String, Object> data) {
        LearningRecord record = new LearningRecord();
        record.setId(getLong(data.get("id")));
        record.setUserId(getLong(data.get("userId")));
        record.setActivityType(getString(data.get("activityType")));
        record.setActivityId(getLong(data.get("activityId")));
        record.setActivityTime(parseDateTime(data.get("activityTime")));
        return record;
    }

    private Long getLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getString(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            log.warn("解析活动时间失败: {}", value);
            return null;
        }
    }
}
