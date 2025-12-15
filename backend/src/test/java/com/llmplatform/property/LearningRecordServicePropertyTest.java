package com.llmplatform.property;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.common.ActivityType;
import com.llmplatform.dto.RecordQueryDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.DialogueSession;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Quiz;
import com.llmplatform.entity.Word;
import com.llmplatform.mapper.DialogueSessionMapper;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.QuizMapper;
import com.llmplatform.mapper.WordMapper;
import com.llmplatform.service.LearningRecordService;
import com.llmplatform.service.UserService;
import com.llmplatform.vo.LearningRecordVO;
import com.llmplatform.vo.LearningRecordsVO;
import com.llmplatform.vo.UserVO;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for LearningRecordService
 */
@SpringBootTest
@ActiveProfiles("test")
class LearningRecordServicePropertyTest {

    @Autowired
    private LearningRecordService learningRecordService;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private DialogueSessionMapper dialogueSessionMapper;

    @Autowired
    private QuizMapper quizMapper;

    @Autowired
    private UserService userService;

    /**
     * Feature: llm-language-learning-platform, Property 11: Learning records completeness
     * 
     * For any user with learning activities, requesting learning records should return
     * all word queries, dialogue sessions, and quiz results associated with that user.
     * 
     * Validates: Requirements 5.1
     */
    @Test
    @Transactional
    void learningRecordsCompleteness_property() {
        Arbitrary<Integer> wordQueryCounts = Arbitraries.integers().between(0, 5);
        Arbitrary<Integer> dialogueCounts = Arbitraries.integers().between(0, 5);
        Arbitrary<Integer> quizCounts = Arbitraries.integers().between(0, 5);

        Arbitrary<ActivityCounts> activityCountsArbitrary = Combinators.combine(
                wordQueryCounts, dialogueCounts, quizCounts)
                .as(ActivityCounts::new);

        for (int i = 0; i < 100; i++) {
            ActivityCounts counts = activityCountsArbitrary.sample();
            
            // Skip if all counts are zero
            if (counts.wordQueries == 0 && counts.dialogues == 0 && counts.quizzes == 0) {
                continue;
            }

            // Create test user
            UserVO testUser = createTestUser();
            Long userId = testUser.getId();

            // Track created activity IDs
            Set<Long> expectedWordIds = new HashSet<>();
            Set<Long> expectedDialogueIds = new HashSet<>();
            Set<Long> expectedQuizIds = new HashSet<>();

            // Create word query records
            for (int j = 0; j < counts.wordQueries; j++) {
                Word word = createTestWord("word" + j + "_" + UUID.randomUUID().toString().substring(0, 4));
                wordMapper.insert(word);
                learningRecordService.recordWordQuery(userId, word.getId());
                expectedWordIds.add(word.getId());
            }

            // Create dialogue session records
            for (int j = 0; j < counts.dialogues; j++) {
                DialogueSession session = createTestDialogueSession(userId);
                dialogueSessionMapper.insert(session);
                learningRecordService.recordDialogueSession(userId, session);
                expectedDialogueIds.add(session.getId());
            }

            // Create quiz records
            for (int j = 0; j < counts.quizzes; j++) {
                Quiz quiz = createTestQuiz(userId);
                quizMapper.insert(quiz);
                learningRecordService.recordQuizResult(userId, quiz);
                expectedQuizIds.add(quiz.getId());
            }

            // Retrieve all records with large page size to get all
            RecordQueryDTO params = new RecordQueryDTO();
            params.setPage(1);
            params.setPageSize(100);
            LearningRecordsVO result = learningRecordService.getRecords(userId, params);

            // Assert - Total count should match
            int expectedTotal = counts.wordQueries + counts.dialogues + counts.quizzes;
            assertThat(result.getTotal()).isEqualTo(expectedTotal);
            assertThat(result.getRecords()).hasSize(expectedTotal);

            // Verify all word query records are present
            Set<Long> actualWordIds = result.getRecords().stream()
                    .filter(r -> ActivityType.WORD_QUERY.name().equals(r.getActivityType()))
                    .map(LearningRecordVO::getActivityId)
                    .collect(Collectors.toSet());
            assertThat(actualWordIds).containsExactlyInAnyOrderElementsOf(expectedWordIds);

            // Verify all dialogue records are present
            Set<Long> actualDialogueIds = result.getRecords().stream()
                    .filter(r -> ActivityType.DIALOGUE.name().equals(r.getActivityType()))
                    .map(LearningRecordVO::getActivityId)
                    .collect(Collectors.toSet());
            assertThat(actualDialogueIds).containsExactlyInAnyOrderElementsOf(expectedDialogueIds);

            // Verify all quiz records are present
            Set<Long> actualQuizIds = result.getRecords().stream()
                    .filter(r -> ActivityType.QUIZ.name().equals(r.getActivityType()))
                    .map(LearningRecordVO::getActivityId)
                    .collect(Collectors.toSet());
            assertThat(actualQuizIds).containsExactlyInAnyOrderElementsOf(expectedQuizIds);
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 12: Learning records ordering
     * 
     * For any set of learning records, they should be sorted by date in descending order
     * (most recent first).
     * 
     * Validates: Requirements 5.2
     */
    @Test
    @Transactional
    void learningRecordsOrdering_property() {
        Arbitrary<Integer> recordCounts = Arbitraries.integers().between(2, 10);

        for (int i = 0; i < 100; i++) {
            int recordCount = recordCounts.sample();

            // Create test user
            UserVO testUser = createTestUser();
            Long userId = testUser.getId();

            // Create records with varying timestamps
            List<LocalDateTime> expectedTimestamps = new ArrayList<>();
            for (int j = 0; j < recordCount; j++) {
                Word word = createTestWord("word" + j + "_" + UUID.randomUUID().toString().substring(0, 4));
                wordMapper.insert(word);
                
                // Create record with specific timestamp
                LearningRecord record = new LearningRecord();
                record.setUserId(userId);
                record.setActivityType(ActivityType.WORD_QUERY.name());
                record.setActivityId(word.getId());
                // Add varying time offsets to ensure different timestamps
                LocalDateTime timestamp = LocalDateTime.now().minusMinutes(recordCount - j);
                record.setActivityTime(timestamp);
                learningRecordMapper.insert(record);
                expectedTimestamps.add(timestamp);
            }

            // Sort expected timestamps in descending order
            expectedTimestamps.sort(Comparator.reverseOrder());

            // Retrieve records
            RecordQueryDTO params = new RecordQueryDTO();
            params.setPage(1);
            params.setPageSize(100);
            LearningRecordsVO result = learningRecordService.getRecords(userId, params);

            // Assert - Records should be in descending order by activity time
            assertThat(result.getRecords()).hasSize(recordCount);

            List<LocalDateTime> actualTimestamps = result.getRecords().stream()
                    .map(LearningRecordVO::getActivityTime)
                    .collect(Collectors.toList());

            // Verify ordering - each timestamp should be >= the next one
            for (int j = 0; j < actualTimestamps.size() - 1; j++) {
                assertThat(actualTimestamps.get(j))
                        .isAfterOrEqualTo(actualTimestamps.get(j + 1));
            }
        }
    }

    /**
     * Helper method to create a test user
     */
    private UserVO createTestUser() {
        RegisterDTO dto = new RegisterDTO();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        dto.setUsername("testuser" + uniqueSuffix);
        dto.setPassword("password123");
        dto.setEmail("test" + uniqueSuffix + "@test.com");
        return userService.register(dto);
    }

    /**
     * Helper method to create a test word
     */
    private Word createTestWord(String wordText) {
        Word word = new Word();
        word.setWord(wordText.toLowerCase());
        word.setSourceLang("en");
        word.setTargetLang("zh");
        word.setDefinition("Test definition");
        word.setTranslation("测试翻译");
        word.setExamples("[]");
        word.setPronunciation("/test/");
        word.setCreatedAt(LocalDateTime.now());
        return word;
    }

    /**
     * Helper method to create a test dialogue session
     */
    private DialogueSession createTestDialogueSession(Long userId) {
        DialogueSession session = new DialogueSession();
        session.setUserId(userId);
        session.setScenarioId(1L); // Use preset scenario
        session.setMessages("[]");
        session.setStartedAt(LocalDateTime.now().minusMinutes(10));
        session.setEndedAt(LocalDateTime.now());
        return session;
    }

    /**
     * Helper method to create a test quiz
     */
    private Quiz createTestQuiz(Long userId) {
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setDifficulty("easy");
        quiz.setQuestions("[]");
        quiz.setTotalScore(10);
        quiz.setUserScore(8);
        quiz.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        quiz.setCompletedAt(LocalDateTime.now());
        return quiz;
    }

    /**
     * Helper class for activity counts
     */
    private static class ActivityCounts {
        final int wordQueries;
        final int dialogues;
        final int quizzes;

        ActivityCounts(int wordQueries, int dialogues, int quizzes) {
            this.wordQueries = wordQueries;
            this.dialogues = dialogues;
            this.quizzes = quizzes;
        }
    }
}
