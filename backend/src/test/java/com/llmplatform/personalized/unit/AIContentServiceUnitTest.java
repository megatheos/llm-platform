package com.llmplatform.personalized.unit;

import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.gateway.AIGateway;
import com.llmplatform.personalized.service.AIContentService;
import com.llmplatform.personalized.service.impl.AIContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI内容生成服务单元测试")
class AIContentServiceUnitTest {

    @Mock
    private AIGateway aiGateway;

    private AIContentService aiContentService;

    @BeforeEach
    void setUp() {
        aiContentService = new AIContentServiceImpl(aiGateway);
    }

    @Test
    @DisplayName("测试生成例句 - AI可用时正常调用")
    void generateExampleSentences_WhenAIAvailable_ReturnsGeneratedSentences() {
        // Arrange
        String word = "example";
        String aiResponse = "The teacher gave us an example to follow.\n" +
                "Please provide another example.\n" +
                "This is a simple example sentence.";

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.builder()
                        .content(aiResponse)
                        .success(true)
                        .provider("deepseek")
                        .build()
        );

        // Act
        List<String> sentences = aiContentService.generateExampleSentences(1L, word, 2, List.of("technology"));

        // Assert
        assertNotNull(sentences);
        assertFalse(sentences.isEmpty());
        assertTrue(sentences.stream().anyMatch(s -> s.toLowerCase().contains(word.toLowerCase())));

        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成例句 - AI不可用时使用模板")
    void generateExampleSentences_WhenAIAvailableFalse_ReturnsFallbackSentences() {
        // Arrange
        String word = "learning";
        when(aiGateway.isAvailable()).thenReturn(false);

        // Act
        List<String> sentences = aiContentService.generateExampleSentences(1L, word, 1, null);

        // Assert
        assertNotNull(sentences);
        assertEquals(3, sentences.size());
        assertTrue(sentences.stream().allMatch(s -> s.contains(word)));

        verify(aiGateway).isAvailable();
        verify(aiGateway, never()).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成例句 - 不同掌握度使用不同模板")
    void generateExampleSentences_DifferentMasteryLevels_UsesAppropriateTemplates() {
        // Arrange
        String word = "vocabulary";
        when(aiGateway.isAvailable()).thenReturn(false);

        // Test level 0 (new learner)
        List<String> level0Sentences = aiContentService.generateExampleSentences(1L, word, 0, null);
        assertTrue(level0Sentences.stream().allMatch(s -> s.contains(word)));
        assertEquals(3, level0Sentences.size());

        // Test level 3 (intermediate)
        List<String> level3Sentences = aiContentService.generateExampleSentences(2L, word, 3, null);
        assertTrue(level3Sentences.stream().allMatch(s -> s.contains(word)));
        assertEquals(3, level3Sentences.size());

        verify(aiGateway, times(2)).isAvailable();
    }

    @Test
    @DisplayName("测试生成错题本练习 - AI可用时")
    void generateErrorNotebookExercises_WhenAIAvailable_ReturnsGeneratedExercises() {
        // Arrange
        String aiResponse = "[{\"type\": \"fill_blank\", \"question\": \"Complete the sentence\", \"correctAnswer\": \"answer\"}]";

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.builder()
                        .content(aiResponse)
                        .success(true)
                        .provider("deepseek")
                        .build()
        );

        // Act
        List<Map<String, Object>> exercises = aiContentService.generateErrorNotebookExercises(List.of(1L, 2L, 3L));

        // Assert
        assertNotNull(exercises);
        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成错题本练习 - AI不可用时使用模板")
    void generateErrorNotebookExercises_WhenAIAvailableFalse_ReturnsTemplateExercises() {
        // Arrange
        when(aiGateway.isAvailable()).thenReturn(false);

        // Act
        List<Map<String, Object>> exercises = aiContentService.generateErrorNotebookExercises(List.of(1L, 2L));

        // Assert
        assertNotNull(exercises);
        assertFalse(exercises.isEmpty());
        assertTrue(exercises.stream().anyMatch(e -> e.containsKey("type")));

        verify(aiGateway).isAvailable();
        verify(aiGateway, never()).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成对话场景 - AI可用时")
    void generateDialogueScenario_WhenAIAvailable_ReturnsGeneratedDialogue() {
        // Arrange
        String goalType = "daily";
        String aiResponse = "{\"title\": \"Morning Chat\", \"scenario\": \"Casual conversation\", \"dialogue\": []}";

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.builder()
                        .content(aiResponse)
                        .success(true)
                        .provider("deepseek")
                        .build()
        );

        // Act
        Map<String, Object> dialogue = aiContentService.generateDialogueScenario(goalType, 2);

        // Assert
        assertNotNull(dialogue);
        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成对话场景 - AI不可用时使用模板")
    void generateDialogueScenario_WhenAIAvailableFalse_ReturnsTemplateDialogue() {
        // Arrange
        when(aiGateway.isAvailable()).thenReturn(false);

        // Act
        Map<String, Object> dialogue = aiContentService.generateDialogueScenario("greeting", 1);

        // Assert
        assertNotNull(dialogue);
        assertTrue(dialogue.containsKey("title"));
        assertTrue(dialogue.containsKey("dialogue"));
        assertFalse(((List<?>) dialogue.get("dialogue")).isEmpty());

        verify(aiGateway).isAvailable();
        verify(aiGateway, never()).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成多选题 - AI可用时")
    void generateMultipleChoiceExercises_WhenAIAvailable_ReturnsGeneratedExercises() {
        // Arrange
        String word = "program";
        String aiResponse = "[{\"question\": \"What does program mean?\", \"options\": [], \"correctAnswer\": \"A\"}]";

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.builder()
                        .content(aiResponse)
                        .success(true)
                        .provider("deepseek")
                        .build()
        );

        // Act
        List<Map<String, Object>> exercises = aiContentService.generateMultipleChoiceExercises(1L, word, 2, 3);

        // Assert
        assertNotNull(exercises);
        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成多选题 - AI不可用时使用模板")
    void generateMultipleChoiceExercises_WhenAIAvailableFalse_ReturnsTemplateExercises() {
        // Arrange
        String word = "algorithm";
        when(aiGateway.isAvailable()).thenReturn(false);

        // Act
        List<Map<String, Object>> exercises = aiContentService.generateMultipleChoiceExercises(1L, word, 2, 3);

        // Assert
        assertNotNull(exercises);
        assertEquals(3, exercises.size());
        assertTrue(exercises.stream().allMatch(e ->
                e.containsKey("type") &&
                        e.containsKey("question") &&
                        e.containsKey("options") &&
                        e.containsKey("correctAnswer")));

        verify(aiGateway).isAvailable();
        verify(aiGateway, never()).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试AI请求参数正确传递")
    void generateExampleSentences_VerifyAIRequestParameters() {
        // Arrange
        String word = "test";
        List<String> interests = List.of("sports", "music");

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.builder()
                        .content("Test sentence one.\nTest sentence two.\nTest sentence three.")
                        .success(true)
                        .provider("deepseek")
                        .build()
        );

        ArgumentCaptor<AIRequest> requestCaptor = ArgumentCaptor.forClass(AIRequest.class);

        // Act
        aiContentService.generateExampleSentences(1L, word, 3, interests);

        // Assert
        verify(aiGateway).generate(requestCaptor.capture());
        AIRequest capturedRequest = requestCaptor.getValue();

        assertNotNull(capturedRequest.getPrompt());
        assertTrue(capturedRequest.getPrompt().contains(word));
        assertNotNull(capturedRequest.getSystemMessage());
        assertNotNull(capturedRequest.getTemperature());
    }

    @Test
    @DisplayName("测试AI生成失败时回退到模板")
    void generateExampleSentences_WhenAIFails_ReturnsFallback() {
        // Arrange
        String word = "memory";

        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenReturn(
                AIResponse.failure("deepseek", "Connection timeout")
        );

        // Act
        List<String> sentences = aiContentService.generateExampleSentences(1L, word, 2, null);

        // Assert
        assertNotNull(sentences);
        assertEquals(3, sentences.size());
        assertTrue(sentences.stream().allMatch(s -> s.contains(word)));

        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试AI异常时回退到模板")
    void generateDialogueScenario_WhenAIThrowsException_ReturnsFallback() {
        // Arrange
        when(aiGateway.isAvailable()).thenReturn(true);
        when(aiGateway.generate(any(AIRequest.class))).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        Map<String, Object> dialogue = aiContentService.generateDialogueScenario("travel", 2);

        // Assert
        assertNotNull(dialogue);
        assertTrue(dialogue.containsKey("title"));
        assertTrue(dialogue.containsKey("dialogue"));

        verify(aiGateway).isAvailable();
        verify(aiGateway).generate(any(AIRequest.class));
    }

    @Test
    @DisplayName("测试生成多选题数量限制")
    void generateMultipleChoiceExercises_RespectsCountLimit() {
        // Arrange
        String word = "function";
        when(aiGateway.isAvailable()).thenReturn(false);

        // Act - request 10, but template only provides 3
        List<Map<String, Object>> exercises = aiContentService.generateMultipleChoiceExercises(1L, word, 2, 10);

        // Assert
        assertNotNull(exercises);
        assertEquals(3, exercises.size()); // Template provides 3 exercises
    }
}
