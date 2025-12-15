package com.llmplatform.property;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.common.ActivityType;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.dto.SubmitAnswersDTO;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Quiz;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.QuizMapper;
import com.llmplatform.service.QuizService;
import com.llmplatform.service.UserService;
import com.llmplatform.vo.QuizResultVO;
import com.llmplatform.vo.QuizVO;
import com.llmplatform.vo.UserVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for QuizService
 */
@SpringBootTest
@ActiveProfiles("test")
class QuizServicePropertyTest {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizMapper quizMapper;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * Feature: llm-language-learning-platform, Property 9: Quiz score calculation correctness
     * 
     * For any quiz with known correct answers, submitting answers should produce a score
     * equal to the count of matching correct answers.
     * 
     * Validates: Requirements 4.2
     */
    @Test
    @Transactional
    void quizScoreCalculationCorrectness_property() {
        // Test the pure calculateScore function with generated data
        Arbitrary<Integer> questionCounts = Arbitraries.integers().between(1, 10);
        Arbitrary<List<String>> answerOptions = Arbitraries.of("A", "B", "C", "D")
                .list().ofSize(4);

        for (int i = 0; i < 100; i++) {
            int numQuestions = questionCounts.sample();
            
            // Generate questions with known correct answers
            List<QuizVO.QuestionVO> questions = IntStream.rangeClosed(1, numQuestions)
                    .mapToObj(qId -> {
                        QuizVO.QuestionVO q = new QuizVO.QuestionVO();
                        q.setQuestionId(qId);
                        q.setQuestion("Question " + qId);
                        q.setOptions(Arrays.asList("A", "B", "C", "D"));
                        // Randomly assign correct answer
                        String correctAnswer = Arbitraries.of("A", "B", "C", "D").sample();
                        q.setCorrectAnswer(correctAnswer);
                        return q;
                    })
                    .collect(Collectors.toList());

            // Generate user answers - some correct, some incorrect
            List<SubmitAnswersDTO.AnswerDTO> userAnswers = questions.stream()
                    .map(q -> {
                        SubmitAnswersDTO.AnswerDTO answer = new SubmitAnswersDTO.AnswerDTO();
                        answer.setQuestionId(q.getQuestionId());
                        // Randomly decide if answer is correct or not
                        boolean makeCorrect = Arbitraries.of(true, false).sample();
                        if (makeCorrect) {
                            answer.setAnswer(q.getCorrectAnswer());
                        } else {
                            // Pick a wrong answer
                            String wrongAnswer = Arbitraries.of("A", "B", "C", "D")
                                    .filter(a -> !a.equals(q.getCorrectAnswer()))
                                    .sample();
                            answer.setAnswer(wrongAnswer);
                        }
                        return answer;
                    })
                    .collect(Collectors.toList());

            // Calculate expected score manually
            int expectedScore = 0;
            for (SubmitAnswersDTO.AnswerDTO answer : userAnswers) {
                QuizVO.QuestionVO question = questions.stream()
                        .filter(q -> q.getQuestionId().equals(answer.getQuestionId()))
                        .findFirst()
                        .orElse(null);
                if (question != null && 
                    question.getCorrectAnswer().equalsIgnoreCase(answer.getAnswer())) {
                    expectedScore++;
                }
            }

            // Calculate score using the service
            int actualScore = quizService.calculateScore(userAnswers, questions);

            // Assert - Score should equal count of matching answers
            assertThat(actualScore)
                    .as("Score for %d questions should match count of correct answers", numQuestions)
                    .isEqualTo(expectedScore);
        }
    }

    /**
     * Additional property: Score is bounded by total questions
     * For any quiz, the score should be between 0 and the total number of questions
     */
    @Test
    @Transactional
    void quizScoreBoundedByTotalQuestions_property() {
        for (int i = 0; i < 100; i++) {
            int numQuestions = Arbitraries.integers().between(1, 20).sample();
            
            // Generate questions
            List<QuizVO.QuestionVO> questions = IntStream.rangeClosed(1, numQuestions)
                    .mapToObj(qId -> {
                        QuizVO.QuestionVO q = new QuizVO.QuestionVO();
                        q.setQuestionId(qId);
                        q.setQuestion("Question " + qId);
                        q.setOptions(Arrays.asList("A", "B", "C", "D"));
                        q.setCorrectAnswer(Arbitraries.of("A", "B", "C", "D").sample());
                        return q;
                    })
                    .collect(Collectors.toList());

            // Generate random answers
            List<SubmitAnswersDTO.AnswerDTO> userAnswers = questions.stream()
                    .map(q -> {
                        SubmitAnswersDTO.AnswerDTO answer = new SubmitAnswersDTO.AnswerDTO();
                        answer.setQuestionId(q.getQuestionId());
                        answer.setAnswer(Arbitraries.of("A", "B", "C", "D").sample());
                        return answer;
                    })
                    .collect(Collectors.toList());

            int score = quizService.calculateScore(userAnswers, questions);

            // Assert - Score should be bounded
            assertThat(score)
                    .as("Score should be >= 0")
                    .isGreaterThanOrEqualTo(0);
            assertThat(score)
                    .as("Score should be <= total questions (%d)", numQuestions)
                    .isLessThanOrEqualTo(numQuestions);
        }
    }

    /**
     * Property: All correct answers yield maximum score
     */
    @Test
    @Transactional
    void allCorrectAnswersYieldMaxScore_property() {
        for (int i = 0; i < 100; i++) {
            int numQuestions = Arbitraries.integers().between(1, 15).sample();
            
            // Generate questions
            List<QuizVO.QuestionVO> questions = IntStream.rangeClosed(1, numQuestions)
                    .mapToObj(qId -> {
                        QuizVO.QuestionVO q = new QuizVO.QuestionVO();
                        q.setQuestionId(qId);
                        q.setQuestion("Question " + qId);
                        q.setOptions(Arrays.asList("A", "B", "C", "D"));
                        q.setCorrectAnswer(Arbitraries.of("A", "B", "C", "D").sample());
                        return q;
                    })
                    .collect(Collectors.toList());

            // Submit all correct answers
            List<SubmitAnswersDTO.AnswerDTO> userAnswers = questions.stream()
                    .map(q -> {
                        SubmitAnswersDTO.AnswerDTO answer = new SubmitAnswersDTO.AnswerDTO();
                        answer.setQuestionId(q.getQuestionId());
                        answer.setAnswer(q.getCorrectAnswer());
                        return answer;
                    })
                    .collect(Collectors.toList());

            int score = quizService.calculateScore(userAnswers, questions);

            // Assert - Score should equal total questions
            assertThat(score)
                    .as("All correct answers should yield maximum score")
                    .isEqualTo(numQuestions);
        }
    }

    /**
     * Property: All wrong answers yield zero score
     */
    @Test
    @Transactional
    void allWrongAnswersYieldZeroScore_property() {
        for (int i = 0; i < 100; i++) {
            int numQuestions = Arbitraries.integers().between(1, 15).sample();
            
            // Generate questions
            List<QuizVO.QuestionVO> questions = IntStream.rangeClosed(1, numQuestions)
                    .mapToObj(qId -> {
                        QuizVO.QuestionVO q = new QuizVO.QuestionVO();
                        q.setQuestionId(qId);
                        q.setQuestion("Question " + qId);
                        q.setOptions(Arrays.asList("A", "B", "C", "D"));
                        q.setCorrectAnswer(Arbitraries.of("A", "B", "C", "D").sample());
                        return q;
                    })
                    .collect(Collectors.toList());

            // Submit all wrong answers
            List<SubmitAnswersDTO.AnswerDTO> userAnswers = questions.stream()
                    .map(q -> {
                        SubmitAnswersDTO.AnswerDTO answer = new SubmitAnswersDTO.AnswerDTO();
                        answer.setQuestionId(q.getQuestionId());
                        // Pick a wrong answer
                        String wrongAnswer = Arbitraries.of("A", "B", "C", "D")
                                .filter(a -> !a.equals(q.getCorrectAnswer()))
                                .sample();
                        answer.setAnswer(wrongAnswer);
                        return answer;
                    })
                    .collect(Collectors.toList());

            int score = quizService.calculateScore(userAnswers, questions);

            // Assert - Score should be zero
            assertThat(score)
                    .as("All wrong answers should yield zero score")
                    .isEqualTo(0);
        }
    }


    /**
     * Feature: llm-language-learning-platform, Property 10: Quiz result persistence
     * 
     * For any completed quiz, the learning record should contain the correct score,
     * difficulty level, and completion timestamp.
     * 
     * Validates: Requirements 4.3
     */
    @Test
    @Transactional
    void quizResultPersistence_property() {
        // Create test user
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();

        Arbitrary<String> difficulties = Arbitraries.of("EASY", "MEDIUM", "HARD");
        Arbitrary<Integer> questionCounts = Arbitraries.integers().between(3, 8);

        for (int i = 0; i < 100; i++) {
            String difficulty = difficulties.sample();
            int numQuestions = questionCounts.sample();

            // Create a quiz directly in the database (simulating AI generation)
            Quiz quiz = createTestQuiz(userId, difficulty, numQuestions);
            
            // Parse questions to get correct answers
            List<QuizVO.QuestionVO> questions = parseQuestions(quiz.getQuestions());

            // Generate random answers
            List<SubmitAnswersDTO.AnswerDTO> userAnswers = questions.stream()
                    .map(q -> {
                        SubmitAnswersDTO.AnswerDTO answer = new SubmitAnswersDTO.AnswerDTO();
                        answer.setQuestionId(q.getQuestionId());
                        // Randomly correct or incorrect
                        boolean makeCorrect = Arbitraries.of(true, false).sample();
                        if (makeCorrect) {
                            answer.setAnswer(q.getCorrectAnswer());
                        } else {
                            answer.setAnswer(Arbitraries.of("A", "B", "C", "D")
                                    .filter(a -> !a.equals(q.getCorrectAnswer()))
                                    .sample());
                        }
                        return answer;
                    })
                    .collect(Collectors.toList());

            // Calculate expected score
            int expectedScore = quizService.calculateScore(userAnswers, questions);

            LocalDateTime beforeSubmit = LocalDateTime.now().minusSeconds(1);

            // Submit answers
            QuizResultVO result = quizService.submitAnswers(quiz.getId(), userAnswers, userId);

            LocalDateTime afterSubmit = LocalDateTime.now().plusSeconds(1);

            // Verify quiz result
            assertThat(result.getQuizId()).isEqualTo(quiz.getId());
            assertThat(result.getUserScore()).isEqualTo(expectedScore);
            assertThat(result.getDifficulty()).isEqualTo(difficulty);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletedAt()).isAfterOrEqualTo(beforeSubmit);
            assertThat(result.getCompletedAt()).isBeforeOrEqualTo(afterSubmit);

            // Verify learning record was created
            List<LearningRecord> records = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getActivityType, ActivityType.QUIZ.name())
                    .eq(LearningRecord::getActivityId, quiz.getId())
            );

            assertThat(records).isNotEmpty();
            LearningRecord record = records.get(records.size() - 1);
            assertThat(record.getUserId()).isEqualTo(userId);
            assertThat(record.getActivityType()).isEqualTo(ActivityType.QUIZ.name());
            assertThat(record.getActivityId()).isEqualTo(quiz.getId());
            assertThat(record.getActivityTime()).isAfterOrEqualTo(beforeSubmit);
            assertThat(record.getActivityTime()).isBeforeOrEqualTo(afterSubmit);

            // Verify quiz was updated in database
            Quiz updatedQuiz = quizMapper.selectById(quiz.getId());
            assertThat(updatedQuiz.getUserScore()).isEqualTo(expectedScore);
            assertThat(updatedQuiz.getDifficulty()).isEqualTo(difficulty);
            assertThat(updatedQuiz.getCompletedAt()).isNotNull();
        }
    }

    /**
     * Helper method to create a test user
     */
    private UserVO createTestUser() {
        RegisterDTO dto = new RegisterDTO();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        dto.setUsername("quizuser" + uniqueSuffix);
        dto.setPassword("password123");
        dto.setEmail("quiz" + uniqueSuffix + "@test.com");
        return userService.register(dto);
    }

    /**
     * Helper method to create a test quiz in the database
     */
    private Quiz createTestQuiz(Long userId, String difficulty, int numQuestions) {
        List<QuizVO.QuestionVO> questions = IntStream.rangeClosed(1, numQuestions)
                .mapToObj(qId -> {
                    QuizVO.QuestionVO q = new QuizVO.QuestionVO();
                    q.setQuestionId(qId);
                    q.setQuestion("Test question " + qId + "?");
                    q.setOptions(Arrays.asList("Option A", "Option B", "Option C", "Option D"));
                    q.setCorrectAnswer(Arbitraries.of("A", "B", "C", "D").sample());
                    return q;
                })
                .collect(Collectors.toList());

        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setDifficulty(difficulty);
        quiz.setTotalScore(numQuestions);
        quiz.setCreatedAt(LocalDateTime.now());

        try {
            quiz.setQuestions(objectMapper.writeValueAsString(questions));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize questions", e);
        }

        quizMapper.insert(quiz);
        return quiz;
    }

    /**
     * Helper method to parse questions JSON
     */
    private List<QuizVO.QuestionVO> parseQuestions(String questionsJson) {
        try {
            return objectMapper.readValue(questionsJson, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<QuizVO.QuestionVO>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
