package com.llmplatform.personalized.repository;

import com.llmplatform.personalized.entity.Achievement;
import com.llmplatform.personalized.entity.UserAchievement;
import com.llmplatform.personalized.entity.LearningStreak;

import java.util.List;
import java.util.Optional;

/**
 * 成就数据访问仓库接口
 */
public interface AchievementRepository {

    /**
     * 获取所有成就
     */
    List<Achievement> findAll();

    /**
     * 根据代码获取成就
     */
    Optional<Achievement> findByCode(String code);

    /**
     * 创建成就
     */
    Achievement create(Achievement achievement);

    /**
     * 获取用户已解锁的成就
     */
    List<UserAchievement> findUserAchievements(Long userId);

    /**
     * 检查用户是否已解锁某成就
     */
    boolean hasUserAchievement(Long userId, Long achievementId);

    /**
     * 授予用户成就
     */
    UserAchievement grantAchievement(Long userId, Long achievementId);

    /**
     * 获取用户的连续学习记录
     */
    Optional<LearningStreak> findStreakByUserId(Long userId);

    /**
     * 更新连续学习记录
     */
    LearningStreak updateStreak(LearningStreak streak);
}
