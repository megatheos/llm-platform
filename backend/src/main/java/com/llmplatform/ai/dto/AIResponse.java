package com.llmplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized AI response DTO across all providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {

    /**
     * The generated content/response text
     */
    private String content;

    /**
     * The model used for generation
     */
    private String model;

    /**
     * Token usage information
     */
    private Usage usage;

    /**
     * Provider name that generated this response
     */
    private String provider;

    /**
     * Whether the response was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Error message if the request failed
     */
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    /**
     * Create a failed response
     */
    public static AIResponse failure(String provider, String errorMessage) {
        return AIResponse.builder()
                .provider(provider)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
