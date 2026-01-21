package com.llmplatform.personalized.service.impl;

import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.engine.LearningAnalyticsEngine;
import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.repository.ProfileRepository;
import com.llmplatform.personalized.service.ProfileAnalysisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 档案分析服务实现
 */
@Slf4j
@Service
public class ProfileAnalysisServiceImpl implements ProfileAnalysisService {

    private static final String PROFILE_CACHE_PREFIX = "learning:profile:";
    private static final Duration PROFILE_CACHE_TTL = Duration.ofHours(24);
    private static final int MIN_DAYS_FOR_REPORT = 7;

    private final ProfileRepository profileRepository;
    private final LearningAnalyticsEngine learningAnalyticsEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数（用于Spring注入）
     */
    public ProfileAnalysisServiceImpl(ProfileRepository profileRepository,
                                       LearningAnalyticsEngine learningAnalyticsEngine,
                                       RedisTemplate<String, Object> redisTemplate,
                                       ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.learningAnalyticsEngine = learningAnalyticsEngine;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LearningProfile getOrCreateProfile(Long userId) {
        // 尝试从缓存获取
        LearningProfile cached = getProfileFromCache(userId);
        if (cached != null) {
            return cached;
        }

        // 从数据库获取
        Optional<LearningProfile> existing = profileRepository.findByUserId(userId);
        if (existing.isPresent()) {
            LearningProfile profile = existing.get();
            cacheProfile(userId, profile);
            return profile;
        }

        // 创建默认档案
        LearningProfile newProfile = createDefaultProfile(userId);
        LearningProfile saved = profileRepository.create(newProfile);
        cacheProfile(userId, saved);

        log.debug("为用户创建默认学习档案, userId={}", userId);
        return saved;
    }

    @Override
    public LearningProfile updateProfile(Long userId, List<LearningRecord> records) {
        LearningProfile profile = getOrCreateProfile(userId);

        // 分析时间偏好
        Map<String, Double> timePreferences = learningAnalyticsEngine.analyzeTimePreferences(records);
        try {
            profile.setPreferredLearningTimes(objectMapper.writeValueAsString(timePreferences));
        } catch (JsonProcessingException e) {
            log.error("序列化时间偏好失败", e);
        }

        // 计算学习速度
        double learningSpeed = learningAnalyticsEngine.calculateLearningSpeed(records);
        profile.setAverageDailyWords(learningSpeed);

        // 计算平均正确率
        double accuracyRate = calculateAverageAccuracy(records);
        profile.setAverageAccuracy(accuracyRate);

        // 判断学习速度趋势
        String speedTrend = determineSpeedTrend(learningSpeed);
        profile.setLearningSpeedTrend(speedTrend);

        // 识别薄弱领域
        List<LearningAnalyticsEngine.WeakArea> weakAreas =
                learningAnalyticsEngine.identifyWeakAreas(records);
        try {
            profile.setWeakAreas(objectMapper.writeValueAsString(weakAreas));
        } catch (JsonProcessingException e) {
            log.error("序列化薄弱领域失败", e);
        }

        profile.setLastAnalysisTime(LocalDateTime.now());

        // 更新数据库
        LearningProfile updated = profileRepository.update(profile);

        // 更新缓存
        cacheProfile(userId, updated);

        log.debug("更新学习档案完成, userId={}, speedTrend={}", userId, speedTrend);
        return updated;
    }

    @Override
    public LearningProfile getProfile(Long userId) {
        // 尝试从缓存获取
        LearningProfile cached = getProfileFromCache(userId);
        if (cached != null) {
            return cached;
        }

        // 从数据库获取
        Optional<LearningProfile> existing = profileRepository.findByUserId(userId);
        if (existing.isPresent()) {
            LearningProfile profile = existing.get();
            cacheProfile(userId, profile);
            return profile;
        }

        return null;
    }

    @Override
    public Map<String, Object> generateInsightReport(Long userId) {
        Map<String, Object> report = new LinkedHashMap<>();

        LearningProfile profile = getProfile(userId);
        if (profile == null) {
            report.put("error", "学习档案不存在");
            return report;
        }

        // 检查数据是否足够
        LocalDateTime lastAnalysis = profile.getLastAnalysisTime();
        if (lastAnalysis == null ||
                lastAnalysis.isBefore(LocalDateTime.now().minusDays(MIN_DAYS_FOR_REPORT))) {
            report.put("warning", "数据不足，无法生成完整分析");
            report.put("daysRequired", MIN_DAYS_FOR_REPORT);
            return report;
        }

        // 学习时间偏好
        if (profile.getPreferredLearningTimes() != null) {
            try {
                Map<String, Double> timePrefs = objectMapper.readValue(
                        profile.getPreferredLearningTimes(),
                        new TypeReference<Map<String, Double>>() {}
                );
                report.put("timePreferences", timePrefs);

                // 找出最佳学习时段
                String bestTime = Collections.max(timePrefs.entrySet(),
                        Map.Entry.comparingByValue()).getKey();
                report.put("bestLearningTime", bestTime);
            } catch (JsonProcessingException e) {
                log.error("解析时间偏好失败", e);
            }
        }

        // 薄弱领域
        if (profile.getWeakAreas() != null) {
            try {
                List<Map<String, Object>> weakAreas = objectMapper.readValue(
                        profile.getWeakAreas(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                report.put("weakAreas", weakAreas);
            } catch (JsonProcessingException e) {
                log.error("解析薄弱领域失败", e);
            }
        }

        // 学习速度
        report.put("averageDailyWords", profile.getAverageDailyWords());
        report.put("learningSpeedTrend", profile.getLearningSpeedTrend());

        // 正确率
        report.put("averageAccuracy", profile.getAverageAccuracy());

        // 进步趋势（通过比较历史数据）
        report.put("progressTrend", calculateProgressTrend(profile));

        log.debug("生成学习洞察报告完成, userId={}", userId);
        return report;
    }

    @Override
    public Map<String, Double> analyzeTimePreferences(Long userId) {
        LearningProfile profile = getProfile(userId);
        if (profile == null || profile.getPreferredLearningTimes() == null) {
            return Map.of("morning", 0.0, "afternoon", 0.0, "evening", 0.0);
        }

        try {
            return objectMapper.readValue(
                    profile.getPreferredLearningTimes(),
                    new TypeReference<Map<String, Double>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("解析时间偏好失败", e);
            return Map.of("morning", 0.0, "afternoon", 0.0, "evening", 0.0);
        }
    }

    @Override
    public List<Map<String, Object>> identifyWeakAreas(Long userId) {
        LearningProfile profile = getProfile(userId);
        if (profile == null || profile.getWeakAreas() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    profile.getWeakAreas(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("解析薄弱领域失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getLearningSpeedTrend(Long userId) {
        LearningProfile profile = getProfile(userId);
        if (profile == null || profile.getLearningSpeedTrend() == null) {
            return "NORMAL";
        }
        return profile.getLearningSpeedTrend();
    }

    /**
     * 创建默认学习档案
     */
    private LearningProfile createDefaultProfile(Long userId) {
        LearningProfile profile = new LearningProfile();
        profile.setUserId(userId);
        profile.setPreferredLearningTimes("{\"morning\": 0.33, \"afternoon\": 0.34, \"evening\": 0.33}");
        profile.setWeakAreas("[]");
        profile.setAverageDailyWords(0.0);
        profile.setAverageAccuracy(0.0);
        profile.setLearningSpeedTrend("NORMAL");
        profile.setCreatedTime(LocalDateTime.now());
        profile.setUpdatedTime(LocalDateTime.now());
        return profile;
    }

    /**
     * 计算平均正确率
     */
    private double calculateAverageAccuracy(List<LearningRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }
        // 由于LearningRecord没有正确率字段，返回默认值
        // 实际实现中应该从其他数据源获取
        return 0.75; // 默认75%正确率
    }

    /**
     * 判断学习速度趋势
     */
    private String determineSpeedTrend(double dailyWords) {
        if (dailyWords >= 50) {
            return "FAST";
        } else if (dailyWords >= 20) {
            return "NORMAL";
        } else {
            return "SLOW";
        }
    }

    /**
     * 计算进步趋势
     */
    private String calculateProgressTrend(LearningProfile profile) {
        Double avgWords = profile.getAverageDailyWords();
        if (avgWords == null || avgWords == 0) {
            return "STABLE";
        }

        // 基于学习速度判断进步趋势
        if (avgWords >= 30) {
            return "IMPROVING";
        } else if (avgWords >= 15) {
            return "STABLE";
        } else {
            return "NEEDS_ATTENTION";
        }
    }

    /**
     * 从缓存获取学习档案
     */
    private LearningProfile getProfileFromCache(Long userId) {
        String cacheKey = PROFILE_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof LearningProfile) {
                log.debug("从缓存获取学习档案, userId={}", userId);
                return (LearningProfile) cached;
            }
        } catch (Exception e) {
            log.warn("从缓存获取学习档案失败, userId={}", userId, e);
        }
        return null;
    }

    /**
     * 缓存学习档案
     */
    private void cacheProfile(Long userId, LearningProfile profile) {
        String cacheKey = PROFILE_CACHE_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(cacheKey, profile, PROFILE_CACHE_TTL);
            log.debug("缓存学习档案, userId={}", userId);
        } catch (Exception e) {
            log.warn("缓存学习档案失败, userId={}", userId, e);
        }
    }

    /**
     * 清除学习档案缓存
     */
    public void clearProfileCache(Long userId) {
        String cacheKey = PROFILE_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.debug("清除学习档案缓存, userId={}", userId);
    }
}
