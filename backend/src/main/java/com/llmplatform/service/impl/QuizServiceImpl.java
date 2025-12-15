package com.llmplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.gateway.AIGateway;
import com.llmplatform.common.ActivityType;
import com.llmplatform.dto.SubmitAnswersDTO;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Quiz;
import com.llmplatform.exception.BusinessException;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.QuizMapper;
import com.llmplatform.service.QuizService;
import com.llmplatform.vo.QuizHistoryVO;
import com.llmplatform.vo.QuizResultVO;
import com.llmplatform.vo.QuizVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quiz service implementation with AI question generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizMapper quizMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final AIGateway aiGateway;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_QUESTION_COUNT = 5;


    @Override
    @Transactional
    public QuizVO generateQuiz(String difficulty, String targetLang, Integer questionCount, Long userId) {
        int numQuestions = questionCount != null && questionCount > 0 ? questionCount : DEFAULT_QUESTION_COUNT;
        String lang = targetLang != null ? targetLang : "en";

        log.info("Generating quiz: difficulty={}, targetLang={}, questionCount={}, userId={}", 
                difficulty, lang, numQuestions, userId);

        // Generate questions via AI
        List<QuizVO.QuestionVO> questions = generateQuestionsViaAI(difficulty, lang, numQuestions);

        // Calculate total score (1 point per question)
        int totalScore = questions.size();

        // Create quiz entity
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setDifficulty(difficulty);
        quiz.setTotalScore(totalScore);
        quiz.setUserScore(null); // Not yet completed
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setCompletedAt(null);

        try {
            quiz.setQuestions(objectMapper.writeValueAsString(questions));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize questions", e);
            throw new BusinessException("SERIALIZATION_ERROR", "Failed to serialize quiz questions");
        }

        quizMapper.insert(quiz);

        return convertToVO(quiz, questions);
    }

    @Override
    @Transactional
    public QuizResultVO submitAnswers(Long quizId, List<SubmitAnswersDTO.AnswerDTO> answers, Long userId) {
        Quiz quiz = quizMapper.selectById(quizId);
        
        if (quiz == null) {
            throw new BusinessException("QUIZ_NOT_FOUND", "Quiz not found");
        }

        if (!quiz.getUserId().equals(userId)) {
            throw new BusinessException("UNAUTHORIZED", "You are not authorized to submit answers for this quiz");
        }

        if (quiz.getCompletedAt() != null) {
            throw new BusinessException("QUIZ_COMPLETED", "This quiz has already been completed");
        }

        // Parse questions
        List<QuizVO.QuestionVO> questions = parseQuestions(quiz.getQuestions());

        // Calculate score
        int score = calculateScore(answers, questions);

        // Update quiz with user answers and score
        updateQuestionsWithUserAnswers(questions, answers);
        
        quiz.setUserScore(score);
        quiz.setCompletedAt(LocalDateTime.now());
        
        try {
            quiz.setQuestions(objectMapper.writeValueAsString(questions));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize questions with answers", e);
            throw new BusinessException("SERIALIZATION_ERROR", "Failed to save quiz answers");
        }

        quizMapper.updateById(quiz);

        // Record in learning records
        recordQuizResult(userId, quiz);

        return buildQuizResult(quiz, questions, answers);
    }

    @Override
    public List<QuizHistoryVO> getHistory(Long userId) {
        List<Quiz> quizzes = quizMapper.selectList(
            new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getUserId, userId)
                .orderByDesc(Quiz::getCreatedAt)
        );

        return quizzes.stream()
            .map(this::convertToHistoryVO)
            .collect(Collectors.toList());
    }

    @Override
    public QuizVO getQuizById(Long quizId) {
        Quiz quiz = quizMapper.selectById(quizId);
        if (quiz == null) {
            return null;
        }
        List<QuizVO.QuestionVO> questions = parseQuestions(quiz.getQuestions());
        return convertToVO(quiz, questions);
    }

    @Override
    public int calculateScore(List<SubmitAnswersDTO.AnswerDTO> userAnswers, List<QuizVO.QuestionVO> questions) {
        if (userAnswers == null || questions == null) {
            return 0;
        }

        // Create a map of questionId -> correctAnswer for efficient lookup
        Map<Integer, String> correctAnswerMap = questions.stream()
            .collect(Collectors.toMap(
                QuizVO.QuestionVO::getQuestionId,
                q -> q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim().toLowerCase() : ""
            ));

        int score = 0;
        for (SubmitAnswersDTO.AnswerDTO answer : userAnswers) {
            String correctAnswer = correctAnswerMap.get(answer.getQuestionId());
            if (correctAnswer != null && answer.getAnswer() != null) {
                String userAnswer = answer.getAnswer().trim().toLowerCase();
                if (correctAnswer.equals(userAnswer)) {
                    score++;
                }
            }
        }

        return score;
    }


    /**
     * Generate quiz questions via AI
     */
    private List<QuizVO.QuestionVO> generateQuestionsViaAI(String difficulty, String targetLang, int numQuestions) {
        String systemMessage = String.format(
            "You are a language learning quiz generator. Generate multiple choice questions for %s language learning. " +
            "Respond ONLY with a valid JSON array (no markdown, no code blocks). " +
            "Each question object must have: questionId (integer starting from 1), question (string), " +
            "options (array of 4 strings labeled A, B, C, D), correctAnswer (one of A, B, C, or D).",
            targetLang
        );

        String difficultyDescription = getDifficultyDescription(difficulty);
        
        String prompt = String.format(
            "Generate %d %s multiple choice questions for %s language learning. " +
            "Questions should test vocabulary, grammar, or comprehension. " +
            "Return ONLY a JSON array of question objects.",
            numQuestions, difficultyDescription, targetLang
        );

        AIRequest request = AIRequest.builder()
            .systemMessage(systemMessage)
            .prompt(prompt)
            .temperature(0.7)
            .maxTokens(2048)
            .build();

        AIResponse response = aiGateway.generate(request);

        if (!response.isSuccess()) {
            log.error("AI generation failed: {}", response.getErrorMessage());
            throw new BusinessException("AI_ERROR", "Failed to generate quiz questions: " + response.getErrorMessage());
        }

        return parseAIResponse(response.getContent());
    }

    /**
     * Get difficulty description for AI prompt
     */
    private String getDifficultyDescription(String difficulty) {
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> "easy (basic vocabulary and simple grammar)";
            case "MEDIUM" -> "medium (intermediate vocabulary and grammar)";
            case "HARD" -> "hard (advanced vocabulary, complex grammar, and idioms)";
            default -> "medium";
        };
    }

    /**
     * Parse AI response into list of questions
     */
    private List<QuizVO.QuestionVO> parseAIResponse(String content) {
        try {
            // Clean up the content - remove markdown code blocks if present
            String cleanContent = content.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7);
            } else if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3);
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
            }
            cleanContent = cleanContent.trim();

            return objectMapper.readValue(cleanContent, new TypeReference<List<QuizVO.QuestionVO>>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new BusinessException("PARSE_ERROR", "Failed to parse quiz questions from AI response");
        }
    }

    /**
     * Parse questions JSON string into list
     */
    private List<QuizVO.QuestionVO> parseQuestions(String questionsJson) {
        try {
            return objectMapper.readValue(questionsJson, new TypeReference<List<QuizVO.QuestionVO>>() {});
        } catch (Exception e) {
            log.error("Failed to parse questions JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Update questions with user answers
     */
    private void updateQuestionsWithUserAnswers(List<QuizVO.QuestionVO> questions, 
                                                 List<SubmitAnswersDTO.AnswerDTO> answers) {
        Map<Integer, String> answerMap = answers.stream()
            .collect(Collectors.toMap(
                SubmitAnswersDTO.AnswerDTO::getQuestionId,
                SubmitAnswersDTO.AnswerDTO::getAnswer
            ));

        for (QuizVO.QuestionVO question : questions) {
            question.setUserAnswer(answerMap.get(question.getQuestionId()));
        }
    }

    /**
     * Record quiz result in learning records
     */
    private void recordQuizResult(Long userId, Quiz quiz) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.QUIZ.name());
        record.setActivityId(quiz.getId());
        record.setActivityTime(LocalDateTime.now());
        learningRecordMapper.insert(record);
    }

    /**
     * Build quiz result VO
     */
    private QuizResultVO buildQuizResult(Quiz quiz, List<QuizVO.QuestionVO> questions,
                                          List<SubmitAnswersDTO.AnswerDTO> answers) {
        QuizResultVO result = new QuizResultVO();
        result.setQuizId(quiz.getId());
        result.setTotalScore(quiz.getTotalScore());
        result.setUserScore(quiz.getUserScore());
        result.setDifficulty(quiz.getDifficulty());
        result.setCompletedAt(quiz.getCompletedAt());

        Map<Integer, String> answerMap = answers.stream()
            .collect(Collectors.toMap(
                SubmitAnswersDTO.AnswerDTO::getQuestionId,
                SubmitAnswersDTO.AnswerDTO::getAnswer
            ));

        List<QuizResultVO.AnswerResultVO> answerResults = questions.stream()
            .map(q -> {
                QuizResultVO.AnswerResultVO ar = new QuizResultVO.AnswerResultVO();
                ar.setQuestionId(q.getQuestionId());
                ar.setQuestion(q.getQuestion());
                ar.setCorrectAnswer(q.getCorrectAnswer());
                String userAnswer = answerMap.get(q.getQuestionId());
                ar.setUserAnswer(userAnswer);
                ar.setIsCorrect(q.getCorrectAnswer() != null && userAnswer != null &&
                    q.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer.trim()));
                return ar;
            })
            .collect(Collectors.toList());

        result.setAnswerResults(answerResults);
        return result;
    }

    /**
     * Convert Quiz entity to QuizVO
     */
    private QuizVO convertToVO(Quiz quiz, List<QuizVO.QuestionVO> questions) {
        QuizVO vo = new QuizVO();
        vo.setId(quiz.getId());
        vo.setUserId(quiz.getUserId());
        vo.setDifficulty(quiz.getDifficulty());
        vo.setQuestions(questions);
        vo.setTotalScore(quiz.getTotalScore());
        vo.setUserScore(quiz.getUserScore());
        vo.setCreatedAt(quiz.getCreatedAt());
        vo.setCompletedAt(quiz.getCompletedAt());
        return vo;
    }

    /**
     * Convert Quiz entity to QuizHistoryVO
     */
    private QuizHistoryVO convertToHistoryVO(Quiz quiz) {
        QuizHistoryVO vo = new QuizHistoryVO();
        vo.setId(quiz.getId());
        vo.setDifficulty(quiz.getDifficulty());
        vo.setTotalScore(quiz.getTotalScore());
        vo.setUserScore(quiz.getUserScore());
        vo.setCreatedAt(quiz.getCreatedAt());
        vo.setCompletedAt(quiz.getCompletedAt());
        return vo;
    }
}
