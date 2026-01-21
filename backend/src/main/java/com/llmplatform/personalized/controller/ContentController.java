package com.llmplatform.personalized.controller;

import com.llmplatform.common.Result;
import com.llmplatform.personalized.service.AIContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI内容生成控制器
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final AIContentService aiContentService;

    /**
     * 生成例句
     */
    @PostMapping("/example-sentences")
    public Result<List<String>> generateExampleSentences(@RequestBody ExampleSentenceRequest request) {
        List<String> sentences = aiContentService.generateExampleSentences(
                request.getWordId(),
                request.getWord(),
                request.getMasteryLevel(),
                request.getInterests()
        );
        return Result.success(sentences);
    }

    /**
     * 生成错题本练习
     */
    @PostMapping("/exercises")
    public Result<List<Map<String, Object>>> generateExercises(@RequestBody ErrorNotebookRequest request) {
        List<Map<String, Object>> exercises = aiContentService.generateErrorNotebookExercises(request.getWordIds());
        return Result.success(exercises);
    }

    /**
     * 生成对话场景
     */
    @PostMapping("/dialogue-scenario")
    public Result<Map<String, Object>> generateDialogueScenario(@RequestBody DialogueRequest request) {
        Map<String, Object> scenario = aiContentService.generateDialogueScenario(
                request.getGoalType(),
                request.getMasteryLevel()
        );
        return Result.success(scenario);
    }

    /**
     * 生成选择练习题
     */
    @PostMapping("/multiple-choice")
    public Result<List<Map<String, Object>>> generateMultipleChoice(@RequestBody MultipleChoiceRequest request) {
        List<Map<String, Object>> exercises = aiContentService.generateMultipleChoiceExercises(
                request.getWordId(),
                request.getWord(),
                request.getMasteryLevel(),
                request.getCount() != null ? request.getCount() : 5
        );
        return Result.success(exercises);
    }

    public static class ExampleSentenceRequest {
        private Long wordId;
        private String word;
        private int masteryLevel;
        private List<String> interests;

        public Long getWordId() { return wordId; }
        public void setWordId(Long wordId) { this.wordId = wordId; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
        public int getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }
        public List<String> getInterests() { return interests; }
        public void setInterests(List<String> interests) { this.interests = interests; }
    }

    public static class ErrorNotebookRequest {
        private List<Long> wordIds;

        public List<Long> getWordIds() { return wordIds; }
        public void setWordIds(List<Long> wordIds) { this.wordIds = wordIds; }
    }

    public static class DialogueRequest {
        private String goalType;
        private int masteryLevel;

        public String getGoalType() { return goalType; }
        public void setGoalType(String goalType) { this.goalType = goalType; }
        public int getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }
    }

    public static class MultipleChoiceRequest {
        private Long wordId;
        private String word;
        private int masteryLevel;
        private Integer count;

        public Long getWordId() { return wordId; }
        public void setWordId(Long wordId) { this.wordId = wordId; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
        public int getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
