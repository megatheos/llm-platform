package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VO for quiz result after submission
 */
@Data
public class QuizResultVO {

    private Long quizId;
    
    private Integer totalScore;
    
    private Integer userScore;
    
    private String difficulty;
    
    private List<AnswerResultVO> answerResults;
    
    private LocalDateTime completedAt;

    @Data
    public static class AnswerResultVO {
        private Integer questionId;
        private String question;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
    }
}
