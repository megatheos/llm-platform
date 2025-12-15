package com.llmplatform.service;

import com.llmplatform.dto.SubmitAnswersDTO;
import com.llmplatform.vo.QuizHistoryVO;
import com.llmplatform.vo.QuizResultVO;
import com.llmplatform.vo.QuizVO;

import java.util.List;

/**
 * Quiz service interface
 */
public interface QuizService {

    /**
     * Generate a quiz with AI-generated questions
     * 
     * @param difficulty difficulty level (EASY, MEDIUM, HARD)
     * @param targetLang optional target language
     * @param questionCount number of questions (default: 5)
     * @param userId user ID
     * @return generated quiz
     */
    QuizVO generateQuiz(String difficulty, String targetLang, Integer questionCount, Long userId);

    /**
     * Submit quiz answers and calculate score
     * 
     * @param quizId quiz ID
     * @param answers list of answers
     * @param userId user ID
     * @return quiz result with score
     */
    QuizResultVO submitAnswers(Long quizId, List<SubmitAnswersDTO.AnswerDTO> answers, Long userId);

    /**
     * Get quiz history for a user
     * 
     * @param userId user ID
     * @return list of quiz history items
     */
    List<QuizHistoryVO> getHistory(Long userId);

    /**
     * Get quiz by ID
     * 
     * @param quizId quiz ID
     * @return quiz VO or null if not found
     */
    QuizVO getQuizById(Long quizId);

    /**
     * Calculate score for given answers against correct answers
     * This is a pure function for testing purposes
     * 
     * @param userAnswers list of user answers
     * @param correctAnswers list of correct answers (from quiz questions)
     * @return calculated score
     */
    int calculateScore(List<SubmitAnswersDTO.AnswerDTO> userAnswers, List<QuizVO.QuestionVO> questions);
}
