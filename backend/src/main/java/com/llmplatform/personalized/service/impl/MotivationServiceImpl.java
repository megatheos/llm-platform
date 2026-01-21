package com.llmplatform.personalized.service.impl;

import com.llmplatform.personalized.entity.Achievement;
import com.llmplatform.personalized.entity.LearningStreak;
import com.llmplatform.personalized.entity.UserAchievement;
import com.llmplatform.personalized.mapper.LearningStreakMapper;
import com.llmplatform.personalized.repository.AchievementRepository;
import com.llmplatform.personalized.repository.MemoryRepository;
import com.llmplatform.personalized.service.MotivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 激励服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MotivationServiceImpl implements MotivationService {

    private static final String STREAK_CACHE_PREFIX = "learning:streak:";
    private static final String ACHIEVEMENTS_CACHE_PREFIX = "learning:achievements:";

    private final AchievementRepository achievementRepository;
    private final MemoryRepository memoryRepository;
    private final LearningStreakMapper learningStreakMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 预定义成就列表
    private static final List<Achievement> DEFAULT_ACHIEVEMENTS = Arrays.asList(
            createAchievement("STREAK_7", "坚持一周", "连续学习7天", "streak", 7),
            createAchievement("STREAK_30", "坚持一月", "连续学习30天", "streak", 30),
            createAchievement("STREAK_100", "百日坚持", "连续学习100天", "streak", 100),
            createAchievement("WORDS_100", "百词斩", "掌握100个词汇", "milestone", 100),
            createAchievement("WORDS_500", "词汇达人", "掌握500个词汇", "milestone", 500),
            createAchievement("WORDS_1000", "词汇大师", "掌握1000个词汇", "milestone", 1000),
            createAchievement("ACCURACY_90", "精准学习", "正确率达到90%", "mastery", 90),
            createAchievement("REVIEW_100", "复习达人", "完成100次复习", "milestone", 100)
    );

    @Override
    public LearningStreak updateStreak(Long userId, LocalDate learningDate) {
        LearningStreak streak = achievementRepository.findStreakByUserId(userId)
                .orElseGet(() -> createNewStreak(userId));

        LocalDate lastLearningDate = streak.getLastLearningDate();

        if (lastLearningDate == null) {
            // 首次学习
            streak.setCurrentStreak(1);
        } else if (lastLearningDate.equals(learningDate)) {
            // 同一天学习，不更新
            log.debug("同日学习，不更新连续记录: userId={}, date={}", userId, learningDate);
            return streak;
        } else if (lastLearningDate.plusDays(1).equals(learningDate)) {
            // 连续第二天
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // 中断了，重置
            log.info("学习连续中断: userId={}, lastDate={}, newDate={}",
                    userId, lastLearningDate, learningDate);
            streak.setCurrentStreak(1);
        }

        // 更新最长记录
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastLearningDate(learningDate);
        LearningStreak updatedStreak = achievementRepository.updateStreak(streak);

        // 清除缓存
        clearStreakCache(userId);

        return updatedStreak;
    }

    private LearningStreak createNewStreak(Long userId) {
        LearningStreak streak = new LearningStreak();
        streak.setUserId(userId);
        streak.setCurrentStreak(0);
        streak.setLongestStreak(0);
        learningStreakMapper.insert(streak);
        return streak;
    }

    @Override
    public List<Achievement> checkAndGrantAchievements(Long userId) {
        List<Achievement> newAchievements = new ArrayList<>();

        // 获取用户连续记录
        LearningStreak streak = achievementRepository.findStreakByUserId(userId)
                .orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;

        // 获取用户掌握的词汇数
        long masteredWords = memoryRepository.countMasteredByUserId(userId);

        // 获取用户总复习次数
        var stats = memoryRepository.getStatisticsByUserId(userId);
        int totalReviews = stats != null && stats.getTotalReviews() != null ?
                stats.getTotalReviews() : 0;

        // 获取平均正确率
        double accuracy = stats != null && stats.getAccuracyRate() != null ?
                stats.getAccuracyRate() * 100 : 0;

        // 检查成就
        checkStreakAchievements(userId, currentStreak, newAchievements);
        checkWordAchievements(userId, masteredWords, newAchievements);
        checkReviewAchievements(userId, totalReviews, newAchievements);
        checkAccuracyAchievements(userId, accuracy, newAchievements);

        if (!newAchievements.isEmpty()) {
            log.info("用户解锁新成就: userId={}, achievements={}",
                    userId, newAchievements.stream().map(Achievement::getCode).toList());
        }

        return newAchievements;
    }

    private void checkStreakAchievements(Long userId, int currentStreak, List<Achievement> newAchievements) {
        for (Achievement achievement : DEFAULT_ACHIEVEMENTS) {
            if ("streak".equals(achievement.getCategory()) &&
                    currentStreak >= achievement.getRequiredValue()) {
                if (tryGrantAchievement(userId, achievement)) {
                    newAchievements.add(achievement);
                }
            }
        }
    }

    private void checkWordAchievements(Long userId, long masteredWords, List<Achievement> newAchievements) {
        for (Achievement achievement : DEFAULT_ACHIEVEMENTS) {
            if ("milestone".equals(achievement.getCategory()) &&
                    achievement.getRequiredValue() <= 500 &&
                    masteredWords >= achievement.getRequiredValue()) {
                if (tryGrantAchievement(userId, achievement)) {
                    newAchievements.add(achievement);
                }
            }
        }
    }

    private void checkReviewAchievements(Long userId, int totalReviews, List<Achievement> newAchievements) {
        for (Achievement achievement : DEFAULT_ACHIEVEMENTS) {
            if ("REVIEW_100".equals(achievement.getCode()) &&
                    totalReviews >= achievement.getRequiredValue()) {
                if (tryGrantAchievement(userId, achievement)) {
                    newAchievements.add(achievement);
                }
            }
        }
    }

    private void checkAccuracyAchievements(Long userId, double accuracy, List<Achievement> newAchievements) {
        for (Achievement achievement : DEFAULT_ACHIEVEMENTS) {
            if ("ACCURACY_90".equals(achievement.getCode()) &&
                    accuracy >= achievement.getRequiredValue()) {
                if (tryGrantAchievement(userId, achievement)) {
                    newAchievements.add(achievement);
                }
            }
        }
    }

    private boolean tryGrantAchievement(Long userId, Achievement achievement) {
        Achievement fullAchievement = achievementRepository.findByCode(achievement.getCode())
                .orElse(achievement);

        if (achievementRepository.hasUserAchievement(userId, fullAchievement.getId())) {
            return false;
        }

        achievementRepository.grantAchievement(userId, fullAchievement.getId());
        return true;
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return achievementRepository.findUserAchievements(userId);
    }

    @Override
    public LearningStreak getStreak(Long userId) {
        String cacheKey = STREAK_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof LearningStreak) {
                return (LearningStreak) cached;
            }
        } catch (Exception e) {
            log.warn("获取连续记录缓存失败", e);
        }

        LearningStreak streak = achievementRepository.findStreakByUserId(userId).orElse(null);
        if (streak != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, streak);
            } catch (Exception e) {
                log.warn("缓存连续记录失败", e);
            }
        }

        return streak;
    }

    @Override
    public Map<String, Object> getProgressVisualization(Long userId) {
        Map<String, Object> progress = new LinkedHashMap<>();

        LearningStreak streak = getStreak(userId);
        progress.put("currentStreak", streak != null ? streak.getCurrentStreak() : 0);
        progress.put("longestStreak", streak != null ? streak.getLongestStreak() : 0);
        progress.put("lastLearningDate", streak != null ? streak.getLastLearningDate() : null);

        progress.put("learningCalendar", generateLearningCalendar(userId));
        progress.put("achievements", getAchievementProgress(userId));

        var stats = memoryRepository.getStatisticsByUserId(userId);
        if (stats != null) {
            progress.put("totalWords", stats.getTotalWords());
            progress.put("masteredWords", stats.getMasteredWords());
            progress.put("masteryProgress",
                    stats.getTotalWords() != null && stats.getTotalWords() > 0 ?
                            (double) stats.getMasteredWords() / stats.getTotalWords() : 0.0);
        } else {
            progress.put("totalWords", 0);
            progress.put("masteredWords", 0);
            progress.put("masteryProgress", 0.0);
        }

        return progress;
    }

    private Map<String, Object> generateLearningCalendar(Long userId) {
        Map<String, Object> calendar = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            boolean learned = false;

            LearningStreak streak = achievementRepository.findStreakByUserId(userId).orElse(null);
            if (streak != null && streak.getLastLearningDate() != null) {
                learned = !streak.getLastLearningDate().isBefore(date);
            }

            calendar.put(dateStr, Map.of("date", dateStr, "learned", learned, "intensity", learned ? 1 : 0));
        }

        return calendar;
    }

    private Map<String, Object> getAchievementProgress(Long userId) {
        Map<String, Object> progress = new LinkedHashMap<>();
        List<Achievement> allAchievements = getAllAchievements();
        List<UserAchievement> userAchievements = getUserAchievements(userId);

        Set<Long> unlockedIds = new HashSet<>();
        userAchievements.forEach(ua -> unlockedIds.add(ua.getAchievementId()));

        int unlocked = 0;
        for (Achievement achievement : allAchievements) {
            Map<String, Object> achievementProgress = new LinkedHashMap<>();
            achievementProgress.put("code", achievement.getCode());
            achievementProgress.put("name", achievement.getName());
            achievementProgress.put("category", achievement.getCategory());
            achievementProgress.put("unlocked", unlockedIds.contains(achievement.getId()));

            if (unlockedIds.contains(achievement.getId())) {
                unlocked++;
            }

            progress.put(achievement.getCode(), achievementProgress);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", allAchievements.size());
        summary.put("unlocked", unlocked);
        summary.put("progress", (double) unlocked / allAchievements.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievements", progress);
        result.put("summary", summary);

        return result;
    }

    @Override
    public List<Achievement> getAllAchievements() {
        String cacheKey = ACHIEVEMENTS_CACHE_PREFIX + "all";
        try {
            @SuppressWarnings("unchecked")
            List<Achievement> cached = (List<Achievement>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("获取成就缓存失败", e);
        }

        List<Achievement> achievements = achievementRepository.findAll();
        if (achievements.isEmpty()) {
            initializeDefaultAchievements();
            achievements = achievementRepository.findAll();
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, achievements);
        } catch (Exception e) {
            log.warn("缓存成就失败", e);
        }

        return achievements;
    }

    @Override
    public void initializeDefaultAchievements() {
        for (Achievement achievement : DEFAULT_ACHIEVEMENTS) {
            if (achievementRepository.findByCode(achievement.getCode()).isEmpty()) {
                achievementRepository.create(achievement);
                log.info("初始化成就: {}", achievement.getCode());
            }
        }
    }

    private static Achievement createAchievement(String code, String name, String description,
                                                   String category, int requiredValue) {
        Achievement achievement = new Achievement();
        achievement.setCode(code);
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setCategory(category);
        achievement.setRequiredValue(requiredValue);
        achievement.setIconUrl("/icons/" + code.toLowerCase() + ".png");
        return achievement;
    }

    private void clearStreakCache(Long userId) {
        String cacheKey = STREAK_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}
