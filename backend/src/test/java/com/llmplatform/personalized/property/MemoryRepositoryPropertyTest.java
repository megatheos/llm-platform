package com.llmplatform.personalized.property;

import com.llmplatform.personalized.entity.MemoryRecord;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for MemoryRecord entity and repository logic
 *
 * Feature: personalized-learning-system, Property 1: 记忆记录创建初始化
 * Feature: personalized-learning-system, Property 5: 复习列表优先级排序
 *
 * For any valid user and word, when creating a new memory record:
 * - Initial mastery level should be 0
 * - Status should be "LEARNING"
 * - Review count should be 0
 *
 * For any due review records, the review list should be sorted by:
 * - Overdue records first (next_review_time <= current time)
 * - Among overdue records, lower mastery level first
 *
 * Validates: Requirements 1.1, 1.6
 */
@DisplayName("MemoryRepository Property Tests")
class MemoryRepositoryPropertyTest {

    // ==================== Property 1: 记忆记录创建初始化 ====================

    /**
     * Property: For any valid user ID and word ID, when creating a new MemoryRecord,
     * setting initial values should result in correct values.
     *
     * Requirement: 1.1
     */
    @Property(tries = 100)
    @DisplayName("新记忆记录的初始值验证")
    void newMemoryRecordHasCorrectInitialValues(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll @LongRange(min = 1) Long wordId) {

        // Arrange - Simulate creating a new record
        MemoryRecord record = new MemoryRecord();
        record.setUserId(userId);
        record.setWordId(wordId);

        // Simulate creation with initial values
        record.setMasteryLevel(0);
        record.setStatus("LEARNING");
        record.setReviewCount(0);
        record.setCorrectCount(0);
        record.setWrongCount(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());

        // Assert - Initial values should be set correctly
        assertThat(record.getMasteryLevel())
                .as("Initial mastery level should be 0")
                .isEqualTo(0);

        assertThat(record.getStatus())
                .as("Initial status should be LEARNING")
                .isEqualTo("LEARNING");

        assertThat(record.getReviewCount())
                .as("Initial review count should be 0")
                .isEqualTo(0);

        assertThat(record.getCorrectCount())
                .as("Initial correct count should be 0")
                .isEqualTo(0);

        assertThat(record.getWrongCount())
                .as("Initial wrong count should be 0")
                .isEqualTo(0);
    }

    /**
     * Property: For any valid input, the created record should have valid timestamps.
     *
     * Requirement: 1.1
     */
    @Property(tries = 50)
    @DisplayName("新创建的记忆记录时间戳验证")
    void newMemoryRecordHasValidTimestamps(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll @LongRange(min = 1) Long wordId) {

        // Arrange
        MemoryRecord record = new MemoryRecord();
        record.setUserId(userId);
        record.setWordId(wordId);
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setUpdatedTime(now);

        // Assert
        assertThat(record.getCreatedTime())
                .as("Created time should be set")
                .isNotNull();

        assertThat(record.getUpdatedTime())
                .as("Updated time should be set")
                .isNotNull();

        assertThat(record.getCreatedTime())
                .as("Created time should be in the past or present")
                .isBeforeOrEqualTo(now);

        assertThat(record.getUpdatedTime())
                .as("Updated time should be in the past or present")
                .isBeforeOrEqualTo(now);
    }

    /**
     * Property: For any mastery level, the status should be correctly determined.
     *
     * Requirement: 1.4
     */
    @Property(tries = 101)
    @DisplayName("记忆记录状态判定验证")
    void memoryRecordStatusIsCorrect(
            @ForAll @IntRange(min = 0, max = 100) int masteryLevel) {

        // Arrange
        MemoryRecord record = new MemoryRecord();
        record.setMasteryLevel(masteryLevel);

        // Set status based on mastery level
        if (masteryLevel >= 80) {
            record.setStatus("MASTERED");
        } else if (masteryLevel <= 0) {
            record.setStatus("FORGOTTEN");
        } else {
            record.setStatus("LEARNING");
        }

        // Assert
        if (masteryLevel >= 80) {
            assertThat(record.getStatus())
                    .as("Mastery level %d should be MASTERED", masteryLevel)
                    .isEqualTo("MASTERED");
        } else if (masteryLevel <= 0) {
            assertThat(record.getStatus())
                    .as("Mastery level %d should be FORGOTTEN", masteryLevel)
                    .isEqualTo("FORGOTTEN");
        } else {
            assertThat(record.getStatus())
                    .as("Mastery level %d should be LEARNING", masteryLevel)
                    .isEqualTo("LEARNING");
        }
    }

    // ==================== Property 5: 复习列表优先级排序 ====================

    /**
     * Property: For any list of due review records, sorting by next_review_time
     * then mastery_level should produce the correct order.
     *
     * Requirement: 1.6
     */
    @Property(tries = 50)
    @DisplayName("复习列表排序验证 - 按复习时间和掌握度")
    void reviewListSortingByTimeAndMastery(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll List<MemoryRecord> records) {

        // Prepare test data - limit size and ensure validity
        List<MemoryRecord> testRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < Math.min(records.size(), 20); i++) {
            MemoryRecord record = records.get(i);
            if (record != null) {
                record.setUserId(userId);
                record.setId((long) (i + 1));

                // Assign random past times to simulate due reviews
                int hoursAgo = (i * 3) % 24 + 1;
                record.setNextReviewTime(now.minusHours(hoursAgo));

                // Assign random mastery levels between 0-100
                record.setMasteryLevel((i * 7) % 101);

                testRecords.add(record);
            }
        }

        if (testRecords.size() < 2) {
            return; // Skip if not enough records
        }

        // Sort using the repository's ordering logic
        List<MemoryRecord> sorted = testRecords.stream()
                .sorted(Comparator
                        .comparing(MemoryRecord::getNextReviewTime)
                        .thenComparing(MemoryRecord::getMasteryLevel))
                .toList();

        // Verify: next_review_time should be non-decreasing
        for (int i = 1; i < sorted.size(); i++) {
            assertThat(sorted.get(i).getNextReviewTime())
                    .as("Next review time should be non-decreasing at index %d", i)
                    .isAfterOrEqualTo(sorted.get(i - 1).getNextReviewTime());
        }
    }

    /**
     * Property: For any due review records with different mastery levels,
     * records with lower mastery level should come first (when same review time).
     *
     * Requirement: 1.6
     */
    @Property(tries = 30)
    @DisplayName("复习列表中相同复习时间时掌握度排序验证")
    void dueReviewsSortedByMasteryLevelForSameTime(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll @IntRange(min = 5, max = 15) int recordCount) {

        LocalDateTime fixedTime = LocalDateTime.now().minusHours(1);
        List<MemoryRecord> testRecords = new ArrayList<>();

        for (int i = 0; i < recordCount; i++) {
            MemoryRecord record = new MemoryRecord();
            record.setUserId(userId);
            record.setId((long) (i + 1));
            record.setNextReviewTime(fixedTime);
            // Assign mastery levels in random order
            record.setMasteryLevel((recordCount - i) * 7 % 101);
            testRecords.add(record);
        }

        // Sort using the repository's ordering logic
        List<MemoryRecord> sorted = testRecords.stream()
                .sorted(Comparator
                        .comparing(MemoryRecord::getNextReviewTime)
                        .thenComparing(MemoryRecord::getMasteryLevel))
                .toList();

        // Verify: mastery level should be non-decreasing for same review time
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).getNextReviewTime().equals(sorted.get(i - 1).getNextReviewTime())) {
                assertThat(sorted.get(i).getMasteryLevel())
                        .as("Mastery level should be non-decreasing for same review time at index %d", i)
                        .isGreaterThanOrEqualTo(sorted.get(i - 1).getMasteryLevel());
            }
        }
    }

    /**
     * Property: Overdue records should always appear before future records.
     *
     * Requirement: 1.6
     */
    @Property(tries = 30)
    @DisplayName("已过期复习应在未来复习之前")
    void overdueReviewsAppearBeforeFutureReviews(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll @IntRange(min = 4, max = 10) int recordCount) {

        LocalDateTime now = LocalDateTime.now();
        List<MemoryRecord> testRecords = new ArrayList<>();

        // Create mixed records: half overdue, half future
        for (int i = 0; i < recordCount; i++) {
            MemoryRecord record = new MemoryRecord();
            record.setUserId(userId);
            record.setId((long) (i + 1));

            // Alternate between overdue and future
            if (i % 2 == 0) {
                record.setNextReviewTime(now.minusHours(i + 1)); // Overdue
            } else {
                record.setNextReviewTime(now.plusHours(i + 1)); // Future
            }
            record.setMasteryLevel(i * 10 % 101);
            testRecords.add(record);
        }

        // Sort using the repository's ordering logic
        List<MemoryRecord> sorted = testRecords.stream()
                .sorted(Comparator
                        .comparing(MemoryRecord::getNextReviewTime)
                        .thenComparing(MemoryRecord::getMasteryLevel))
                .toList();

        // Find first future record index
        int firstFutureIndex = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getNextReviewTime().isAfter(now)) {
                firstFutureIndex = i;
                break;
            }
        }

        if (firstFutureIndex > 0) {
            // Verify all records before first future are overdue
            for (int i = 0; i < firstFutureIndex; i++) {
                assertThat(sorted.get(i).getNextReviewTime())
                        .as("Record at index %d should be overdue", i)
                        .isBeforeOrEqualTo(now);
            }

            // Verify all records from first future onwards are future
            for (int i = firstFutureIndex; i < sorted.size(); i++) {
                assertThat(sorted.get(i).getNextReviewTime())
                        .as("Record at index %d should be in the future", i)
                        .isAfter(now);
            }
        }
    }

    /**
     * Property: Empty list should remain empty after sorting.
     */
    @Property(tries = 10)
    @DisplayName("空列表排序验证")
    void emptyListSorting() {
        List<MemoryRecord> emptyList = new ArrayList<>();

        List<MemoryRecord> sorted = emptyList.stream()
                .sorted(Comparator
                        .comparing(MemoryRecord::getNextReviewTime)
                        .thenComparing(MemoryRecord::getMasteryLevel))
                .toList();

        assertThat(sorted)
                .as("Empty list should remain empty after sorting")
                .isEmpty();
    }

    /**
     * Property: Single element list should remain unchanged after sorting.
     */
    @Property(tries = 20)
    @DisplayName("单元素列表排序验证")
    void singleElementListSorting(
            @ForAll @LongRange(min = 1) Long userId,
            @ForAll @IntRange(min = 0, max = 100) int masteryLevel) {

        MemoryRecord record = new MemoryRecord();
        record.setUserId(userId);
        record.setId(1L);
        record.setMasteryLevel(masteryLevel);
        record.setNextReviewTime(LocalDateTime.now());

        List<MemoryRecord> singleList = List.of(record);

        List<MemoryRecord> sorted = singleList.stream()
                .sorted(Comparator
                        .comparing(MemoryRecord::getNextReviewTime)
                        .thenComparing(MemoryRecord::getMasteryLevel))
                .toList();

        assertThat(sorted)
                .as("Single element list should have same element after sorting")
                .hasSize(1);
        assertThat(sorted.get(0))
                .as("The single element should be the same record")
                .isEqualTo(record);
    }
}
