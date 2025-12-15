package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VO for quiz data
 */
@Data
public class QuizVO {

    private Long id;
    
    private Long userId;
    
    private String difficulty;
    
    private String targetLang;
    
    private List<QuestionVO> questions;
    
    private Integer totalScore;
    
    private Integer userScore;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;

    @Data
    public static class QuestionVO {
        private Integer questionId;
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String userAnswer;
    }
}
