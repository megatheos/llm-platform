package com.llmplatform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO for quiz answer submission
 */
@Data
public class SubmitAnswersDTO {

    @NotEmpty(message = "Answers are required")
    private List<AnswerDTO> answers;

    @Data
    public static class AnswerDTO {
        @NotNull(message = "Question ID is required")
        private Integer questionId;
        
        @NotNull(message = "Answer is required")
        private String answer;
    }
}
