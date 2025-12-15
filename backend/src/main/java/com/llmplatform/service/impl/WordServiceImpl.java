package com.llmplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.ai.dto.AIRequest;
import com.llmplatform.ai.dto.AIResponse;
import com.llmplatform.ai.gateway.AIGateway;
import com.llmplatform.common.ActivityType;
import com.llmplatform.common.CacheConstants;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Word;
import com.llmplatform.entity.WordQueryHistory;
import com.llmplatform.exception.BusinessException;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.WordMapper;
import com.llmplatform.mapper.WordQueryHistoryMapper;
import com.llmplatform.service.WordService;
import com.llmplatform.util.CacheUtil;
import com.llmplatform.vo.WordHistoryVO;
import com.llmplatform.vo.WordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Word service implementation with AI generation and caching
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordServiceImpl implements WordService {

    private final WordMapper wordMapper;
    private final WordQueryHistoryMapper wordQueryHistoryMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final AIGateway aiGateway;
    private final CacheUtil cacheUtil;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional
    public WordVO query(String word, String sourceLang, String targetLang, Long userId) {
        String normalizedWord = word.trim().toLowerCase();
        String cacheKey = CacheConstants.wordKey(sourceLang, targetLang, normalizedWord);

        // 1. Check Redis cache first
        WordVO cachedWord = cacheUtil.get(cacheKey, WordVO.class);
        if (cachedWord != null) {
            log.debug("Word found in cache: {}", normalizedWord);
            recordWordQuery(userId, cachedWord.getId());
            return cachedWord;
        }

        // 2. Check database
        Word dbWord = wordMapper.selectOne(
            new LambdaQueryWrapper<Word>()
                .eq(Word::getWord, normalizedWord)
                .eq(Word::getSourceLang, sourceLang)
                .eq(Word::getTargetLang, targetLang)
        );

        if (dbWord != null) {
            log.debug("Word found in database: {}", normalizedWord);
            WordVO wordVO = convertToVO(dbWord);
            // Cache the result
            cacheUtil.set(cacheKey, wordVO, CacheConstants.WORD_TTL_SECONDS);
            recordWordQuery(userId, dbWord.getId());
            return wordVO;
        }

        // 3. Generate via AI
        log.info("Generating word definition via AI: {} ({} -> {})", normalizedWord, sourceLang, targetLang);
        Word generatedWord = generateWordViaAI(normalizedWord, sourceLang, targetLang);
        
        // Save to database
        wordMapper.insert(generatedWord);
        
        WordVO wordVO = convertToVO(generatedWord);
        // Cache the result
        cacheUtil.set(cacheKey, wordVO, CacheConstants.WORD_TTL_SECONDS);
        
        // Record the query
        recordWordQuery(userId, generatedWord.getId());
        
        return wordVO;
    }

    @Override
    public List<WordHistoryVO> getHistory(Long userId) {
        // Get query history ordered by query time descending
        List<WordQueryHistory> historyList = wordQueryHistoryMapper.selectList(
            new LambdaQueryWrapper<WordQueryHistory>()
                .eq(WordQueryHistory::getUserId, userId)
                .orderByDesc(WordQueryHistory::getQueryTime)
        );

        return historyList.stream()
            .map(this::convertToHistoryVO)
            .collect(Collectors.toList());
    }

    @Override
    public WordVO getWordById(Long wordId) {
        Word word = wordMapper.selectById(wordId);
        return word != null ? convertToVO(word) : null;
    }

    /**
     * Generate word definition and translation via AI
     */
    private Word generateWordViaAI(String word, String sourceLang, String targetLang) {
        String targetLangName = getLanguageName(targetLang);
        String sourceLangName = getLanguageName(sourceLang);
        
        String systemMessage = String.format(
            "You are a language learning assistant. Provide word definitions and translations. " +
            "Respond ONLY with a valid JSON object (no markdown, no code blocks) with these fields: " +
            "definition (MUST be written in %s, explaining what the word means), " +
            "translation (the word translated to %s), " +
            "examples (array of 2-3 objects, each containing 'sentence' which is an example sentence in %s, and 'translation' which is the %s translation of that sentence), " +
            "pronunciation (phonetic guide).",
            targetLangName, targetLangName, sourceLangName, targetLangName
        );

        String prompt = String.format(
            "Word: '%s' (from %s to %s)\n" +
            "IMPORTANT: The 'definition' field MUST be written entirely in %s (not English unless target is English).\n" +
            "The 'examples' array must contain objects with 'sentence' (in %s) and 'translation' (in %s).\n" +
            "Return ONLY a valid JSON object.",
            word, sourceLangName, targetLangName, targetLangName, sourceLangName, targetLangName
        );

        AIRequest request = AIRequest.builder()
            .systemMessage(systemMessage)
            .prompt(prompt)
            .temperature(0.3)
            .maxTokens(1024)
            .build();

        AIResponse response = aiGateway.generate(request);

        if (!response.isSuccess()) {
            log.error("AI generation failed: {}", response.getErrorMessage());
            throw new BusinessException("AI_ERROR", "Failed to generate word definition: " + response.getErrorMessage());
        }

        return parseAIResponse(word, sourceLang, targetLang, response.getContent());
    }

    /**
     * Get full language name from language code
     */
    private String getLanguageName(String langCode) {
        return switch (langCode.toLowerCase()) {
            case "en" -> "English";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            default -> langCode;
        };
    }

    /**
     * Parse AI response into Word entity
     */
    private Word parseAIResponse(String word, String sourceLang, String targetLang, String content) {
        Word wordEntity = new Word();
        wordEntity.setWord(word);
        wordEntity.setSourceLang(sourceLang);
        wordEntity.setTargetLang(targetLang);
        wordEntity.setCreatedAt(LocalDateTime.now());

        try {
            // Clean up the content - remove markdown code blocks if present
            String cleanContent = content.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7);
            } else if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3);
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
            }
            cleanContent = cleanContent.trim();

            JsonNode json = objectMapper.readTree(cleanContent);
            
            wordEntity.setDefinition(getJsonString(json, "definition"));
            wordEntity.setTranslation(getJsonString(json, "translation"));
            wordEntity.setPronunciation(getJsonString(json, "pronunciation"));
            
            // Handle examples as JSON array string
            if (json.has("examples") && json.get("examples").isArray()) {
                wordEntity.setExamples(json.get("examples").toString());
            } else if (json.has("examples")) {
                wordEntity.setExamples(json.get("examples").asText());
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON, using raw content: {}", e.getMessage());
            // Fallback: use raw content as definition
            wordEntity.setDefinition(content);
            wordEntity.setTranslation("");
            wordEntity.setExamples("[]");
            wordEntity.setPronunciation("");
        }

        return wordEntity;
    }

    private String getJsonString(JsonNode json, String field) {
        if (json.has(field) && !json.get(field).isNull()) {
            return json.get(field).asText();
        }
        return "";
    }

    /**
     * Record word query in history and learning records
     */
    private void recordWordQuery(Long userId, Long wordId) {
        // Record in word query history
        WordQueryHistory history = new WordQueryHistory();
        history.setUserId(userId);
        history.setWordId(wordId);
        history.setQueryTime(LocalDateTime.now());
        wordQueryHistoryMapper.insert(history);

        // Record in learning records
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setActivityType(ActivityType.WORD_QUERY.name());
        record.setActivityId(wordId);
        record.setActivityTime(LocalDateTime.now());
        learningRecordMapper.insert(record);
        
        // Invalidate statistics cache
        String statsCacheKey = CacheConstants.statsKey(userId);
        cacheUtil.delete(statsCacheKey);
    }

    /**
     * Convert Word entity to WordVO
     */
    private WordVO convertToVO(Word word) {
        WordVO vo = new WordVO();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setSourceLang(word.getSourceLang());
        vo.setTargetLang(word.getTargetLang());
        vo.setDefinition(word.getDefinition());
        vo.setTranslation(word.getTranslation());
        vo.setExamples(word.getExamples());
        vo.setPronunciation(word.getPronunciation());
        vo.setCreatedAt(word.getCreatedAt());
        return vo;
    }

    /**
     * Convert WordQueryHistory to WordHistoryVO
     */
    private WordHistoryVO convertToHistoryVO(WordQueryHistory history) {
        WordHistoryVO vo = new WordHistoryVO();
        vo.setId(history.getId());
        vo.setWordId(history.getWordId());
        vo.setQueryTime(history.getQueryTime());

        // Fetch word details
        Word word = wordMapper.selectById(history.getWordId());
        if (word != null) {
            vo.setWord(word.getWord());
            vo.setSourceLang(word.getSourceLang());
            vo.setTargetLang(word.getTargetLang());
            vo.setTranslation(word.getTranslation());
        }

        return vo;
    }
}
