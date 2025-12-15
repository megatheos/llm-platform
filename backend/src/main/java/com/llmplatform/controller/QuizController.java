package com.llmplatform.controller;

import com.llmplatform.common.Result;
import com.llmplatform.dto.GenerateQuizDTO;
import com.llmplatform.dto.SubmitAnswersDTO;
import com.llmplatform.service.QuizService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.QuizHistoryVO;
import com.llmplatform.vo.QuizResultVO;
import com.llmplatform.vo.QuizVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quiz Controller
 * Handles quiz generation, submission, and history operations
 */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final JwtUtil jwtUtil;

    /**
     * Generate a new quiz with AI-generated questions
     * POST /api/quiz/generate
     * 
     * @param dto quiz generation parameters (difficulty, optional targetLang, optional questionCount)
     * @param authHeader Authorization header containing the Bearer token
     * @return generated quiz with questions
     */
    @PostMapping("/generate")
    public Result<QuizVO> generateQuiz(@Valid @RequestBody GenerateQuizDTO dto,
                                       @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        QuizVO quiz = quizService.generateQuiz(
            dto.getDifficulty(), 
            dto.getTargetLang(), 
            dto.getQuestionCount(), 
            userId
        );
        return Result.success(quiz);
    }

    /**
     * Submit quiz answers and get results
     * POST /api/quiz/{id}/submit
     * 
     * @param id quiz ID
     * @param dto answers submission data
     * @param authHeader Authorization header containing the Bearer token
     * @return quiz result with score and answer details
     */
    @PostMapping("/{id}/submit")
    public Result<QuizResultVO> submitAnswers(@PathVariable Long id,
                                               @Valid @RequestBody SubmitAnswersDTO dto,
                                               @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        QuizResultVO result = quizService.submitAnswers(id, dto.getAnswers(), userId);
        return Result.success(result);
    }

    /**
     * Get quiz history for the current user
     * GET /api/quiz/history
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return list of quiz history items
     */
    @GetMapping("/history")
    public Result<List<QuizHistoryVO>> getHistory(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<QuizHistoryVO> history = quizService.getHistory(userId);
        return Result.success(history);
    }

    /**
     * Extract user ID from Authorization header
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.getUserIdFromToken(token);
    }
}
