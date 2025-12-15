package com.llmplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for word query requests
 */
@Data
public class WordQueryDTO {

    @NotBlank(message = "Word is required")
    @Size(min = 1, max = 100, message = "Word must be between 1 and 100 characters")
    private String word;

    @NotBlank(message = "Source language is required")
    @Size(max = 20, message = "Source language code must not exceed 20 characters")
    private String sourceLang;

    @NotBlank(message = "Target language is required")
    @Size(max = 20, message = "Target language code must not exceed 20 characters")
    private String targetLang;
}
