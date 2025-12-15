package com.llmplatform.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.ai.config.AIProviderConfig;
import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.util.AIResponseNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI API provider implementation
 */
@Slf4j
@Component
public class OpenAIProvider implements AIProvider {

    private static final String PROVIDER_NAME = "openai";

    private final AIProviderConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIProvider(AIProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        AIProviderConfig.ProviderSettings settings = config.getProviderSettings(PROVIDER_NAME);
        return settings.isEnabled() && 
               settings.getApiKey() != null && 
               !settings.getApiKey().isEmpty();
    }

    @Override
    public int getPriority() {
        return config.getProviderSettings(PROVIDER_NAME).getPriority();
    }


    @Override
    public AIResponse generate(AIRequest request) {
        if (!isAvailable()) {
            return AIResponse.failure(PROVIDER_NAME, "OpenAI provider is not available or configured");
        }

        AIProviderConfig.ProviderSettings settings = config.getProviderSettings(PROVIDER_NAME);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(settings.getApiKey());

            Map<String, Object> requestBody = buildRequestBody(request, settings);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = settings.getBaseUrl() + "/chat/completions";
            
            log.debug("Sending request to OpenAI: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            return AIResponse.failure(PROVIDER_NAME, "OpenAI API call failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(AIRequest request, AIProviderConfig.ProviderSettings settings) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", settings.getModel());
        body.put("temperature", request.getTemperature());
        body.put("max_tokens", request.getMaxTokens());

        List<Map<String, String>> messages = new ArrayList<>();

        // Add system message if present
        if (request.getSystemMessage() != null && !request.getSystemMessage().isEmpty()) {
            messages.add(Map.of("role", "system", "content", request.getSystemMessage()));
        }

        // Add conversation history if present
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (AIRequest.Message msg : request.getMessages()) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        // Add current prompt as user message
        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            messages.add(Map.of("role", "user", "content", request.getPrompt()));
        }

        body.put("messages", messages);

        return body;
    }

    private AIResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            String content = root.path("choices").get(0).path("message").path("content").asText();
            String model = root.path("model").asText();
            
            JsonNode usageNode = root.path("usage");
            AIResponse.Usage usage = AIResponse.Usage.builder()
                    .promptTokens(usageNode.path("prompt_tokens").asInt())
                    .completionTokens(usageNode.path("completion_tokens").asInt())
                    .totalTokens(usageNode.path("total_tokens").asInt())
                    .build();

            AIResponse response = AIResponse.builder()
                    .content(content)
                    .model(model)
                    .usage(usage)
                    .provider(PROVIDER_NAME)
                    .success(true)
                    .build();

            // Normalize response to ensure consistent format
            return AIResponseNormalizer.normalize(response, PROVIDER_NAME);

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage(), e);
            return AIResponse.failure(PROVIDER_NAME, "Failed to parse response: " + e.getMessage());
        }
    }
}
