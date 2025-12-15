package com.llmplatform.ai.provider;

import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;

/**
 * Interface for AI service providers
 * Implementations handle communication with specific AI services (OpenAI, Ollama, etc.)
 */
public interface AIProvider {

    /**
     * Get the unique name of this provider
     * @return provider name (e.g., "openai", "ollama")
     */
    String getName();

    /**
     * Check if this provider is currently available and configured
     * @return true if the provider can accept requests
     */
    boolean isAvailable();

    /**
     * Generate a response for the given request
     * @param request the AI request containing prompt and parameters
     * @return normalized AI response
     */
    AIResponse generate(AIRequest request);

    /**
     * Get the priority of this provider (lower = higher priority)
     * Used for failover ordering
     * @return priority value
     */
    default int getPriority() {
        return 100;
    }
}
