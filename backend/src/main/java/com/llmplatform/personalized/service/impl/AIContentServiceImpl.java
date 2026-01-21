package com.llmplatform.personalized.service.impl;

import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.gateway.AIGateway;
import com.llmplatform.personalized.service.AIContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI内容生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIContentServiceImpl implements AIContentService {

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 2048;

    private static final Map<Integer, List<String>> TEMPLATE_EXAMPLE_SENTENCES = Map.of(
            0, List.of(
                    "The word '**' is used when you first learn it.",
                    "Practice makes perfect. The word '**' needs more review.",
                    "Keep going! The word '**' is on your learning list."
            ),
            1, List.of(
                    "You're getting familiar with '**'. Try using it in a sentence.",
                    "The word '**' is becoming easier to remember.",
                    "Keep practicing '**' to strengthen your memory."
            ),
            2, List.of(
                    "You're doing well with '**'. Here's a sentence: I use '**' every day.",
                    "The word '**' is part of your growing vocabulary.",
                    "Great progress! The word '**' is becoming familiar."
            ),
            3, List.of(
                    "Excellent! You've mastered '**'. Example: She has a deep understanding of '**'.",
                    "The word '**' is now a strong part of your vocabulary.",
                    "Amazing progress with '**'!"
            )
    );

    private static final List<Map<String, Object>> TEMPLATE_EXERCISES = List.of(
            createExerciseTemplate("fill_blank", "Fill in the blank with the correct word.", "The teacher asked us to _____ the meaning of the new vocabulary.", "explain"),
            createExerciseTemplate("multiple_choice", "Choose the correct definition.", "What does 'progress' mean?", Map.of("A", "Moving forward", "B", "Standing still", "C", "Going backward", "D", "Staying the same")),
            createExerciseTemplate("matching", "Match the word with its synonym.", "progress", List.of(Map.of("option", "A", "text", "advance"), Map.of("option", "B", "text", "stop")))
    );

    private static final List<Map<String, Object>> TEMPLATE_DIALOGUES = List.of(
            Map.of("title", "Daily Greeting", "scenario", "Casual conversation about the day", "difficulty", "beginner",
                    "dialogue", List.of(
                            Map.of("speaker", "A", "content", "Good morning! How are you doing today?"),
                            Map.of("speaker", "B", "content", "I'm doing great, thank you! And you?"),
                            Map.of("speaker", "A", "content", "I'm doing well too. Let's learn something new today!")
                    )),
            Map.of("title", "Library Scenario", "scenario", "Asking for help in a library", "difficulty", "intermediate",
                    "dialogue", List.of(
                            Map.of("speaker", "Student", "content", "Excuse me, could you help me find a book about history?"),
                            Map.of("speaker", "Librarian", "content", "Of course! What specific period of history are you interested in?"),
                            Map.of("speaker", "Student", "content", "I'm looking for books about ancient civilizations.")
                    ))
    );

    private final AIGateway aiGateway;

    @Override
    public List<String> generateExampleSentences(Long wordId, String word, int masteryLevel, List<String> interests) {
        if (!aiGateway.isAvailable()) {
            log.warn("AI Gateway not available, using template fallback for word: {}", word);
            return getFallbackExampleSentences(word, masteryLevel);
        }

        try {
            String prompt = buildExampleSentencePrompt(word, masteryLevel, interests);
            AIRequest request = AIRequest.builder()
                    .prompt(prompt)
                    .systemMessage("You are a helpful language learning assistant. Generate simple, natural example sentences.")
                    .temperature(DEFAULT_TEMPERATURE)
                    .maxTokens(DEFAULT_MAX_TOKENS)
                    .build();

            AIResponse response = aiGateway.generate(request);

            if (response.isSuccess()) {
                List<String> sentences = parseExampleSentences(response.getContent(), word);
                if (!sentences.isEmpty()) {
                    log.info("Generated {} example sentences for word: {}", sentences.size(), word);
                    return sentences;
                }
            }

            log.warn("AI generation failed for word: {}, using fallback", word);
            return getFallbackExampleSentences(word, masteryLevel);

        } catch (Exception e) {
            log.error("Error generating example sentences for word: {}", word, e);
            return getFallbackExampleSentences(word, masteryLevel);
        }
    }

    @Override
    public List<Map<String, Object>> generateErrorNotebookExercises(List<Long> wordIds) {
        if (!aiGateway.isAvailable()) {
            log.warn("AI Gateway not available, using template fallback for exercises");
            return TEMPLATE_EXERCISES;
        }

        try {
            String prompt = buildErrorExercisePrompt(wordIds.size());
            AIRequest request = AIRequest.builder()
                    .prompt(prompt)
                    .systemMessage("You are a language learning exercise generator. Create varied exercises based on vocabulary words.")
                    .temperature(0.8)
                    .maxTokens(2048)
                    .build();

            AIResponse response = aiGateway.generate(request);

            if (response.isSuccess()) {
                List<Map<String, Object>> exercises = parseExercises(response.getContent());
                if (!exercises.isEmpty()) {
                    log.info("Generated {} exercises for error notebook", exercises.size());
                    return exercises;
                }
            }

            log.warn("AI generation failed for exercises, using fallback");
            return TEMPLATE_EXERCISES;

        } catch (Exception e) {
            log.error("Error generating error notebook exercises", e);
            return TEMPLATE_EXERCISES;
        }
    }

    @Override
    public Map<String, Object> generateDialogueScenario(String goalType, int masteryLevel) {
        if (!aiGateway.isAvailable()) {
            log.warn("AI Gateway not available, using template fallback for dialogue");
            return getFallbackDialogue(goalType, masteryLevel);
        }

        try {
            String prompt = buildDialoguePrompt(goalType, masteryLevel);
            AIRequest request = AIRequest.builder()
                    .prompt(prompt)
                    .systemMessage("You are a language learning conversation partner. Create natural, contextual dialogues for learning.")
                    .temperature(0.8)
                    .maxTokens(2048)
                    .build();

            AIResponse response = aiGateway.generate(request);

            if (response.isSuccess()) {
                Map<String, Object> dialogue = parseDialogue(response.getContent(), goalType);
                if (!dialogue.isEmpty()) {
                    log.info("Generated dialogue scenario for goal: {}", goalType);
                    return dialogue;
                }
            }

            log.warn("AI generation failed for dialogue, using fallback");
            return getFallbackDialogue(goalType, masteryLevel);

        } catch (Exception e) {
            log.error("Error generating dialogue scenario for goal: {}", goalType, e);
            return getFallbackDialogue(goalType, masteryLevel);
        }
    }

    @Override
    public List<Map<String, Object>> generateMultipleChoiceExercises(Long wordId, String word, int masteryLevel, int count) {
        if (!aiGateway.isAvailable()) {
            log.warn("AI Gateway not available, using template fallback for multiple choice");
            return createTemplateMultipleChoice(word, count);
        }

        try {
            String prompt = buildMultipleChoicePrompt(word, masteryLevel, count);
            AIRequest request = AIRequest.builder()
                    .prompt(prompt)
                    .systemMessage("You are a language learning quiz generator. Create multiple choice questions for vocabulary practice.")
                    .temperature(0.8)
                    .maxTokens(2048)
                    .build();

            AIResponse response = aiGateway.generate(request);

            if (response.isSuccess()) {
                List<Map<String, Object>> exercises = parseMultipleChoice(response.getContent(), word);
                if (!exercises.isEmpty()) {
                    log.info("Generated {} multiple choice exercises for word: {}", exercises.size(), word);
                    return exercises;
                }
            }

            log.warn("AI generation failed for multiple choice, using fallback");
            return createTemplateMultipleChoice(word, count);

        } catch (Exception e) {
            log.error("Error generating multiple choice exercises for word: {}", word, e);
            return createTemplateMultipleChoice(word, count);
        }
    }

    private String buildExampleSentencePrompt(String word, int masteryLevel, List<String> interests) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate 3 natural example sentences using the word '").append(word).append("'.\n");
        prompt.append("Current mastery level: ").append(masteryLevel).append(" (0=new, 5=mastered).\n");

        if (interests != null && !interests.isEmpty()) {
            prompt.append("User interests: ").append(String.join(", ", interests)).append(".\n");
        }

        prompt.append("Difficulty: ");
        if (masteryLevel <= 1) {
            prompt.append("simple sentences for beginners");
        } else if (masteryLevel <= 3) {
            prompt.append("moderate sentences with context");
        } else {
            prompt.append("complex sentences for advanced learners");
        }
        prompt.append(".\n");

        prompt.append("Format each sentence on a new line, without numbering.");

        return prompt.toString();
    }

    private String buildErrorExercisePrompt(int wordCount) {
        return String.format(
                "Generate %d varied vocabulary exercises to help reinforce learning. " +
                        "Include different types: fill-in-the-blank, multiple choice, and matching. " +
                        "Return the result as a JSON array with exercise objects containing: type, question, and correctAnswer.",
                wordCount > 0 ? Math.min(wordCount, 5) : 3
        );
    }

    private String buildDialoguePrompt(String goalType, int masteryLevel) {
        return String.format(
                "Create a short dialogue scenario for practicing language learning with the goal: '%s'. " +
                        "Difficulty level: %d. " +
                        "Return a JSON object with: title, scenario, difficulty, and dialogue (array of speaker/content pairs).",
                goalType, masteryLevel
        );
    }

    private String buildMultipleChoicePrompt(String word, int masteryLevel, int count) {
        return String.format(
                "Generate %d multiple choice questions for the word '%s'. " +
                        "Each question should test understanding or usage. " +
                        "Return as JSON array with: question, options (array), and correctAnswer.",
                count > 0 ? Math.min(count, 5) : 3, word
        );
    }

    private List<String> parseExampleSentences(String content, String word) {
        List<String> sentences = new ArrayList<>();
        String[] lines = content.split("\n");
        Pattern pattern = Pattern.compile("[^.!?]*" + Pattern.quote(word) + "[^.!?]*[.!?]?");

        for (String line : lines) {
            line = line.replaceAll("^\\d+\\.\\s*", "").trim();
            if (!line.isEmpty() && line.toLowerCase().contains(word.toLowerCase())) {
                sentences.add(line);
            }
        }

        if (sentences.isEmpty() && content.toLowerCase().contains(word.toLowerCase())) {
            sentences.addAll(Arrays.asList(content.split("\n")));
            sentences.removeIf(String::isEmpty);
        }

        return sentences;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseExercises(String content) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        try {
            String jsonContent = extractJsonArray(content);
            if (jsonContent != null) {
                List<Map<String, Object>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonContent, List.class);
                for (Object item : parsed) {
                    if (item instanceof Map) {
                        exercises.add((Map<String, Object>) item);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse exercises JSON, using raw content", e);
        }

        return exercises;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDialogue(String content, String goalType) {
        Map<String, Object> dialogue = new HashMap<>();

        try {
            String jsonContent = extractJsonObject(content);
            if (jsonContent != null) {
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonContent, Map.class);
                dialogue.putAll(parsed);
                dialogue.put("goalType", goalType);
            }
        } catch (Exception e) {
            log.debug("Failed to parse dialogue JSON", e);
        }

        return dialogue;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseMultipleChoice(String content, String word) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        try {
            String jsonContent = extractJsonArray(content);
            if (jsonContent != null) {
                List<Map<String, Object>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonContent, List.class);
                exercises.addAll(parsed);
            }
        } catch (Exception e) {
            log.debug("Failed to parse multiple choice JSON", e);
        }

        return exercises;
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }

    private List<String> getFallbackExampleSentences(String word, int masteryLevel) {
        int level = Math.min(Math.max(masteryLevel, 0), 3);
        List<String> templates = TEMPLATE_EXAMPLE_SENTENCES.get(level);

        List<String> sentences = new ArrayList<>();
        for (String template : templates) {
            sentences.add(template.replace("**", word));
        }

        log.info("Using fallback example sentences for word: {}", word);
        return sentences;
    }

    private Map<String, Object> getFallbackDialogue(String goalType, int masteryLevel) {
        Map<String, Object> fallback = new HashMap<>();

        for (Map<String, Object> template : TEMPLATE_DIALOGUES) {
            if (template.get("title").toString().toLowerCase().contains(goalType.toLowerCase())) {
                fallback.putAll(template);
                fallback.put("goalType", goalType);
                fallback.put("generated", false);
                return fallback;
            }
        }

        fallback.put("title", goalType + " Practice");
        fallback.put("scenario", "Practice conversation for " + goalType);
        fallback.put("difficulty", masteryLevel <= 2 ? "beginner" : "intermediate");
        fallback.put("dialogue", TEMPLATE_DIALOGUES.get(0).get("dialogue"));
        fallback.put("goalType", goalType);
        fallback.put("generated", false);

        log.info("Using fallback dialogue for goal: {}", goalType);
        return fallback;
    }

    private List<Map<String, Object>> createTemplateMultipleChoice(String word, int count) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        for (int i = 0; i < Math.min(count, 3); i++) {
            Map<String, Object> exercise = new HashMap<>();
            exercise.put("type", "multiple_choice");
            exercise.put("question", "What is the meaning of '" + word + "'?");
            exercise.put("options", Arrays.asList(
                    Map.of("label", "A", "text", "Meaning A for " + word),
                    Map.of("label", "B", "text", "Meaning B for " + word),
                    Map.of("label", "C", "text", "Meaning C for " + word),
                    Map.of("label", "D", "text", "Meaning D for " + word)
            ));
            exercise.put("correctAnswer", "A");
            exercises.add(exercise);
        }

        log.info("Using template multiple choice exercises for word: {}", word);
        return exercises;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> createExerciseTemplate(String type, String instruction, String template, Object correctAnswer) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("type", type);
        exercise.put("instruction", instruction);
        exercise.put("question", template);
        exercise.put("correctAnswer", correctAnswer);
        return exercise;
    }
}
