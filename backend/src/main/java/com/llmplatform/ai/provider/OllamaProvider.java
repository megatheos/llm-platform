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
 * Ollama local deployment provider implementation
 */
@Slf4j
@Component
public class OllamaProvider implements AIProvider {

    private static final String PROVIDER_NAME = "ollama";

    private final AIProviderConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaProvider(AIProviderConfig config, ObjectMapper objectMapper) {
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
        if (!settings.isEnabled()) {
            return false;
        }
        
        // Check if Ollama server is reachable
        try {
            String url = settings.getBaseUrl() + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Ollama server not reachable: {}", e.getMessage());
            return false;
        }
    }


    @Override
    public int getPriority() {
        return config.getProviderSettings(PROVIDER_NAME).getPriority();
    }

    @Override
    public AIResponse generate(AIRequest request) {
        AIProviderConfig.ProviderSettings settings = config.getProviderSettings(PROVIDER_NAME);
        
        if (!settings.isEnabled()) {
            return AIResponse.failure(PROVIDER_NAME, "Ollama provider is not enabled");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = buildRequestBody(request, settings);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = settings.getBaseUrl() + "/api/chat";
            
            log.debug("Sending request to Ollama: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody(), settings.getModel());

        } catch (Exception e) {
            log.error("Ollama API call failed: {}", e.getMessage(), e);
            return AIResponse.failure(PROVIDER_NAME, "Ollama API call failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(AIRequest request, AIProviderConfig.ProviderSettings settings) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", settings.getModel());
        body.put("stream", false);

        // Options for temperature and other parameters
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) {
            options.put("num_predict", request.getMaxTokens());
        }
        body.put("options", options);

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

    private AIResponse parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            String content = root.path("message").path("content").asText();
            String responseModel = root.path("model").asText(model);
            
            // Ollama provides token counts in different fields
            int promptTokens = root.path("prompt_eval_count").asInt(0);
            int completionTokens = root.path("eval_count").asInt(0);
            
            AIResponse.Usage usage = AIResponse.Usage.builder()
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .build();

            AIResponse response = AIResponse.builder()
                    .content(content)
                    .model(responseModel)
                    .usage(usage)
                    .provider(PROVIDER_NAME)
                    .success(true)
                    .build();

            // Normalize response to ensure consistent format
            return AIResponseNormalizer.normalize(response, PROVIDER_NAME);

        } catch (Exception e) {
            log.error("Failed to parse Ollama response: {}", e.getMessage(), e);
            return AIResponse.failure(PROVIDER_NAME, "Failed to parse response: " + e.getMessage());
        }
    }
}
