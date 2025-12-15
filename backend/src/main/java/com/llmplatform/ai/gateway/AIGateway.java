package com.llmplatform.ai.gateway;

import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;

import java.util.List;

/**
 * Unified AI Gateway interface for routing requests to AI providers
 */
public interface AIGateway {

    /**
     * Generate a response using the configured AI provider
     * @param request the AI request
     * @return normalized AI response
     */
    AIResponse generate(AIRequest request);

    /**
     * Generate a response with conversation context
     * @param request the AI request
     * @param context previous messages for context
     * @return normalized AI response
     */
    AIResponse generateWithContext(AIRequest request, List<AIRequest.Message> context);

    /**
     * Get the name of the currently active provider
     * @return provider name
     */
    String getActiveProvider();

    /**
     * Check if any AI provider is available
     * @return true if at least one provider is available
     */
    boolean isAvailable();
}
