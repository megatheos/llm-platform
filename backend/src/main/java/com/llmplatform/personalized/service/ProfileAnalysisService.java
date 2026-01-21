package com.llmplatform.personalized.service;

import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.entity.LearningProfile;

import java.util.List;
import java.util.Map;

/**
 * 档案分析服务接口
 * 提供学习档案的业务逻辑操作
 */
public interface ProfileAnalysisService {

    /**
     * 获取学习档案
     *
     * @param userId 用户ID
     * @return 学习档案（不存在则创建默认档案）
     */
    LearningProfile getOrCreateProfile(Long userId);

    /**
     * 更新学习档案
     *
     * @param userId 用户ID
     * @param records 学习记录列表
     * @return 更新后的学习档案
     */
    LearningProfile updateProfile(Long userId, List<LearningRecord> records);

    /**
     * 获取学习档案（仅查询）
     *
     * @param userId 用户ID
     * @return 学习档案（可能为空）
     */
    LearningProfile getProfile(Long userId);

    /**
     * 生成学习洞察报告
     *
     * @param userId 用户ID
     * @return 洞察报告数据
     */
    Map<String, Object> generateInsightReport(Long userId);

    /**
     * 分析学习时间偏好
     *
     * @param userId 用户ID
     * @return 时间段分布
     */
    Map<String, Double> analyzeTimePreferences(Long userId);

    /**
     * 识别薄弱领域
     *
     * @param userId 用户ID
     * @return 薄弱领域列表
     */
    List<Map<String, Object>> identifyWeakAreas(Long userId);

    /**
     * 获取学习速度趋势
     *
     * @param userId 用户ID
     * @return 学习速度趋势 (FAST, NORMAL, SLOW)
     */
    String getLearningSpeedTrend(Long userId);
}
