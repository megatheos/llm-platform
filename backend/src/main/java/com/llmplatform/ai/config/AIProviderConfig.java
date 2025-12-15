package com.llmplatform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for AI providers
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.gateway")
public class AIProviderConfig {

    /**
     * Default provider to use
     */
    private String defaultProvider = "ollama";

    /**
     * Provider-specific configurations
     */
    private Map<String, ProviderSettings> providers = new HashMap<>();

    @Data
    public static class ProviderSettings {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl;
        private String model;
        private int timeout = 60000;
        private int priority = 100;
    }

    /**
     * Get settings for a specific provider
     */
    public ProviderSettings getProviderSettings(String providerName) {
        return providers.getOrDefault(providerName, new ProviderSettings());
    }

    /**
     * Check if a provider is enabled
     */
    public boolean isProviderEnabled(String providerName) {
        ProviderSettings settings = providers.get(providerName);
        return settings != null && settings.isEnabled();
    }
}
