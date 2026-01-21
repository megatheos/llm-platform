package com.llmplatform.personalized.property;

import com.llmplatform.personalized.engine.SpacedRepetitionEngine;
import com.llmplatform.personalized.engine.impl.SpacedRepetitionEngineImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for SpacedRepetitionEngine
 *
 * Feature: personalized-learning-system, Property 2: 掌握度更新边界保持
 * Feature: personalized-learning-system, Property 3: 复习间隔单调性
 * Feature: personalized-learning-system, Property 4: 掌握状态判定
 *
 * For any valid input, the engine should:
 * - Keep mastery level within bounds [0, 100]
 * - Update mastery in correct direction (increase for correct, decrease for wrong)
 * - Calculate review intervals that increase with mastery level
 * - Determine status correctly based on mastery threshold (>=80 is MASTERED)
 *
 * Validates: Requirements 1.2, 1.3, 1.4, 1.7
 */
class SpacedRepetitionEnginePropertyTest {

    private final SpacedRepetitionEngine engine = new SpacedRepetitionEngineImpl();

    // ==================== Property 2: 掌握度更新边界保持 ====================

    /**
     * Property: For any current mastery level (0-100) and answer result (correct/wrong),
     * the updated mastery level should always stay within [0, 100] bounds.
     *
     * Requirement: 1.2, 1.7
     */
    @Property(tries = 100)
    void masteryLevelUpdateStaysWithinBounds(
            @ForAll @IntRange(min = 0, max = 100) int currentLevel,
            @ForAll boolean isCorrect) {

        // Act
        int newLevel = engine.updateMasteryLevel(currentLevel, isCorrect);

        // Assert - Boundary check
        assertThat(newLevel)
                .as("Mastery level should be >= 0")
                .isGreaterThanOrEqualTo(0);
        assertThat(newLevel)
                .as("Mastery level should be <= 100")
                .isLessThanOrEqualTo(100);
    }

    /**
     * Property: For any current mastery level, correct answer should increase or maintain level,
     * wrong answer should decrease or maintain level.
     *
     * Requirement: 1.2
     */
    @Property(tries = 100)
    void masteryLevelUpdateDirection(
            @ForAll @IntRange(min = 0, max = 100) int currentLevel,
            @ForAll boolean isCorrect) {

        // Act
        int newLevel = engine.updateMasteryLevel(currentLevel, isCorrect);

        // Assert - Direction check
        if (isCorrect) {
            assertThat(newLevel)
                    .as("Correct answer should not decrease mastery level")
                    .isGreaterThanOrEqualTo(currentLevel);
        } else {
            assertThat(newLevel)
                    .as("Wrong answer should not increase mastery level")
                    .isLessThanOrEqualTo(currentLevel);
        }
    }

    /**
     * Property: Correct answer at max level (100) should stay at 100.
     *
     * Requirement: 1.7
     */
    @Property(tries = 1)
    void correctAnswerAtMaxLevelStaysAtMax() {

        // Act
        int newLevel = engine.updateMasteryLevel(100, true);

        // Assert
        assertThat(newLevel)
                .as("Mastery level at 100 with correct answer should stay at 100")
                .isEqualTo(100);
    }

    /**
     * Property: Wrong answer at min level (0) should stay at 0.
     *
     * Requirement: 1.7
     */
    @Property(tries = 1)
    void wrongAnswerAtMinLevelStaysAtMin() {

        // Act
        int newLevel = engine.updateMasteryLevel(0, false);

        // Assert
        assertThat(newLevel)
                .as("Mastery level at 0 with wrong answer should stay at 0")
                .isEqualTo(0);
    }

    // ==================== Property 3: 复习间隔单调性 ====================

    /**
     * Property: For any two mastery levels where level1 < level2,
     * the review interval for level2 should be >= level1 at the same review count.
     *
     * Requirement: 1.3
     */
    @Property(tries = 100)
    void reviewIntervalIncreasesWithMasteryLevel(
            @ForAll @IntRange(min = 0, max = 99) int level1,
            @ForAll @IntRange(min = 0, max = 10) int reviewCount,
            @ForAll @IntRange(min = 0, max = 2) int consecutiveWrongCount) {

        // Arrange
        int level2 = level1 + 1;

        // Act
        int interval1 = engine.calculateReviewInterval(level1, reviewCount, consecutiveWrongCount);
        int interval2 = engine.calculateReviewInterval(level2, reviewCount, consecutiveWrongCount);

        // Assert - Higher mastery should have >= interval
        assertThat(interval2)
                .as("Higher mastery level should have >= review interval")
                .isGreaterThanOrEqualTo(interval1);
    }

    /**
     * Property: For any mastery level, higher review count should produce >= interval.
     *
     * Requirement: 1.3
     */
    @Property(tries = 100)
    void reviewIntervalIncreasesWithReviewCount(
            @ForAll @IntRange(min = 0, max = 100) int masteryLevel,
            @ForAll @IntRange(min = 0, max = 9) int reviewCount1,
            @ForAll @IntRange(min = 0, max = 2) int consecutiveWrongCount) {

        // Arrange
        int reviewCount2 = reviewCount1 + 1;

        // Act
        int interval1 = engine.calculateReviewInterval(masteryLevel, reviewCount1, consecutiveWrongCount);
        int interval2 = engine.calculateReviewInterval(masteryLevel, reviewCount2, consecutiveWrongCount);

        // Assert - Higher review count should have >= interval
        assertThat(interval2)
                .as("Higher review count should have >= review interval")
                .isGreaterThanOrEqualTo(interval1);
    }

    /**
     * Property: Consecutive wrong count >= 3 should reset interval to 1 hour.
     *
     * Requirement: 1.5
     */
    @Property(tries = 100)
    void consecutiveThreeWrongResetsInterval(
            @ForAll @IntRange(min = 0, max = 100) int masteryLevel,
            @ForAll @IntRange(min = 3, max = 10) int consecutiveWrongCount,
            @ForAll @IntRange(min = 0, max = 20) int reviewCount) {

        // Act
        int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

        // Assert - Should reset to minimum interval
        assertThat(interval)
                .as("Consecutive wrong count >= 3 should reset interval to 1")
                .isEqualTo(1);
    }

    /**
     * Property: Review interval should always be positive.
     *
     * Requirement: 1.3
     */
    @Property(tries = 100)
    void reviewIntervalAlwaysPositive(
            @ForAll @IntRange(min = 0, max = 100) int masteryLevel,
            @ForAll @IntRange(min = 0, max = 20) int reviewCount,
            @ForAll @IntRange(min = 0, max = 10) int consecutiveWrongCount) {

        // Act
        int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

        // Assert
        assertThat(interval)
                .as("Review interval should always be positive")
                .isGreaterThan(0);
    }

    // ==================== Property 4: 掌握状态判定 ====================

    /**
     * Property: For any mastery level >= 80, isMastered should return true.
     *
     * Requirement: 1.4
     */
    @Property(tries = 100)
    void masteryLevel80OrAboveIsMastered(
            @ForAll @IntRange(min = 80, max = 100) int masteryLevel) {

        // Act & Assert
        assertThat(engine.isMastered(masteryLevel))
                .as("Mastery level >= 80 should be considered mastered")
                .isTrue();
    }

    /**
     * Property: For any mastery level < 80, isMastered should return false.
     *
     * Requirement: 1.4
     */
    @Property(tries = 100)
    void masteryLevelBelow80IsNotMastered(
            @ForAll @IntRange(min = 0, max = 79) int masteryLevel) {

        // Act & Assert
        assertThat(engine.isMastered(masteryLevel))
                .as("Mastery level < 80 should not be considered mastered")
                .isFalse();
    }

    /**
     * Property: For any mastery level >= 80, determineStatus should return "MASTERED".
     *
     * Requirement: 1.4
     */
    @Property(tries = 21)
    void masteryLevel80OrAboveIsMasteredStatus(
            @ForAll @IntRange(min = 80, max = 100) int masteryLevel) {

        // Act
        String status = engine.determineStatus(masteryLevel);

        // Assert
        assertThat(status)
                .as("Mastery level >= 80 should have MASTERED status")
                .isEqualTo("MASTERED");
    }

    /**
     * Property: For mastery level = 0, determineStatus should return "FORGOTTEN".
     *
     * Requirement: 1.4
     */
    @Property(tries = 1)
    void masteryLevelZeroIsForgottenStatus() {

        // Act
        String status = engine.determineStatus(0);

        // Assert
        assertThat(status)
                .as("Mastery level 0 should have FORGOTTEN status")
                .isEqualTo("FORGOTTEN");
    }

    /**
     * Property: For mastery level between 1-79, determineStatus should return "LEARNING".
     *
     * Requirement: 1.4
     */
    @Property(tries = 79)
    void masteryLevelBetween1And79IsLearningStatus(
            @ForAll @IntRange(min = 1, max = 79) int masteryLevel) {

        // Act
        String status = engine.determineStatus(masteryLevel);

        // Assert
        assertThat(status)
                .as("Mastery level between 1-79 should have LEARNING status")
                .isEqualTo("LEARNING");
    }
}
