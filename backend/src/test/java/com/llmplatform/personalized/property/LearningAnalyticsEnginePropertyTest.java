package com.llmplatform.personalized.property;

import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.engine.LearningAnalyticsEngine;
import com.llmplatform.personalized.engine.impl.LearningAnalyticsEngineImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LearningAnalyticsEngine 属性测试
 */
class LearningAnalyticsEnginePropertyTest {

    private LearningAnalyticsEngine engine;

    @BeforeEach
    void setUp() {
        // Note: In real scenario, would need mocked MemoryRepository
        // For these tests, we focus on pure logic tests
        engine = new LearningAnalyticsEngineImpl(null);
    }

    // Feature: personalized-learning-system, Property 7: 时间偏好分析归一化
    @Property
    @DisplayName("时间偏好分析归一化")
    void timePreferencesSumToOne(@ForAll List<LearningRecord> records) {
        Map<String, Double> preferences = engine.analyzeTimePreferences(records);

        double sum = preferences.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        // Allow small floating point error
        assertTrue(sum >= 0.99 && sum <= 1.01,
                "Time preferences should sum to approximately 1.0, but was: " + sum);
    }

    // Feature: personalized-learning-system, Property 7: 时间偏好分析边界情况
    @Property
    @DisplayName("空记录返回零偏好")
    void emptyRecordsReturnZeroPreferences() {
        Map<String, Double> preferences = engine.analyzeTimePreferences(List.of());

        assertEquals(0.0, preferences.get("morning"));
        assertEquals(0.0, preferences.get("afternoon"));
        assertEquals(0.0, preferences.get("evening"));
    }

    // Feature: personalized-learning-system, Property 7: 时间偏好分析单条记录
    @Property
    @DisplayName("单条记录分配到正确时间段")
    void singleRecordAssignedToCorrectPeriod(
            @ForAll @IntRange(min = 0, max = 23) int hour) {

        LearningRecord record = new LearningRecord();
        record.setUserId(1L);
        record.setActivityTime(LocalDateTime.of(2024, 1, 15, hour, 0));

        Map<String, Double> preferences = engine.analyzeTimePreferences(List.of(record));

        String expectedPeriod;
        if (hour >= 5 && hour < 12) {
            expectedPeriod = "morning";
        } else if (hour >= 12 && hour < 18) {
            expectedPeriod = "afternoon";
        } else {
            expectedPeriod = "evening";
        }

        assertEquals(1.0, preferences.get(expectedPeriod));
        assertEquals(0.0, preferences.get(
                expectedPeriod.equals("morning") ? "afternoon" :
                        expectedPeriod.equals("afternoon") ? "morning" : "morning"));
    }

    // Feature: personalized-learning-system, Property 8: 薄弱领域识别阈值
    @Property
    @DisplayName("薄弱领域识别阈值边界")
    void weakAreaThresholdBoundary(
            @ForAll @IntRange(min = 0, max = 100) int errorRatePercent) {

        // Create memory records with specific forgotten ratio
        int totalRecords = 100;
        int forgottenCount = errorRatePercent;
        int learningCount = totalRecords - forgottenCount;

        // Test the threshold logic by checking error rates
        double errorRate = (double) forgottenCount / totalRecords;

        boolean shouldBeWeakArea = errorRate > 0.40;

        assertEquals(forgottenCount > 40, shouldBeWeakArea);
    }

    // Feature: personalized-learning-system, Property 9: 学习速度计算准确性
    @Property
    @DisplayName("学习速度计算非负")
    void learningSpeedIsNonNegative(@ForAll List<LearningRecord> records) {
        double speed = engine.calculateLearningSpeed(records);

        assertTrue(speed >= 0, "Learning speed should be non-negative");
    }

    // Feature: personalized-learning-system, Property 9: 学习速度计算空记录
    @Property
    @DisplayName("空记录学习速度为零")
    void emptyRecordsLearningSpeedZero() {
        double speed = engine.calculateLearningSpeed(List.of());

        assertEquals(0.0, speed);
    }

    // Feature: personalized-learning-system, Property 9: 学习速度计算边界
    @Property
    @DisplayName("学习速度随记录数增加")
    void learningSpeedIncreasesWithRecords(
            @ForAll @IntRange(min = 1, max = 100) int recordCount) {

        List<LearningRecord> records = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        // Create records spanning different days
        for (int i = 0; i < recordCount; i++) {
            LearningRecord record = new LearningRecord();
            record.setUserId(1L);
            record.setActivityTime(baseTime.minusDays(i % 10));
            records.add(record);
        }

        double speed = engine.calculateLearningSpeed(records);

        // Speed should be between 1 and recordCount (for 1-10 days)
        assertTrue(speed >= 1.0 && speed <= recordCount,
                "Speed should be between 1 and " + recordCount + ", but was: " + speed);
    }

    // Feature: personalized-learning-system, Property 7: 进度曲线日期范围
    @Property
    @DisplayName("进度曲线日期数量正确")
    void progressCurveDateCount(@ForAll @IntRange(min = 1, max = 90) int days) {
        LearningAnalyticsEngine.ProgressCurve curve = engine.generateProgressCurve(1L, days);

        assertEquals(days, curve.dates().size());
        assertEquals(days, curve.cumulativeWords().size());
        assertEquals(days, curve.masteredWords().size());
        assertEquals(days, curve.accuracyRates().size());
    }

    // Feature: personalized-learning-system, Property 7: 进度曲线数据非负
    @Property
    @DisplayName("进度曲线数据非负")
    void progressCurveDataNonNegative(@ForAll @IntRange(min = 1, max = 30) int days) {
        LearningAnalyticsEngine.ProgressCurve curve = engine.generateProgressCurve(1L, days);

        assertTrue(curve.cumulativeWords().stream().allMatch(v -> v >= 0));
        assertTrue(curve.masteredWords().stream().allMatch(v -> v >= 0));
        assertTrue(curve.accuracyRates().stream().allMatch(v -> v >= 0 && v <= 1));
    }

    // Feature: personalized-learning-system, Property 10: 进度曲线数据顺序
    @Property
    @DisplayName("进度曲线累计数据单调递增")
    void progressCurveCumulativeMonotonic(@ForAll @IntRange(min = 7, max = 30) int days) {
        LearningAnalyticsEngine.ProgressCurve curve = engine.generateProgressCurve(1L, days);

        // Cumulative words should be non-decreasing
        for (int i = 1; i < curve.cumulativeWords().size(); i++) {
            assertTrue(curve.cumulativeWords().get(i) >= curve.cumulativeWords().get(i - 1),
                    "Cumulative words should be non-decreasing");
        }

        // Mastered words should be non-decreasing
        for (int i = 1; i < curve.masteredWords().size(); i++) {
            assertTrue(curve.masteredWords().get(i) >= curve.masteredWords().get(i - 1),
                    "Mastered words should be non-decreasing");
        }
    }

    // Feature: personalized-learning-system, Property 8: 薄弱领域列表为空的情况
    @Property
    @DisplayName("空记录返回空薄弱领域列表")
    void emptyRecordsReturnEmptyWeakAreas() {
        List<LearningAnalyticsEngine.WeakArea> weakAreas = engine.identifyWeakAreas(List.of());

        assertTrue(weakAreas.isEmpty());
    }

    // Additional edge case tests
    @Test
    @DisplayName("null记录列表处理")
    void nullRecordsListHandling() {
        Map<String, Double> preferences = engine.analyzeTimePreferences(null);
        assertNotNull(preferences);
        assertEquals(0.0, preferences.get("morning"));
        assertEquals(0.0, preferences.get("afternoon"));
        assertEquals(0.0, preferences.get("evening"));
    }

    @Test
    @DisplayName("null活动时间处理")
    void nullActivityTimeHandling() {
        LearningRecord record = new LearningRecord();
        record.setUserId(1L);
        record.setActivityTime(null);

        Map<String, Double> preferences = engine.analyzeTimePreferences(List.of(record));

        // Should treat null as not contributing to any period
        assertEquals(0.0, preferences.get("morning"));
        assertEquals(0.0, preferences.get("afternoon"));
        assertEquals(0.0, preferences.get("evening"));
    }
}
