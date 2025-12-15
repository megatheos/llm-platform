package com.llmplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.gateway.AIGateway;
import com.llmplatform.common.ActivityType;
import com.llmplatform.common.CacheConstants;
import com.llmplatform.dto.CreateScenarioDTO;
import com.llmplatform.entity.DialogueSession;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Scenario;
import com.llmplatform.exception.BusinessException;
import com.llmplatform.mapper.DialogueSessionMapper;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.ScenarioMapper;
import com.llmplatform.service.DialogueService;
import com.llmplatform.util.CacheUtil;
import com.llmplatform.vo.AIResponseVO;
import com.llmplatform.vo.DialogueSessionVO;
import com.llmplatform.vo.ScenarioVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialogue service implementation with session management and AI integration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogueServiceImpl implements DialogueService {

    private final ScenarioMapper scenarioMapper;
    private final DialogueSessionMapper dialogueSessionMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final AIGateway aiGateway;
    private final CacheUtil cacheUtil;
    private final ObjectMapper objectMapper;


    @Override
    public List<ScenarioVO> getScenarios(Long userId) {
        // Get all preset scenarios and user's custom scenarios
        List<Scenario> scenarios = scenarioMapper.selectList(
            new LambdaQueryWrapper<Scenario>()
                .eq(Scenario::getIsPreset, true)
                .or()
                .eq(Scenario::getCreatedBy, userId)
                .orderByAsc(Scenario::getCategory)
                .orderByAsc(Scenario::getName)
        );

        return scenarios.stream()
            .map(this::convertToScenarioVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScenarioVO createScenario(CreateScenarioDTO dto, Long userId) {
        Scenario scenario = new Scenario();
        scenario.setName(dto.getName().trim());
        scenario.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        scenario.setCategory(dto.getCategory().trim());
        scenario.setIsPreset(false);
        scenario.setCreatedBy(userId);
        scenario.setCreatedAt(LocalDateTime.now());

        scenarioMapper.insert(scenario);
        log.info("Created custom scenario: {} for user: {}", scenario.getName(), userId);

        return convertToScenarioVO(scenario);
    }

    @Override
    public ScenarioVO getScenarioById(Long scenarioId) {
        Scenario scenario = scenarioMapper.selectById(scenarioId);
        return scenario != null ? convertToScenarioVO(scenario) : null;
    }

    @Override
    @Transactional
    public DialogueSessionVO startSession(Long scenarioId, String targetLang, Long userId) {
        // Verify scenario exists
        Scenario scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new BusinessException("SCENARIO_NOT_FOUND", "Scenario not found");
        }

        String lang = targetLang != null ? targetLang : "en";

        // Create new session
        DialogueSession session = new DialogueSession();
        session.setUserId(userId);
        session.setScenarioId(scenarioId);
        session.setTargetLang(lang);
        session.setMessages("[]");
        session.setStartedAt(LocalDateTime.now());

        dialogueSessionMapper.insert(session);

        // Initialize context in Redis
        List<DialogueSessionVO.MessageVO> emptyMessages = new ArrayList<>();
        cacheDialogueContext(session.getId(), emptyMessages);

        log.info("Started dialogue session: {} for user: {} with scenario: {} in language: {}", 
                session.getId(), userId, scenarioId, lang);

        DialogueSessionVO vo = convertToSessionVO(session, scenario.getName());
        vo.setTargetLang(lang);
        return vo;
    }


    @Override
    @Transactional
    public AIResponseVO sendMessage(Long sessionId, String message, Long userId) {
        // Get session and validate ownership
        DialogueSession session = dialogueSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "Dialogue session not found");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "You don't have access to this session");
        }
        if (session.getEndedAt() != null) {
            throw new BusinessException("SESSION_ENDED", "This dialogue session has ended");
        }

        // Get scenario for context
        Scenario scenario = scenarioMapper.selectById(session.getScenarioId());
        if (scenario == null) {
            throw new BusinessException("SCENARIO_NOT_FOUND", "Scenario not found");
        }

        // Get conversation context from cache or database
        List<DialogueSessionVO.MessageVO> messages = getDialogueContext(session);

        // Add user message to context
        DialogueSessionVO.MessageVO userMessage = new DialogueSessionVO.MessageVO();
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setTimestamp(LocalDateTime.now());
        messages.add(userMessage);

        // Build AI request with context
        AIResponse aiResponse = generateAIResponse(scenario, messages, session.getTargetLang());

        if (!aiResponse.isSuccess()) {
            throw new BusinessException("AI_ERROR", "Failed to generate response: " + aiResponse.getErrorMessage());
        }

        // Add AI response to context
        DialogueSessionVO.MessageVO assistantMessage = new DialogueSessionVO.MessageVO();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(aiResponse.getContent());
        assistantMessage.setTimestamp(LocalDateTime.now());
        messages.add(assistantMessage);

        // Update cache and database
        cacheDialogueContext(sessionId, messages);
        updateSessionMessages(session, messages);

        // Build response
        AIResponseVO responseVO = new AIResponseVO();
        responseVO.setContent(aiResponse.getContent());
        responseVO.setRole("assistant");
        responseVO.setSessionId(sessionId);

        return responseVO;
    }

    @Override
    @Transactional
    public void endSession(Long sessionId, Long userId) {
        DialogueSession session = dialogueSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "Dialogue session not found");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "You don't have access to this session");
        }
        if (session.getEndedAt() != null) {
            return; // Already ended
        }

        // Try to get messages from cache first, fall back to what's already in DB
        List<DialogueSessionVO.MessageVO> cachedMessages = getCachedDialogueContext(sessionId);
        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            // Update session with cached messages (more recent than DB)
            updateSessionMessages(session, cachedMessages);
        }
        // If no cached messages, keep whatever is already in the database
        
        // Update session with end time
        session.setEndedAt(LocalDateTime.now());
        dialogueSessionMapper.updateById(session);

        // Clear cache
        String cacheKey = CacheConstants.dialogueKey(sessionId);
        cacheUtil.delete(cacheKey);

        // Record learning activity
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.DIALOGUE.name());
        record.setActivityId(sessionId);
        record.setActivityTime(LocalDateTime.now());
        learningRecordMapper.insert(record);

        log.info("Ended dialogue session: {} for user: {}", sessionId, userId);
    }


    @Override
    public DialogueSessionVO getSession(Long sessionId, Long userId) {
        DialogueSession session = dialogueSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return null;
        }

        Scenario scenario = scenarioMapper.selectById(session.getScenarioId());
        String scenarioName = scenario != null ? scenario.getName() : "Unknown";

        DialogueSessionVO vo = convertToSessionVO(session, scenarioName);
        
        // Get messages from cache or database
        List<DialogueSessionVO.MessageVO> messages = getDialogueContext(session);
        vo.setMessages(messages);

        return vo;
    }

    @Override
    public List<DialogueSessionVO> getUserSessions(Long userId) {
        List<DialogueSession> sessions = dialogueSessionMapper.selectList(
            new LambdaQueryWrapper<DialogueSession>()
                .eq(DialogueSession::getUserId, userId)
                .orderByDesc(DialogueSession::getStartedAt)
        );

        return sessions.stream()
            .map(session -> {
                Scenario scenario = scenarioMapper.selectById(session.getScenarioId());
                String scenarioName = scenario != null ? scenario.getName() : "Unknown";
                DialogueSessionVO vo = convertToSessionVO(session, scenarioName);
                vo.setMessages(parseMessages(session.getMessages()));
                return vo;
            })
            .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private AIResponse generateAIResponse(Scenario scenario, List<DialogueSessionVO.MessageVO> messages, String targetLang) {
        String lang = targetLang != null ? targetLang : "en";
        String systemMessage = String.format(
            "You are a language learning assistant helping a user practice %s conversations in a '%s' scenario. " +
            "Scenario description: %s. " +
            "Respond in %s language naturally and helpfully, keeping the conversation relevant to the scenario. " +
            "Provide corrections or suggestions when appropriate to help the user improve their %s language skills.",
            lang, scenario.getName(),
            scenario.getDescription() != null ? scenario.getDescription() : scenario.getName(),
            lang, lang
        );

        // Convert messages to AI request format
        List<AIRequest.Message> contextMessages = messages.stream()
            .map(m -> AIRequest.Message.builder()
                .role(m.getRole())
                .content(m.getContent())
                .build())
            .collect(Collectors.toList());

        AIRequest request = AIRequest.builder()
            .systemMessage(systemMessage)
            .messages(contextMessages)
            .temperature(0.7)
            .maxTokens(1024)
            .build();

        return aiGateway.generateWithContext(request, contextMessages);
    }

    private List<DialogueSessionVO.MessageVO> getDialogueContext(DialogueSession session) {
        // Try cache first
        List<DialogueSessionVO.MessageVO> cached = getCachedDialogueContext(session.getId());
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Fall back to database
        return parseMessages(session.getMessages());
    }

    /**
     * Get dialogue context from cache only (no DB fallback)
     * Returns null if cache is unavailable or empty
     */
    private List<DialogueSessionVO.MessageVO> getCachedDialogueContext(Long sessionId) {
        String cacheKey = CacheConstants.dialogueKey(sessionId);
        try {
            Object cached = cacheUtil.get(cacheKey);
            if (cached != null) {
                if (cached instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<DialogueSessionVO.MessageVO> messages = (List<DialogueSessionVO.MessageVO>) cached;
                    return new ArrayList<>(messages);
                }
                // Try to parse as JSON string
                String jsonStr = objectMapper.writeValueAsString(cached);
                return objectMapper.readValue(jsonStr, 
                    new TypeReference<List<DialogueSessionVO.MessageVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to get dialogue context from cache: {}", e.getMessage());
        }
        return null;
    }

    private void cacheDialogueContext(Long sessionId, List<DialogueSessionVO.MessageVO> messages) {
        String cacheKey = CacheConstants.dialogueKey(sessionId);
        try {
            cacheUtil.set(cacheKey, messages, CacheConstants.DIALOGUE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache dialogue context: {}", e.getMessage());
        }
    }


    private void updateSessionMessages(DialogueSession session, List<DialogueSessionVO.MessageVO> messages) {
        try {
            session.setMessages(objectMapper.writeValueAsString(messages));
            dialogueSessionMapper.updateById(session);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages: {}", e.getMessage());
            throw new BusinessException("SERIALIZATION_ERROR", "Failed to save messages");
        }
    }

    private List<DialogueSessionVO.MessageVO> parseMessages(String messagesJson) {
        if (messagesJson == null || messagesJson.isEmpty() || messagesJson.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(messagesJson, 
                new TypeReference<List<DialogueSessionVO.MessageVO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse messages JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ScenarioVO convertToScenarioVO(Scenario scenario) {
        ScenarioVO vo = new ScenarioVO();
        vo.setId(scenario.getId());
        vo.setName(scenario.getName());
        vo.setDescription(scenario.getDescription());
        vo.setCategory(scenario.getCategory());
        vo.setIsPreset(scenario.getIsPreset());
        vo.setCreatedBy(scenario.getCreatedBy());
        vo.setCreatedAt(scenario.getCreatedAt());
        return vo;
    }

    private DialogueSessionVO convertToSessionVO(DialogueSession session, String scenarioName) {
        DialogueSessionVO vo = new DialogueSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setScenarioId(session.getScenarioId());
        vo.setScenarioName(scenarioName);
        vo.setMessages(parseMessages(session.getMessages()));
        vo.setStartedAt(session.getStartedAt());
        vo.setEndedAt(session.getEndedAt());
        return vo;
    }
}
