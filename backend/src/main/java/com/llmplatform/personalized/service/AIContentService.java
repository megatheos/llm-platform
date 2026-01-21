package com.llmplatform.personalized.service;

import java.util.List;
import java.util.Map;

/**
 * AI内容生成服务接口
 */
public interface AIContentService {

    /**
     * 生成例句
     *
     * @param wordId 词汇ID
     * @param word 词汇内容
     * @param masteryLevel 掌握度
     * @param interests 兴趣标签
     * @return 例句列表
     */
    List<String> generateExampleSentences(Long wordId, String word, int masteryLevel, List<String> interests);

    /**
     * 生成错题本练习
     *
     * @param wordIds 错误词汇ID列表
     * @return 练习题列表
     */
    List<Map<String, Object>> generateErrorNotebookExercises(List<Long> wordIds);

    /**
     * 生成对话场景
     *
     * @param goalType 学习目标类型
     * @param masteryLevel 掌握度
     * @return 对话场景
     */
    Map<String, Object> generateDialogueScenario(String goalType, int masteryLevel);

    /**
     * 生成选择练习题
     *
     * @param wordId 词汇ID
     * @param word 词汇
     * @param masteryLevel 掌握度
     * @param count 题目数量
     * @return 练习题列表
     */
    List<Map<String, Object>> generateMultipleChoiceExercises(Long wordId, String word, int masteryLevel, int count);
}
