package com.llmplatform.personalized.property;

import com.llmplatform.personalized.engine.PlanOptimizerEngine;
import com.llmplatform.personalized.engine.impl.PlanOptimizerEngineImpl;
import com.llmplatform.personalized.entity.LearningProfile;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlanOptimizerEngine 属性测试
 */
class PlanOptimizerEnginePropertyTest {

    private PlanOptimizerEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PlanOptimizerEngineImpl();
    }

    // Feature: personalized-learning-system, Property 13: 学习路径目标匹配
    @Property
    @DisplayName("学习路径目标匹配")
    void learningPathMatchesGoalType(
            @ForAll String goalType,
            @ForAll LocalDate targetDate,
            @ForAll String currentLevel) {

        // 有效的目标类型应该返回包含相关词汇集的学习路径
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(goalType, targetDate, currentLevel);

        assertNotNull(path);
        assertNotNull(path.wordSets());
        assertNotNull(path.priorities());
        assertTrue(path.totalWords() >= 0);

        // 路径中的词汇集数量应该与优先级数量一致
        assertEquals(path.wordSets().size(), path.priorities().size());
    }

    // Feature: personalized-learning-system, Property 13: EXAM目标包含学术词汇
    @Property
    @DisplayName("EXAM目标包含学术词汇集")
    void examGoalContainsAcademicWords() {
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(
                "EXAM", LocalDate.now().plusDays(30), "BEGINNER");

        boolean hasAcademic = path.wordSets().stream()
                .anyMatch(ws -> ws.category().equals("ACADEMIC") || ws.name().contains("学术"));

        assertTrue(hasAcademic, "EXAM目标应该包含学术词汇集");
    }

    // Feature: personalized-learning-system, Property 13: TRAVEL目标包含旅行场景
    @Property
    @DisplayName("TRAVEL目标包含旅行场景词汇集")
    void travelGoalContainsTravelWords() {
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(
                "TRAVEL", LocalDate.now().plusDays(14), "BEGINNER");

        boolean hasTravel = path.wordSets().stream()
                .anyMatch(ws -> ws.category().equals("TRANSPORTATION")
                        || ws.category().equals("ACCOMMODATION")
                        || ws.name().contains("交通")
                        || ws.name().contains("住宿"));

        assertTrue(hasTravel, "TRAVEL目标应该包含交通或住宿词汇集");
    }

    // Feature: personalized-learning-system, Property 13: BUSINESS目标包含商务词汇
    @Property
    @DisplayName("BUSINESS目标包含商务词汇集")
    void businessGoalContainsBusinessWords() {
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(
                "BUSINESS", LocalDate.now().plusDays(60), "INTERMEDIATE");

        boolean hasBusiness = path.wordSets().stream()
                .anyMatch(ws -> ws.category().equals("EMAIL")
                        || ws.category().equals("MEETING")
                        || ws.name().contains("邮件")
                        || ws.name().contains("会议"));

        assertTrue(hasBusiness, "BUSINESS目标应该包含邮件或会议词汇集");
    }

    // Feature: personalized-learning-system, Property 14: 每日任务量边界
    @Property
    @DisplayName("每日任务量在10-50范围内")
    void dailyTaskCountWithinBounds(
            @ForAll LearningProfile profile,
            @ForAll @IntRange(min = 1, max = 365) int remainingDays,
            @ForAll @IntRange(min = 100, max = 5000) int targetWordCount) {

        int count = engine.calculateDailyTaskCount(profile, remainingDays, targetWordCount);

        assertTrue(count >= 10 && count <= 50,
                "每日任务量应该在10-50范围内，实际: " + count);
    }

    // Feature: personalized-learning-system, Property 14: 每日任务量边界边界测试
    @Property
    @DisplayName("每日任务量边界值测试")
    void dailyTaskCountBoundaryTest(
            @ForAll LearningProfile profile,
            @ForAll @IntRange(min = 1, max = 365) int remainingDays,
            @ForAll @IntRange(min = 10, max = 5000) int targetWordCount) {

        int count = engine.calculateDailyTaskCount(profile, remainingDays, targetWordCount);

        assertTrue(count >= 10, "每日任务量最小为10，实际: " + count);
        assertTrue(count <= 50, "每日任务量最大为50，实际: " + count);
    }

    // Feature: personalized-learning-system, Property 15: 薄弱领域任务比例
    @Property
    @DisplayName("学习速度影响每日任务量")
    void learningSpeedAffectsDailyTaskCount(
            @ForAll @IntRange(min = 1, max = 30) int remainingDays,
            @ForAll @IntRange(min = 300, max = 600) int targetWordCount) {

        // FAST学习者
        LearningProfile fastProfile = new LearningProfile();
        fastProfile.setLearningSpeedTrend("FAST");

        // SLOW学习者
        LearningProfile slowProfile = new LearningProfile();
        slowProfile.setLearningSpeedTrend("SLOW");

        int fastCount = engine.calculateDailyTaskCount(fastProfile, remainingDays, targetWordCount);
        int slowCount = engine.calculateDailyTaskCount(slowProfile, remainingDays, targetWordCount);

        // FAST学习者的每日任务量应该 >= SLOW学习者
        assertTrue(fastCount >= slowCount,
                "FAST学习者(" + fastCount + ")的任务量应该 >= SLOW学习者(" + slowCount + ")");
    }

    // Feature: personalized-learning-system, Property 15: 任务难度调整-完成率低
    @Property
    @DisplayName("完成率低于60%减少任务量")
    void lowCompletionRateReducesTasks(
            @ForAll @IntRange(min = 10, max = 50) int currentCount,
            @ForAll @DoubleRange(min = 0.0, max = 0.59) double lowRate) {

        int adjusted = engine.adjustTaskDifficulty(lowRate, currentCount);

        assertTrue(adjusted < currentCount,
                "完成率低时任务量应该减少: " + currentCount + " -> " + adjusted);
        assertTrue(adjusted >= 10, "调整后任务量最小为10");
    }

    // Feature: personalized-learning-system, Property 15: 任务难度调整-完成率高
    @Property
    @DisplayName("完成率高于90%增加任务量")
    void highCompletionRateIncreasesTasks(
            @ForAll @IntRange(min = 10, max = 45) int currentCount,
            @ForAll @DoubleRange(min = 0.91, max = 1.0) double highRate) {

        int adjusted = engine.adjustTaskDifficulty(highRate, currentCount);

        assertTrue(adjusted > currentCount,
                "完成率高时任务量应该增加: " + currentCount + " -> " + adjusted);
        assertTrue(adjusted <= 50, "调整后任务量最大为50");
    }

    // Feature: personalized-learning-system, Property 15: 任务难度调整-完成率正常
    @Property
    @DisplayName("完成率60%-90%之间保持任务量")
    void normalCompletionRateKeepsTasks(
            @ForAll @IntRange(min = 10, max = 50) int currentCount,
            @ForAll @DoubleRange(min = 0.60, max = 0.90) double normalRate) {

        int adjusted = engine.adjustTaskDifficulty(normalRate, currentCount);

        assertEquals(currentCount, adjusted,
                "完成率正常时任务量应保持不变: " + currentCount);
    }

    // Feature: personalized-learning-system, Property 15: 任务量调整边界
    @Property
    @DisplayName("任务量调整后仍在10-50范围内")
    void adjustedTaskCountWithinBounds(
            @ForAll @IntRange(min = 10, max = 50) int currentCount,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double completionRate) {

        int adjusted = engine.adjustTaskDifficulty(completionRate, currentCount);

        assertTrue(adjusted >= 10 && adjusted <= 50,
                "调整后任务量应在10-50范围内，实际: " + adjusted);
    }

    // Additional edge case tests
    @Test
    @DisplayName("无效目标类型使用默认路径")
    void invalidGoalTypeUsesDefaultPath() {
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(
                "INVALID", LocalDate.now().plusDays(30), "BEGINNER");

        assertNotNull(path);
        assertFalse(path.wordSets().isEmpty());
    }

    @Test
    @DisplayName("剩余天数为零使用最小任务量")
    void zeroRemainingDaysUsesMinimumTasks() {
        LearningProfile profile = new LearningProfile();
        profile.setLearningSpeedTrend("FAST");

        int count = engine.calculateDailyTaskCount(profile, 0, 1000);

        assertEquals(10, count);
    }

    @Test
    @DisplayName("目标词汇数为零使用最小任务量")
    void zeroTargetWordsUsesMinimumTasks() {
        LearningProfile profile = new LearningProfile();
        profile.setLearningSpeedTrend("FAST");

        int count = engine.calculateDailyTaskCount(profile, 30, 0);

        assertEquals(10, count);
    }

    @Test
    @DisplayName("当前任务量超出范围保持不变")
    void outOfRangeTaskCountStaysSame() {
        int lowResult = engine.adjustTaskDifficulty(0.8, 5);
        int highResult = engine.adjustTaskDifficulty(0.8, 60);

        assertEquals(5, lowResult);
        assertEquals(60, highResult);
    }

    @Test
    @DisplayName("学习路径总词汇数计算正确")
    void totalWordsCalculationCorrect() {
        PlanOptimizerEngine.LearningPath path = engine.generateLearningPath(
                "DAILY", LocalDate.now().plusDays(30), "BEGINNER");

        int expectedTotal = path.wordSets().stream()
                .mapToInt(ws -> ws.wordIds().size())
                .sum();

        assertEquals(expectedTotal, path.totalWords());
    }
}
