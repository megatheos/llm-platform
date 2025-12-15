package com.llmplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a custom scenario
 */
@Data
public class CreateScenarioDTO {

    @NotBlank(message = "Scenario name is required")
    @Size(min = 1, max = 100, message = "Scenario name must be between 1 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(min = 1, max = 50, message = "Category must be between 1 and 50 characters")
    private String category;
}
