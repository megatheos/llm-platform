package com.llmplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for starting a dialogue session
 */
@Data
public class StartSessionDTO {

    @NotNull(message = "Scenario ID is required")
    private Long scenarioId;
    
    /**
     * Optional: target language for the dialogue
     */
    private String targetLang;
}
