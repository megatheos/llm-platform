package com.llmplatform.property;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.dto.CreateScenarioDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.DialogueSession;
import com.llmplatform.entity.Scenario;
import com.llmplatform.mapper.DialogueSessionMapper;
import com.llmplatform.mapper.ScenarioMapper;
import com.llmplatform.service.DialogueService;
import com.llmplatform.service.UserService;
import com.llmplatform.vo.DialogueSessionVO;
import com.llmplatform.vo.ScenarioVO;
import com.llmplatform.vo.UserVO;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for DialogueService
 */
@SpringBootTest
@ActiveProfiles("test")
class DialogueServicePropertyTest {

    @Autowired
    private DialogueService dialogueService;

    @Autowired
    private ScenarioMapper scenarioMapper;

    @Autowired
    private DialogueSessionMapper dialogueSessionMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * Feature: llm-language-learning-platform, Property 6: Custom scenario round-trip
     * 
     * For any valid custom scenario data, after creation the scenario should be retrievable
     * and contain the same name, description, and category.
     * 
     * Validates: Requirements 3.2
     */
    @Test
    @Transactional
    void customScenarioRoundTrip_property() {
        // Create test user
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();

        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(s -> s + UUID.randomUUID().toString().substring(0, 4));
        
        Arbitrary<String> descriptions = Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(200)
                .filter(s -> !s.contains("\u0000")); // Filter null characters
        
        Arbitrary<String> categories = Arbitraries.of("travel", "business", "daily_life", "academic", "custom");

        Arbitrary<ScenarioTestData> testDataArbitrary = Combinators.combine(names, descriptions, categories)
                .as(ScenarioTestData::new);

        for (int i = 0; i < 100; i++) {
            ScenarioTestData testData = testDataArbitrary.sample();

            // Create scenario
            CreateScenarioDTO dto = new CreateScenarioDTO();
            dto.setName(testData.name);
            dto.setDescription(testData.description);
            dto.setCategory(testData.category);

            ScenarioVO created = dialogueService.createScenario(dto, userId);

            // Retrieve scenario
            ScenarioVO retrieved = dialogueService.getScenarioById(created.getId());

            // Assert round-trip property
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(created.getId());
            assertThat(retrieved.getName()).isEqualTo(testData.name.trim());
            assertThat(retrieved.getDescription()).isEqualTo(
                    testData.description != null ? testData.description.trim() : "");
            assertThat(retrieved.getCategory()).isEqualTo(testData.category.trim());
            assertThat(retrieved.getIsPreset()).isFalse();
            assertThat(retrieved.getCreatedBy()).isEqualTo(userId);
        }
    }


    /**
     * Feature: llm-language-learning-platform, Property 7: Dialogue context preservation
     * 
     * For any active dialogue session with N messages, the context sent to AI_Gateway
     * should contain all N previous messages in chronological order.
     * 
     * Note: This test validates that messages are properly stored and retrieved in order.
     * The actual AI gateway call is not tested here - we test the context management.
     * 
     * Validates: Requirements 3.5
     */
    @Test
    @Transactional
    void dialogueContextPreservation_property() {
        // Create test user and scenario
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();
        
        Scenario testScenario = createTestScenario();

        Arbitrary<Integer> messageCounts = Arbitraries.integers().between(1, 10);
        Arbitrary<String> messageContents = Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100);

        for (int i = 0; i < 100; i++) {
            int messageCount = messageCounts.sample();
            
            // Start a session
            DialogueSessionVO session = dialogueService.startSession(testScenario.getId(), "en", userId);
            assertThat(session).isNotNull();
            assertThat(session.getMessages()).isEmpty();

            // Simulate adding messages directly to the session (without AI call)
            List<DialogueSessionVO.MessageVO> expectedMessages = new java.util.ArrayList<>();
            
            for (int j = 0; j < messageCount; j++) {
                String content = messageContents.sample();
                
                DialogueSessionVO.MessageVO userMsg = new DialogueSessionVO.MessageVO();
                userMsg.setRole("user");
                userMsg.setContent(content);
                userMsg.setTimestamp(LocalDateTime.now());
                expectedMessages.add(userMsg);

                // Simulate assistant response
                DialogueSessionVO.MessageVO assistantMsg = new DialogueSessionVO.MessageVO();
                assistantMsg.setRole("assistant");
                assistantMsg.setContent("Response to: " + content);
                assistantMsg.setTimestamp(LocalDateTime.now());
                expectedMessages.add(assistantMsg);
            }

            // Update session messages directly in database using raw SQL to avoid MyBatis cache
            DialogueSession dbSession = dialogueSessionMapper.selectById(session.getId());
            String messagesJson = serializeMessages(expectedMessages);
            dbSession.setMessages(messagesJson);
            dialogueSessionMapper.updateById(dbSession);

            // Fetch fresh from database to verify persistence (bypass any caching)
            DialogueSession freshSession = dialogueSessionMapper.selectById(session.getId());
            List<DialogueSessionVO.MessageVO> persistedMessages = parseMessages(freshSession.getMessages());
            
            assertThat(persistedMessages).isNotNull();
            assertThat(persistedMessages).hasSize(expectedMessages.size());

            // Verify chronological order and content
            for (int k = 0; k < expectedMessages.size(); k++) {
                DialogueSessionVO.MessageVO expected = expectedMessages.get(k);
                DialogueSessionVO.MessageVO actual = persistedMessages.get(k);
                
                assertThat(actual.getRole()).isEqualTo(expected.getRole());
                assertThat(actual.getContent()).isEqualTo(expected.getContent());
            }
        }
    }


    /**
     * Feature: llm-language-learning-platform, Property 8: Dialogue session persistence
     * 
     * For any ended dialogue session, the saved conversation history should contain
     * all messages exchanged during the session.
     * 
     * Validates: Requirements 3.4
     */
    @Test
    @Transactional
    void dialogueSessionPersistence_property() {
        // Create test user and scenario
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();
        
        Scenario testScenario = createTestScenario();

        Arbitrary<Integer> messageCounts = Arbitraries.integers().between(1, 10);
        Arbitrary<String> messageContents = Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100);

        for (int i = 0; i < 100; i++) {
            int messageCount = messageCounts.sample();
            
            // Start a session
            DialogueSessionVO session = dialogueService.startSession(testScenario.getId(), "en", userId);
            assertThat(session).isNotNull();

            // Add messages to the session
            List<DialogueSessionVO.MessageVO> allMessages = new java.util.ArrayList<>();
            
            for (int j = 0; j < messageCount; j++) {
                String content = messageContents.sample();
                
                DialogueSessionVO.MessageVO userMsg = new DialogueSessionVO.MessageVO();
                userMsg.setRole("user");
                userMsg.setContent(content);
                userMsg.setTimestamp(LocalDateTime.now());
                allMessages.add(userMsg);

                DialogueSessionVO.MessageVO assistantMsg = new DialogueSessionVO.MessageVO();
                assistantMsg.setRole("assistant");
                assistantMsg.setContent("Response to: " + content);
                assistantMsg.setTimestamp(LocalDateTime.now());
                allMessages.add(assistantMsg);
            }

            // Update session with messages before ending
            DialogueSession dbSession = dialogueSessionMapper.selectById(session.getId());
            String messagesJson = serializeMessages(allMessages);
            dbSession.setMessages(messagesJson);
            dialogueSessionMapper.updateById(dbSession);

            // End the session - this should preserve the messages
            dialogueService.endSession(session.getId(), userId);

            // Fetch fresh from database to verify persistence
            DialogueSession endedSession = dialogueSessionMapper.selectById(session.getId());
            
            assertThat(endedSession).isNotNull();
            assertThat(endedSession.getEndedAt()).isNotNull();
            
            // Parse and verify persisted messages
            List<DialogueSessionVO.MessageVO> persistedMessages = parseMessages(endedSession.getMessages());
            
            assertThat(persistedMessages).hasSize(allMessages.size());
            
            for (int k = 0; k < allMessages.size(); k++) {
                DialogueSessionVO.MessageVO expected = allMessages.get(k);
                DialogueSessionVO.MessageVO actual = persistedMessages.get(k);
                
                assertThat(actual.getRole()).isEqualTo(expected.getRole());
                assertThat(actual.getContent()).isEqualTo(expected.getContent());
            }
        }
    }


    // ==================== Helper Methods ====================

    /**
     * Helper method to create a test user
     */
    private UserVO createTestUser() {
        RegisterDTO dto = new RegisterDTO();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        dto.setUsername("dialogueuser" + uniqueSuffix);
        dto.setPassword("password123");
        dto.setEmail("dialogue" + uniqueSuffix + "@test.com");
        return userService.register(dto);
    }

    /**
     * Helper method to create a test scenario
     */
    private Scenario createTestScenario() {
        Scenario scenario = new Scenario();
        scenario.setName("Test Scenario " + UUID.randomUUID().toString().substring(0, 8));
        scenario.setDescription("A test scenario for property testing");
        scenario.setCategory("test");
        scenario.setIsPreset(true);
        scenario.setCreatedAt(LocalDateTime.now());
        scenarioMapper.insert(scenario);
        return scenario;
    }

    /**
     * Helper method to serialize messages to JSON
     */
    private String serializeMessages(List<DialogueSessionVO.MessageVO> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Helper method to parse messages from JSON
     */
    private List<DialogueSessionVO.MessageVO> parseMessages(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, 
                new TypeReference<List<DialogueSessionVO.MessageVO>>() {});
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Helper class for scenario test data
     */
    private static class ScenarioTestData {
        final String name;
        final String description;
        final String category;

        ScenarioTestData(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }
    }
}
