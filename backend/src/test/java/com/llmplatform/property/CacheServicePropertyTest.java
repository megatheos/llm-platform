package com.llmplatform.property;

import com.llmplatform.common.CacheConstants;
import com.llmplatform.service.CacheService;
import com.llmplatform.util.CacheUtil;
import com.llmplatform.vo.WordVO;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Property-based tests for CacheService
 * 
 * Feature: llm-language-learning-platform, Property 14: Cache consistency
 * 
 * For any cacheable data, if the data exists in cache, it should be identical
 * to the data that was stored.
 * 
 * Validates: Requirements 7.1, 7.2, 7.3
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheServicePropertyTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if Redis is available for testing
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: For any string value stored in cache, retrieving it should return
     * the exact same value.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void cacheConsistency_stringValues_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> "test:string:" + s + ":" + UUID.randomUUID().toString().substring(0, 8));
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(100);
        Arbitrary<Long> ttls = Arbitraries.longs().between(60, 3600);

        Arbitrary<CacheTestData<String>> testDataArbitrary = Combinators.combine(keys, values, ttls)
                .as(CacheTestData::new);

        for (int i = 0; i < 100; i++) {
            CacheTestData<String> testData = testDataArbitrary.sample();

            try {
                // Store value in cache
                boolean setResult = cacheService.set(testData.key, testData.value, testData.ttl, TimeUnit.SECONDS);
                assertThat(setResult).isTrue();

                // Retrieve value from cache
                Optional<Object> retrieved = cacheService.get(testData.key);

                // Assert - Retrieved value should match stored value
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get()).isEqualTo(testData.value);

                // Verify TTL is set (should be positive and <= original TTL)
                long remainingTtl = cacheService.getExpire(testData.key, TimeUnit.SECONDS);
                assertThat(remainingTtl).isGreaterThan(0);
                assertThat(remainingTtl).isLessThanOrEqualTo(testData.ttl);
            } finally {
                // Cleanup
                cacheService.delete(testData.key);
            }
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: For any WordVO object stored in cache, retrieving it should return
     * an object with identical field values.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void cacheConsistency_wordVOValues_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> words = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20);
        Arbitrary<String> langs = Arbitraries.of("en", "zh", "es", "fr", "de", "ja");
        Arbitrary<String> definitions = Arbitraries.strings().ofMinLength(10).ofMaxLength(200);
        Arbitrary<String> translations = Arbitraries.strings().ofMinLength(5).ofMaxLength(100);

        Arbitrary<WordVO> wordVOArbitrary = Combinators.combine(words, langs, langs, definitions, translations)
                .as((word, sourceLang, targetLang, definition, translation) -> {
                    WordVO vo = new WordVO();
                    vo.setId((long) (Math.random() * 10000));
                    vo.setWord(word);
                    vo.setSourceLang(sourceLang);
                    vo.setTargetLang(targetLang.equals(sourceLang) ? "en" : targetLang);
                    vo.setDefinition(definition);
                    vo.setTranslation(translation);
                    vo.setExamples("[\"Example 1\", \"Example 2\"]");
                    vo.setPronunciation("/test/");
                    vo.setCreatedAt(LocalDateTime.now());
                    return vo;
                });

        for (int i = 0; i < 100; i++) {
            WordVO originalWord = wordVOArbitrary.sample();
            String cacheKey = CacheConstants.wordKey(originalWord.getSourceLang(), 
                    originalWord.getTargetLang(), originalWord.getWord());

            try {
                // Store WordVO in cache using CacheUtil (as used in WordService)
                boolean setResult = cacheUtil.set(cacheKey, originalWord, CacheConstants.WORD_TTL_SECONDS);
                assertThat(setResult).isTrue();

                // Retrieve from cache
                WordVO retrieved = cacheUtil.get(cacheKey, WordVO.class);

                // Assert - All fields should match
                assertThat(retrieved).isNotNull();
                assertThat(retrieved.getId()).isEqualTo(originalWord.getId());
                assertThat(retrieved.getWord()).isEqualTo(originalWord.getWord());
                assertThat(retrieved.getSourceLang()).isEqualTo(originalWord.getSourceLang());
                assertThat(retrieved.getTargetLang()).isEqualTo(originalWord.getTargetLang());
                assertThat(retrieved.getDefinition()).isEqualTo(originalWord.getDefinition());
                assertThat(retrieved.getTranslation()).isEqualTo(originalWord.getTranslation());
                assertThat(retrieved.getExamples()).isEqualTo(originalWord.getExamples());
                assertThat(retrieved.getPronunciation()).isEqualTo(originalWord.getPronunciation());
            } finally {
                // Cleanup
                cacheUtil.delete(cacheKey);
            }
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: After cache expiration (TTL), the data should no longer be retrievable.
     * 
     * Validates: Requirements 7.3
     */
    @Test
    void cacheExpiration_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15)
                .map(s -> "test:expire:" + s + ":" + UUID.randomUUID().toString().substring(0, 8));
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(50);

        Arbitrary<CacheTestData<String>> testDataArbitrary = Combinators.combine(keys, values, Arbitraries.just(1L))
                .as(CacheTestData::new);

        for (int i = 0; i < 10; i++) {
            CacheTestData<String> testData = testDataArbitrary.sample();

            try {
                // Ensure key doesn't exist before test (cleanup from any previous runs)
                cacheService.delete(testData.key);
                
                // Store with 1 second TTL
                boolean setResult = cacheService.set(testData.key, testData.value, 1, TimeUnit.SECONDS);
                assertThat(setResult).isTrue();

                // Verify it exists immediately
                assertThat(cacheService.exists(testData.key)).isTrue();

                // Wait for expiration (2 seconds to be safe with timing variations)
                Thread.sleep(2100);

                // Verify it no longer exists
                Optional<Object> retrieved = cacheService.get(testData.key);
                assertThat(retrieved).isEmpty();
                assertThat(cacheService.exists(testData.key)).isFalse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                cacheService.delete(testData.key);
            }
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: Deleting a cached value should make it no longer retrievable.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void cacheDelete_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> "test:delete:" + s + ":" + UUID.randomUUID().toString().substring(0, 8));
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(100);

        Arbitrary<CacheTestData<String>> testDataArbitrary = Combinators.combine(keys, values, Arbitraries.just(3600L))
                .as(CacheTestData::new);

        for (int i = 0; i < 100; i++) {
            CacheTestData<String> testData = testDataArbitrary.sample();

            // Store value
            cacheService.set(testData.key, testData.value, testData.ttl, TimeUnit.SECONDS);

            // Verify it exists
            assertThat(cacheService.exists(testData.key)).isTrue();

            // Delete it
            boolean deleteResult = cacheService.delete(testData.key);
            assertThat(deleteResult).isTrue();

            // Verify it no longer exists
            Optional<Object> retrieved = cacheService.get(testData.key);
            assertThat(retrieved).isEmpty();
            assertThat(cacheService.exists(testData.key)).isFalse();
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: setIfAbsent should only set value if key doesn't exist.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void cacheSetIfAbsent_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> "test:setifabsent:" + s + ":" + UUID.randomUUID().toString().substring(0, 8));
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(50);

        Arbitrary<CacheTestData<String>> testDataArbitrary = Combinators.combine(keys, values, Arbitraries.just(3600L))
                .as(CacheTestData::new);

        for (int i = 0; i < 100; i++) {
            CacheTestData<String> testData = testDataArbitrary.sample();
            String secondValue = "different_" + testData.value;

            try {
                // First setIfAbsent should succeed
                boolean firstSet = cacheService.setIfAbsent(testData.key, testData.value, testData.ttl, TimeUnit.SECONDS);
                assertThat(firstSet).isTrue();

                // Second setIfAbsent with different value should fail
                boolean secondSet = cacheService.setIfAbsent(testData.key, secondValue, testData.ttl, TimeUnit.SECONDS);
                assertThat(secondSet).isFalse();

                // Value should still be the original
                Optional<Object> retrieved = cacheService.get(testData.key);
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get()).isEqualTo(testData.value);
            } finally {
                cacheService.delete(testData.key);
            }
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 14: Cache consistency
     * 
     * Property: getAndRefresh should return value and extend TTL.
     * 
     * Validates: Requirements 7.1, 7.2, 7.3
     */
    @Test
    void cacheGetAndRefresh_property() {
        assumeTrue(isRedisAvailable(), "Redis is not available, skipping cache test");

        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> "test:refresh:" + s + ":" + UUID.randomUUID().toString().substring(0, 8));
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(50);

        Arbitrary<CacheTestData<String>> testDataArbitrary = Combinators.combine(keys, values, Arbitraries.just(60L))
                .as(CacheTestData::new);

        for (int i = 0; i < 100; i++) {
            CacheTestData<String> testData = testDataArbitrary.sample();
            long newTtl = 3600L;

            try {
                // Store with short TTL
                cacheUtil.set(testData.key, testData.value, testData.ttl);

                // Get initial TTL
                Long initialTtl = cacheUtil.getExpire(testData.key);
                assertThat(initialTtl).isNotNull();
                assertThat(initialTtl).isLessThanOrEqualTo(testData.ttl);

                // Get and refresh with longer TTL
                String retrieved = cacheUtil.getAndRefresh(testData.key, String.class, newTtl);

                // Value should match
                assertThat(retrieved).isEqualTo(testData.value);

                // TTL should be extended
                Long refreshedTtl = cacheUtil.getExpire(testData.key);
                assertThat(refreshedTtl).isNotNull();
                assertThat(refreshedTtl).isGreaterThan(initialTtl);
                assertThat(refreshedTtl).isLessThanOrEqualTo(newTtl);
            } finally {
                cacheUtil.delete(testData.key);
            }
        }
    }

    /**
     * Helper class for cache test data
     */
    private static class CacheTestData<T> {
        final String key;
        final T value;
        final Long ttl;

        CacheTestData(String key, T value, Long ttl) {
            this.key = key;
            this.value = value;
            this.ttl = ttl;
        }
    }
}
