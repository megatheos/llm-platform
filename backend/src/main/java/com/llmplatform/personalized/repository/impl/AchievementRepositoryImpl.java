package com.llmplatform.personalized.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.personalized.entity.Achievement;
import com.llmplatform.personalized.entity.UserAchievement;
import com.llmplatform.personalized.entity.LearningStreak;
import com.llmplatform.personalized.mapper.AchievementMapper;
import com.llmplatform.personalized.mapper.UserAchievementMapper;
import com.llmplatform.personalized.mapper.LearningStreakMapper;
import com.llmplatform.personalized.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 成就数据访问仓库实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AchievementRepositoryImpl implements AchievementRepository {

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final LearningStreakMapper learningStreakMapper;

    @Override
    public List<Achievement> findAll() {
        return achievementMapper.selectList(null);
    }

    @Override
    public Optional<Achievement> findByCode(String code) {
        return Optional.ofNullable(achievementMapper.selectOne(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getCode, code)
        ));
    }

    @Override
    public Achievement create(Achievement achievement) {
        achievement.setCreatedTime(LocalDateTime.now());
        achievementMapper.insert(achievement);
        log.debug("创建成就成功: {}", achievement.getCode());
        return achievement;
    }

    @Override
    public List<UserAchievement> findUserAchievements(Long userId) {
        return userAchievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .orderByDesc(UserAchievement::getUnlockedTime)
        );
    }

    @Override
    public boolean hasUserAchievement(Long userId, Long achievementId) {
        return userAchievementMapper.selectCount(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .eq(UserAchievement::getAchievementId, achievementId)
        ) > 0;
    }

    @Override
    public UserAchievement grantAchievement(Long userId, Long achievementId) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUserId(userId);
        userAchievement.setAchievementId(achievementId);
        userAchievement.setUnlockedTime(LocalDateTime.now());
        userAchievement.setIsNotified(false);
        userAchievementMapper.insert(userAchievement);
        log.info("授予用户成就: userId={}, achievementId={}", userId, achievementId);
        return userAchievement;
    }

    @Override
    public Optional<LearningStreak> findStreakByUserId(Long userId) {
        return Optional.ofNullable(learningStreakMapper.selectOne(
                new LambdaQueryWrapper<LearningStreak>()
                        .eq(LearningStreak::getUserId, userId)
        ));
    }

    @Override
    public LearningStreak updateStreak(LearningStreak streak) {
        streak.setUpdatedTime(LocalDateTime.now());
        learningStreakMapper.updateById(streak);
        log.debug("更新连续学习记录: userId={}, currentStreak={}",
                streak.getUserId(), streak.getCurrentStreak());
        return streak;
    }
}
