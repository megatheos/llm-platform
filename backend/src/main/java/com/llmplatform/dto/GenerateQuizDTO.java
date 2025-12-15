package com.llmplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO for quiz generation request
 */
@Data
public class GenerateQuizDTO {

    @NotBlank(message = "Difficulty level is required")
    @Pattern(regexp = "^(EASY|MEDIUM|HARD)$", message = "Difficulty must be EASY, MEDIUM, or HARD")
    private String difficulty;
    
    /**
     * Optional: target language for the quiz
     */
    private String targetLang;
    
    /**
     * Optional: number of questions (default: 5)
     */
    private Integer questionCount;
}
