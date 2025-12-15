package com.llmplatform.ai.util;

import com.llmplatform.ai.dto.AIResponse;

/**
 * Utility class for normalizing AI responses from different providers
 * Ensures consistent response format regardless of the source provider
 */
public class AIResponseNormalizer {

    /**
     * Normalize an AI response to ensure all required fields are present
     * Required fields: content, model, usage (with promptTokens, completionTokens, totalTokens)
     * 
     * @param response the raw response from a provider
     * @param providerName the name of the provider
     * @return normalized response with all required fields
     */
    public static AIResponse normalize(AIResponse response, String providerName) {
        if (response == null) {
            return AIResponse.builder()
                    .content("")
                    .model("unknown")
                    .usage(createDefaultUsage())
                    .provider(providerName)
                    .success(false)
                    .errorMessage("Response was null")
                    .build();
        }

        return AIResponse.builder()
                .content(normalizeContent(response.getContent()))
                .model(normalizeModel(response.getModel()))
                .usage(normalizeUsage(response.getUsage()))
                .provider(providerName != null ? providerName : response.getProvider())
                .success(response.isSuccess())
                .errorMessage(response.getErrorMessage())
                .build();
    }

    /**
     * Check if a response has all required fields properly set
     */
    public static boolean hasRequiredFields(AIResponse response) {
        if (response == null) {
            return false;
        }

        // Check content field
        if (response.getContent() == null) {
            return false;
        }

        // Check model field
        if (response.getModel() == null || response.getModel().isEmpty()) {
            return false;
        }

        // Check usage field
        if (response.getUsage() == null) {
            return false;
        }

        AIResponse.Usage usage = response.getUsage();
        return usage.getPromptTokens() != null &&
               usage.getCompletionTokens() != null &&
               usage.getTotalTokens() != null;
    }

    private static String normalizeContent(String content) {
        return content != null ? content : "";
    }

    private static String normalizeModel(String model) {
        return (model != null && !model.isEmpty()) ? model : "unknown";
    }

    private static AIResponse.Usage normalizeUsage(AIResponse.Usage usage) {
        if (usage == null) {
            return createDefaultUsage();
        }

        return AIResponse.Usage.builder()
                .promptTokens(usage.getPromptTokens() != null ? usage.getPromptTokens() : 0)
                .completionTokens(usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0)
                .totalTokens(usage.getTotalTokens() != null ? usage.getTotalTokens() : 
                        (usage.getPromptTokens() != null ? usage.getPromptTokens() : 0) + 
                        (usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0))
                .build();
    }

    private static AIResponse.Usage createDefaultUsage() {
        return AIResponse.Usage.builder()
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .build();
    }
}
