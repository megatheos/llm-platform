package com.llmplatform.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Unified AI request DTO for all providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {

    /**
     * The prompt or user message
     */
    private String prompt;

    /**
     * System message for context setting
     */
    private String systemMessage;

    /**
     * Conversation history for multi-turn dialogues
     */
    private List<Message> messages;

    /**
     * Temperature for response randomness (0.0 - 1.0)
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Maximum tokens in response
     */
    @Builder.Default
    private Integer maxTokens = 2048;

    /**
     * Additional provider-specific parameters
     */
    private Map<String, Object> additionalParams;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;  // "user", "assistant", "system"
        private String content;
    }
}
