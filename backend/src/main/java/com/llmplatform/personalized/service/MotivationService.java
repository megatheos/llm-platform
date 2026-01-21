package com.llmplatform.personalized.service;

import com.llmplatform.personalized.entity.Achievement;
import com.llmplatform.personalized.entity.LearningStreak;
import com.llmplatform.personalized.entity.UserAchievement;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 激励服务接口
 */
public interface MotivationService {

    /**
     * 更新学习连续记录
     *
     * @param userId 用户ID
     * @param learningDate 学习日期
     * @return 更新后的连续记录
     */
    LearningStreak updateStreak(Long userId, LocalDate learningDate);

    /**
     * 检查并授予成就
     *
     * @param userId 用户ID
     * @return 新解锁的成就列表
     */
    List<Achievement> checkAndGrantAchievements(Long userId);

    /**
     * 获取用户成就列表
     *
     * @param userId 用户ID
     * @return 用户成就列表
     */
    List<UserAchievement> getUserAchievements(Long userId);

    /**
     * 获取用户连续记录
     *
     * @param userId 用户ID
     * @return 连续记录
     */
    LearningStreak getStreak(Long userId);

    /**
     * 获取进度可视化数据
     *
     * @param userId 用户ID
     * @return 进度数据
     */
    Map<String, Object> getProgressVisualization(Long userId);

    /**
     * 获取所有可用成就
     *
     * @return 成就列表
     */
    List<Achievement> getAllAchievements();

    /**
     * 初始化系统默认成就
     */
    void initializeDefaultAchievements();
}
