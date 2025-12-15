package com.llmplatform.ai.gateway;

import com.llmplatform.ai.config.AIProviderConfig;
import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.provider.AIProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * AI Gateway implementation with provider routing and failover support
 */
@Slf4j
@Service
public class AIGatewayImpl implements AIGateway {

    private final AIProviderConfig config;
    private final List<AIProvider> providers;

    public AIGatewayImpl(AIProviderConfig config, List<AIProvider> providers) {
        this.config = config;
        this.providers = providers;
        log.info("AI Gateway initialized with {} providers", providers.size());
        providers.forEach(p -> log.info("  - {} (available: {})", p.getName(), p.isAvailable()));
    }

    @Override
    public AIResponse generate(AIRequest request) {
        AIProvider primaryProvider = getPrimaryProvider();
        
        if (primaryProvider == null) {
            log.error("No AI provider available");
            return AIResponse.failure("gateway", "No AI provider is available");
        }

        // Try primary provider first
        log.debug("Attempting generation with primary provider: {}", primaryProvider.getName());
        AIResponse response = primaryProvider.generate(request);
        
        if (response.isSuccess()) {
            return response;
        }

        // Failover to other providers
        log.warn("Primary provider {} failed: {}", primaryProvider.getName(), response.getErrorMessage());
        return attemptFailover(request, primaryProvider);
    }


    @Override
    public AIResponse generateWithContext(AIRequest request, List<AIRequest.Message> context) {
        // Merge context with request messages
        AIRequest contextualRequest = AIRequest.builder()
                .prompt(request.getPrompt())
                .systemMessage(request.getSystemMessage())
                .messages(mergeMessages(context, request.getMessages()))
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .additionalParams(request.getAdditionalParams())
                .build();

        return generate(contextualRequest);
    }

    @Override
    public String getActiveProvider() {
        AIProvider primary = getPrimaryProvider();
        return primary != null ? primary.getName() : null;
    }

    @Override
    public boolean isAvailable() {
        return providers.stream().anyMatch(AIProvider::isAvailable);
    }

    /**
     * Get the primary provider based on configuration
     */
    private AIProvider getPrimaryProvider() {
        String defaultProviderName = config.getDefaultProvider();
        
        // First try to get the configured default provider
        Optional<AIProvider> defaultProvider = providers.stream()
                .filter(p -> p.getName().equals(defaultProviderName))
                .filter(AIProvider::isAvailable)
                .findFirst();

        if (defaultProvider.isPresent()) {
            return defaultProvider.get();
        }

        // Fall back to any available provider, sorted by priority
        return providers.stream()
                .filter(AIProvider::isAvailable)
                .min(Comparator.comparingInt(AIProvider::getPriority))
                .orElse(null);
    }

    /**
     * Attempt failover to other available providers
     */
    private AIResponse attemptFailover(AIRequest request, AIProvider failedProvider) {
        List<AIProvider> fallbackProviders = providers.stream()
                .filter(p -> !p.getName().equals(failedProvider.getName()))
                .filter(AIProvider::isAvailable)
                .sorted(Comparator.comparingInt(AIProvider::getPriority))
                .toList();

        for (AIProvider fallback : fallbackProviders) {
            log.info("Attempting failover to provider: {}", fallback.getName());
            
            try {
                AIResponse response = fallback.generate(request);
                if (response.isSuccess()) {
                    log.info("Failover to {} successful", fallback.getName());
                    return response;
                }
                log.warn("Failover provider {} failed: {}", fallback.getName(), response.getErrorMessage());
            } catch (Exception e) {
                log.warn("Failover provider {} threw exception: {}", fallback.getName(), e.getMessage());
            }
        }

        log.error("All AI providers failed");
        return AIResponse.failure("gateway", "All AI providers failed to generate response");
    }

    /**
     * Merge context messages with request messages
     */
    private List<AIRequest.Message> mergeMessages(List<AIRequest.Message> context, List<AIRequest.Message> requestMessages) {
        List<AIRequest.Message> merged = new ArrayList<>();
        
        if (context != null) {
            merged.addAll(context);
        }
        
        if (requestMessages != null) {
            merged.addAll(requestMessages);
        }
        
        return merged;
    }
}
