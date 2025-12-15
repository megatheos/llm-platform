package com.llmplatform.property;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.common.ActivityType;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.LearningRecord;
import com.llmplatform.entity.Word;
import com.llmplatform.mapper.LearningRecordMapper;
import com.llmplatform.mapper.WordMapper;
import com.llmplatform.mapper.WordQueryHistoryMapper;
import com.llmplatform.service.UserService;
import com.llmplatform.service.WordService;
import com.llmplatform.vo.UserVO;
import com.llmplatform.vo.WordVO;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
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
 * Property-based tests for WordService
 */
@SpringBootTest
@ActiveProfiles("test")
class WordServicePropertyTest {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private WordQueryHistoryMapper wordQueryHistoryMapper;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Autowired
    private UserService userService;


    /**
     * Feature: llm-language-learning-platform, Property 3: Word query caching round-trip
     * 
     * For any word query with source and target language, the first query should store the result,
     * and subsequent queries for the same word/language pair should return identical results.
     * 
     * Note: This test validates the database round-trip behavior. Cache behavior is tested
     * when Redis is available, but the core property (repeated queries return identical results)
     * is validated through database consistency.
     * 
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    @Transactional
    void wordQueryCachingRoundTrip_property() {
        // Create test user
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();

        Arbitrary<String> words = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20)
                .map(String::toLowerCase);
        Arbitrary<String> sourceLangs = Arbitraries.of("en", "zh", "es", "fr", "de", "ja");
        Arbitrary<String> targetLangs = Arbitraries.of("en", "zh", "es", "fr", "de", "ja");

        Arbitrary<WordTestData> testDataArbitrary = Combinators.combine(words, sourceLangs, targetLangs)
                .as((word, source, target) -> {
                    // Ensure source and target are different
                    String actualTarget = source.equals(target) ? 
                            (target.equals("en") ? "zh" : "en") : target;
                    return new WordTestData(word + UUID.randomUUID().toString().substring(0, 4), 
                            source, actualTarget);
                });

        for (int i = 0; i < 100; i++) {
            WordTestData testData = testDataArbitrary.sample();
            
            // Pre-populate the database with a word (simulating AI generation result)
            Word prePopulatedWord = new Word();
            prePopulatedWord.setWord(testData.word.toLowerCase());
            prePopulatedWord.setSourceLang(testData.sourceLang);
            prePopulatedWord.setTargetLang(testData.targetLang);
            prePopulatedWord.setDefinition("Test definition for " + testData.word);
            prePopulatedWord.setTranslation("Test translation for " + testData.word);
            prePopulatedWord.setExamples("[\"Example 1\", \"Example 2\"]");
            prePopulatedWord.setPronunciation("/test/");
            prePopulatedWord.setCreatedAt(LocalDateTime.now());
            wordMapper.insert(prePopulatedWord);

            // First query - should retrieve from database (and attempt to cache)
            WordVO firstResult = wordService.query(testData.word, testData.sourceLang, 
                    testData.targetLang, userId);

            // Second query - should retrieve same result (from cache if available, or database)
            WordVO secondResult = wordService.query(testData.word, testData.sourceLang, 
                    testData.targetLang, userId);

            // Assert - Both queries should return identical results (round-trip property)
            assertThat(firstResult).isNotNull();
            assertThat(secondResult).isNotNull();
            
            assertThat(secondResult.getId()).isEqualTo(firstResult.getId());
            assertThat(secondResult.getWord()).isEqualTo(firstResult.getWord());
            assertThat(secondResult.getSourceLang()).isEqualTo(firstResult.getSourceLang());
            assertThat(secondResult.getTargetLang()).isEqualTo(firstResult.getTargetLang());
            assertThat(secondResult.getDefinition()).isEqualTo(firstResult.getDefinition());
            assertThat(secondResult.getTranslation()).isEqualTo(firstResult.getTranslation());
            assertThat(secondResult.getExamples()).isEqualTo(firstResult.getExamples());
            assertThat(secondResult.getPronunciation()).isEqualTo(firstResult.getPronunciation());
        }
    }


    /**
     * Feature: llm-language-learning-platform, Property 4: Language pair consistency
     * 
     * For any word query with specified source and target languages, the returned translation
     * should have matching source_lang and target_lang fields.
     * 
     * Validates: Requirements 2.3
     */
    @Test
    @Transactional
    void languagePairConsistency_property() {
        // Create test user
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();

        Arbitrary<String> words = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20)
                .map(String::toLowerCase);
        Arbitrary<String> sourceLangs = Arbitraries.of("en", "zh", "es", "fr", "de", "ja");
        Arbitrary<String> targetLangs = Arbitraries.of("en", "zh", "es", "fr", "de", "ja");

        Arbitrary<WordTestData> testDataArbitrary = Combinators.combine(words, sourceLangs, targetLangs)
                .as((word, source, target) -> {
                    String actualTarget = source.equals(target) ? 
                            (target.equals("en") ? "zh" : "en") : target;
                    return new WordTestData(word + UUID.randomUUID().toString().substring(0, 4), 
                            source, actualTarget);
                });

        for (int i = 0; i < 100; i++) {
            WordTestData testData = testDataArbitrary.sample();
            
            // Pre-populate the database with a word
            Word prePopulatedWord = new Word();
            prePopulatedWord.setWord(testData.word.toLowerCase());
            prePopulatedWord.setSourceLang(testData.sourceLang);
            prePopulatedWord.setTargetLang(testData.targetLang);
            prePopulatedWord.setDefinition("Definition in " + testData.sourceLang);
            prePopulatedWord.setTranslation("Translation in " + testData.targetLang);
            prePopulatedWord.setExamples("[]");
            prePopulatedWord.setPronunciation("");
            prePopulatedWord.setCreatedAt(LocalDateTime.now());
            wordMapper.insert(prePopulatedWord);

            // Query the word
            WordVO result = wordService.query(testData.word, testData.sourceLang, 
                    testData.targetLang, userId);

            // Assert - Language pair should match the request
            assertThat(result).isNotNull();
            assertThat(result.getSourceLang()).isEqualTo(testData.sourceLang);
            assertThat(result.getTargetLang()).isEqualTo(testData.targetLang);
        }
    }


    /**
     * Feature: llm-language-learning-platform, Property 5: Word query creates learning record
     * 
     * For any completed word query, a corresponding learning record should exist in the user's
     * history with matching word_id and timestamp.
     * 
     * Validates: Requirements 2.5
     */
    @Test
    @Transactional
    void wordQueryCreatesLearningRecord_property() {
        // Create test user
        UserVO testUser = createTestUser();
        Long userId = testUser.getId();

        Arbitrary<String> words = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20)
                .map(String::toLowerCase);
        Arbitrary<String> sourceLangs = Arbitraries.of("en", "zh", "es", "fr", "de");
        Arbitrary<String> targetLangs = Arbitraries.of("en", "zh", "es", "fr", "de");

        Arbitrary<WordTestData> testDataArbitrary = Combinators.combine(words, sourceLangs, targetLangs)
                .as((word, source, target) -> {
                    String actualTarget = source.equals(target) ? 
                            (target.equals("en") ? "zh" : "en") : target;
                    return new WordTestData(word + UUID.randomUUID().toString().substring(0, 4), 
                            source, actualTarget);
                });

        for (int i = 0; i < 100; i++) {
            WordTestData testData = testDataArbitrary.sample();
            
            // Pre-populate the database with a word
            Word prePopulatedWord = new Word();
            prePopulatedWord.setWord(testData.word.toLowerCase());
            prePopulatedWord.setSourceLang(testData.sourceLang);
            prePopulatedWord.setTargetLang(testData.targetLang);
            prePopulatedWord.setDefinition("Definition");
            prePopulatedWord.setTranslation("Translation");
            prePopulatedWord.setExamples("[]");
            prePopulatedWord.setPronunciation("");
            prePopulatedWord.setCreatedAt(LocalDateTime.now());
            wordMapper.insert(prePopulatedWord);

            LocalDateTime beforeQuery = LocalDateTime.now().minusSeconds(1);

            // Query the word
            WordVO result = wordService.query(testData.word, testData.sourceLang, 
                    testData.targetLang, userId);

            LocalDateTime afterQuery = LocalDateTime.now().plusSeconds(1);

            // Assert - Learning record should exist
            List<LearningRecord> records = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getActivityType, ActivityType.WORD_QUERY.name())
                    .eq(LearningRecord::getActivityId, result.getId())
            );

            assertThat(records).isNotEmpty();
            
            LearningRecord record = records.get(records.size() - 1);
            assertThat(record.getUserId()).isEqualTo(userId);
            assertThat(record.getActivityType()).isEqualTo(ActivityType.WORD_QUERY.name());
            assertThat(record.getActivityId()).isEqualTo(result.getId());
            assertThat(record.getActivityTime()).isAfterOrEqualTo(beforeQuery);
            assertThat(record.getActivityTime()).isBeforeOrEqualTo(afterQuery);
        }
    }

    /**
     * Helper method to create a test user
     */
    private UserVO createTestUser() {
        RegisterDTO dto = new RegisterDTO();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        dto.setUsername("testuser" + uniqueSuffix);
        dto.setPassword("password123");
        dto.setEmail("test" + uniqueSuffix + "@test.com");
        return userService.register(dto);
    }

    /**
     * Helper class for test data
     */
    private static class WordTestData {
        final String word;
        final String sourceLang;
        final String targetLang;

        WordTestData(String word, String sourceLang, String targetLang) {
            this.word = word;
            this.sourceLang = sourceLang;
            this.targetLang = targetLang;
        }
    }
}
