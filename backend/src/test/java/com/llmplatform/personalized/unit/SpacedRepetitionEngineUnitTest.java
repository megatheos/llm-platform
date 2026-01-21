package com.llmplatform.personalized.unit;

import com.llmplatform.personalized.engine.SpacedRepetitionEngine;
import com.llmplatform.personalized.engine.impl.SpacedRepetitionEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SpacedRepetitionEngine
 *
 * Tests specific edge cases and boundary conditions that are difficult
 * to verify with property-based testing alone.
 *
 * Requirement: 1.5 - 连续3次答错重置间隔
 */
@DisplayName("SpacedRepetitionEngine Unit Tests")
class SpacedRepetitionEngineUnitTest {

    private SpacedRepetitionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SpacedRepetitionEngineImpl();
    }

    @Nested
    @DisplayName("Consecutive Wrong Answer Tests")
    class ConsecutiveWrongTests {

        @Test
        @DisplayName("连续3次答错应重置复习间隔为1小时")
        void consecutiveThreeWrongShouldResetIntervalToOneHour() {
            // Given: 任意掌握度和复习次数下
            int masteryLevel = 50;
            int reviewCount = 5;
            int consecutiveWrongCount = 3;

            // When: 连续错误次数达到3次
            int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

            // Then: 复习间隔应重置为1小时
            assertThat(interval)
                    .as("连续3次答错应重置为1小时")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("连续4次答错也应重置复习间隔为1小时")
        void consecutiveFourWrongShouldResetIntervalToOneHour() {
            // Given
            int masteryLevel = 80;
            int reviewCount = 10;
            int consecutiveWrongCount = 4;

            // When
            int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

            // Then
            assertThat(interval)
                    .as("连续4次答错应重置为1小时")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("连续2次答错不应重置间隔")
        void consecutiveTwoWrongShouldNotResetInterval() {
            // Given: 连续2次错误，不应触发重置
            int masteryLevel = 50;
            int reviewCount = 5;
            int consecutiveWrongCount = 2;

            // When
            int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

            // Then: 应该按正常算法计算间隔，而不是1
            assertThat(interval)
                    .as("连续2次答错不应重置间隔")
                    .isGreaterThan(1);
        }

        @Test
        @DisplayName("连续0次答错应按正常算法计算间隔")
        void zeroConsecutiveWrongShouldUseNormalAlgorithm() {
            // Given
            int masteryLevel = 50;
            int reviewCount = 5;
            int consecutiveWrongCount = 0;

            // When
            int interval = engine.calculateReviewInterval(masteryLevel, reviewCount, consecutiveWrongCount);

            // Then: 应该按正常算法计算
            assertThat(interval)
                    .as("无连续错误应按正常算法计算")
                    .isGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("Mastery Level Boundary Tests")
    class MasteryLevelBoundaryTests {

        @Test
        @DisplayName("初始掌握度0答错应保持0")
        void initialLevelZeroWrongShouldStayZero() {
            int newLevel = engine.updateMasteryLevel(0, false);
            assertThat(newLevel).isEqualTo(0);
        }

        @Test
        @DisplayName("初始掌握度0答对应变为10")
        void initialLevelZeroCorrectShouldBecomeTen() {
            int newLevel = engine.updateMasteryLevel(0, true);
            assertThat(newLevel).isEqualTo(10);
        }

        @Test
        @DisplayName("掌握度95答错应变为80")
        void level95WrongShouldBecome80() {
            int newLevel = engine.updateMasteryLevel(95, false);
            assertThat(newLevel).isEqualTo(80);
        }

        @Test
        @DisplayName("掌握度100答错应变为85")
        void level100WrongShouldBecome85() {
            int newLevel = engine.updateMasteryLevel(100, false);
            assertThat(newLevel).isEqualTo(85);
        }

        @Test
        @DisplayName("掌握度100答对应保持100")
        void level100CorrectShouldStay100() {
            int newLevel = engine.updateMasteryLevel(100, true);
            assertThat(newLevel).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Status Determination Tests")
    class StatusDeterminationTests {

        @Test
        @DisplayName("掌握度79应返回LEARNING状态")
        void level79ShouldBeLearning() {
            assertThat(engine.determineStatus(79)).isEqualTo("LEARNING");
        }

        @Test
        @DisplayName("掌握度80应返回MASTERED状态")
        void level80ShouldBeMastered() {
            assertThat(engine.determineStatus(80)).isEqualTo("MASTERED");
        }

        @Test
        @DisplayName("掌握度100应返回MASTERED状态")
        void level100ShouldBeMastered() {
            assertThat(engine.determineStatus(100)).isEqualTo("MASTERED");
        }

        @Test
        @DisplayName("掌握度1应返回LEARNING状态")
        void level1ShouldBeLearning() {
            assertThat(engine.determineStatus(1)).isEqualTo("LEARNING");
        }

        @Test
        @DisplayName("掌握度0应返回FORGOTTEN状态")
        void level0ShouldBeForgotten() {
            assertThat(engine.determineStatus(0)).isEqualTo("FORGOTTEN");
        }

        @Test
        @DisplayName("isMastered边界测试：79不应已掌握")
        void level79ShouldNotBeMastered() {
            assertThat(engine.isMastered(79)).isFalse();
        }

        @Test
        @DisplayName("isMastered边界测试：80应已掌握")
        void isMasteredAtLevel80ShouldReturnTrue() {
            assertThat(engine.isMastered(80)).isTrue();
        }
    }

    @Nested
    @DisplayName("Review Interval Calculation Tests")
    class ReviewIntervalTests {

        @Test
        @DisplayName("首次复习间隔应为1小时")
        void firstReviewIntervalShouldBeOneHour() {
            int interval = engine.calculateReviewInterval(50, 0, 0);
            assertThat(interval).isEqualTo(1);
        }

        @Test
        @DisplayName("相同掌握度下，复习次数增加间隔应增加")
        void intervalShouldIncreaseWithReviewCount() {
            int interval1 = engine.calculateReviewInterval(50, 0, 0);
            int interval2 = engine.calculateReviewInterval(50, 1, 0);
            int interval3 = engine.calculateReviewInterval(50, 2, 0);

            assertThat(interval2).isGreaterThan(interval1);
            assertThat(interval3).isGreaterThan(interval2);
        }

        @Test
        @DisplayName("低掌握度区间(0-20)的增长因子应为2")
        void lowMasteryLevelShouldUseGrowthFactor2() {
            // 0-20 掌握度区间，增长因子为2
            int interval0 = engine.calculateReviewInterval(10, 0, 0);
            int interval1 = engine.calculateReviewInterval(10, 1, 0);
            int interval2 = engine.calculateReviewInterval(10, 2, 0);

            assertThat(interval1).isEqualTo(2);  // 1 * 2^1 = 2
            assertThat(interval2).isEqualTo(4);  // 1 * 2^2 = 4
        }

        @Test
        @DisplayName("高掌握度区间(81-100)的增长因子应为8")
        void highMasteryLevelShouldUseGrowthFactor8() {
            // 81-100 掌握度区间，增长因子为8
            int interval0 = engine.calculateReviewInterval(90, 0, 0);
            int interval1 = engine.calculateReviewInterval(90, 1, 0);
            int interval2 = engine.calculateReviewInterval(90, 2, 0);

            assertThat(interval0).isEqualTo(1);   // 1 * 8^0 = 1
            assertThat(interval1).isEqualTo(8);   // 1 * 8^1 = 8
            assertThat(interval2).isEqualTo(64);  // 1 * 8^2 = 64
        }

        @Test
        @DisplayName("间隔不应超过720小时（约30天）")
        void intervalShouldNotExceedMaxLimit() {
            // 多次复习后，间隔应该被限制
            int interval = engine.calculateReviewInterval(100, 20, 0);
            assertThat(interval).isLessThanOrEqualTo(720);
        }
    }
}
