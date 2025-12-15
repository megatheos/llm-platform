package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for quiz history item
 */
@Data
public class QuizHistoryVO {

    private Long id;
    
    private String difficulty;
    
    private Integer totalScore;
    
    private Integer userScore;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;
}
