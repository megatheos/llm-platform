package com.llmplatform.property;

import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.util.AIResponseNormalizer;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for AI response format normalization
 * 
 * Feature: llm-language-learning-platform, Property 13: AI response format normalization
 * 
 * For any AI provider response, the normalized output should contain the same required fields
 * (content, model, usage) regardless of the original provider.
 * 
 * Validates: Requirements 6.5
 */
class AIResponseNormalizerPropertyTest {

    /**
     * Property: For any AI response (complete or partial), after normalization,
     * all required fields (content, model, usage with token counts) must be present and non-null.
     */
    @Property(tries = 100)
    void normalizedResponseAlwaysHasRequiredFields(
            @ForAll("aiResponses") AIResponse rawResponse,
            @ForAll @StringLength(min = 1, max = 20) String providerName) {
        
        // Act
        AIResponse normalized = AIResponseNormalizer.normalize(rawResponse, providerName);
        
        // Assert - All required fields must be present
        assertThat(normalized).isNotNull();
        assertThat(normalized.getContent()).isNotNull();
        assertThat(normalized.getModel()).isNotNull();
        assertThat(normalized.getModel()).isNotEmpty();
        assertThat(normalized.getUsage()).isNotNull();
        assertThat(normalized.getUsage().getPromptTokens()).isNotNull();
        assertThat(normalized.getUsage().getCompletionTokens()).isNotNull();
        assertThat(normalized.getUsage().getTotalTokens()).isNotNull();
        
        // Assert - hasRequiredFields should return true for normalized response
        assertThat(AIResponseNormalizer.hasRequiredFields(normalized)).isTrue();
    }


    /**
     * Property: For any complete AI response with all fields set,
     * normalization should preserve the original content, model, and usage values.
     */
    @Property(tries = 100)
    void normalizationPreservesCompleteResponseData(
            @ForAll @StringLength(min = 0, max = 1000) String content,
            @ForAll @StringLength(min = 1, max = 50) String model,
            @ForAll @IntRange(min = 0, max = 10000) int promptTokens,
            @ForAll @IntRange(min = 0, max = 10000) int completionTokens,
            @ForAll @StringLength(min = 1, max = 20) String providerName) {
        
        // Arrange - Create a complete response
        AIResponse.Usage usage = AIResponse.Usage.builder()
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .build();
        
        AIResponse rawResponse = AIResponse.builder()
                .content(content)
                .model(model)
                .usage(usage)
                .provider("original-provider")
                .success(true)
                .build();
        
        // Act
        AIResponse normalized = AIResponseNormalizer.normalize(rawResponse, providerName);
        
        // Assert - Original values should be preserved
        assertThat(normalized.getContent()).isEqualTo(content);
        assertThat(normalized.getModel()).isEqualTo(model);
        assertThat(normalized.getUsage().getPromptTokens()).isEqualTo(promptTokens);
        assertThat(normalized.getUsage().getCompletionTokens()).isEqualTo(completionTokens);
        assertThat(normalized.getUsage().getTotalTokens()).isEqualTo(promptTokens + completionTokens);
        assertThat(normalized.getProvider()).isEqualTo(providerName);
    }

    /**
     * Property: For any response with missing usage data,
     * normalization should provide default values (zeros) for token counts.
     */
    @Property(tries = 100)
    void normalizationHandlesMissingUsage(
            @ForAll @StringLength(min = 0, max = 500) String content,
            @ForAll @StringLength(min = 1, max = 50) String model,
            @ForAll @StringLength(min = 1, max = 20) String providerName) {
        
        // Arrange - Create response without usage
        AIResponse rawResponse = AIResponse.builder()
                .content(content)
                .model(model)
                .usage(null)
                .provider("original-provider")
                .success(true)
                .build();
        
        // Act
        AIResponse normalized = AIResponseNormalizer.normalize(rawResponse, providerName);
        
        // Assert - Usage should have default values
        assertThat(normalized.getUsage()).isNotNull();
        assertThat(normalized.getUsage().getPromptTokens()).isEqualTo(0);
        assertThat(normalized.getUsage().getCompletionTokens()).isEqualTo(0);
        assertThat(normalized.getUsage().getTotalTokens()).isEqualTo(0);
    }

    /**
     * Property: Null responses should be normalized to a valid failure response
     * with all required fields present.
     */
    @Property(tries = 100)
    void normalizationHandlesNullResponse(
            @ForAll @StringLength(min = 1, max = 20) String providerName) {
        
        // Act
        AIResponse normalized = AIResponseNormalizer.normalize(null, providerName);
        
        // Assert - Should return a valid failure response
        assertThat(normalized).isNotNull();
        assertThat(normalized.getContent()).isNotNull();
        assertThat(normalized.getModel()).isNotNull();
        assertThat(normalized.getUsage()).isNotNull();
        assertThat(normalized.isSuccess()).isFalse();
        assertThat(normalized.getProvider()).isEqualTo(providerName);
        assertThat(AIResponseNormalizer.hasRequiredFields(normalized)).isTrue();
    }

    /**
     * Arbitrary provider for generating various AI responses with different completeness levels
     */
    @Provide
    Arbitrary<AIResponse> aiResponses() {
        Arbitrary<String> contents = Arbitraries.strings().ofMaxLength(500).injectNull(0.1);
        Arbitrary<String> models = Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just(null),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
        );
        Arbitrary<AIResponse.Usage> usages = Arbitraries.oneOf(
                Arbitraries.just(null),
                partialUsages(),
                completeUsages()
        );
        Arbitrary<Boolean> successes = Arbitraries.of(true, false);
        Arbitrary<String> providers = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);

        return Combinators.combine(contents, models, usages, successes, providers)
                .as((content, model, usage, success, provider) -> 
                        AIResponse.builder()
                                .content(content)
                                .model(model)
                                .usage(usage)
                                .success(success)
                                .provider(provider)
                                .build());
    }

    private Arbitrary<AIResponse.Usage> partialUsages() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 1000).injectNull(0.3),
                Arbitraries.integers().between(0, 1000).injectNull(0.3),
                Arbitraries.integers().between(0, 2000).injectNull(0.3)
        ).as((prompt, completion, total) -> 
                AIResponse.Usage.builder()
                        .promptTokens(prompt)
                        .completionTokens(completion)
                        .totalTokens(total)
                        .build());
    }

    private Arbitrary<AIResponse.Usage> completeUsages() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 1000),
                Arbitraries.integers().between(0, 1000)
        ).as((prompt, completion) -> 
                AIResponse.Usage.builder()
                        .promptTokens(prompt)
                        .completionTokens(completion)
                        .totalTokens(prompt + completion)
                        .build());
    }
}
