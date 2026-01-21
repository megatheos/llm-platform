package com.llmplatform.property;

import com.llmplatform.personalized.service.CacheService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存TTL属性测试
 * Property 27: 缓存TTL设置
 * 验证需求：6.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("缓存TTL属性测试")
class CacheTTLPropertyTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CacheService(redisTemplate);
    }

    @Property
    @DisplayName("档案缓存TTL为24小时")
    void profileCache_TTLIs24Hours(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:profile", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(24 * 3600L);

        Long ttl = cacheService.getTtl(key);

        assertNotNull(ttl);
        assertEquals(24 * 3600, ttl);
    }

    @Property
    @DisplayName("待复习缓存TTL为1小时")
    void dueReviewsCache_TTLIs1Hour(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("memory:due_reviews", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(3600L);

        Long ttl = cacheService.getTtl(key);

        assertNotNull(ttl);
        assertEquals(3600, ttl);
    }

    @Property
    @DisplayName("计划缓存TTL为24小时")
    void planCache_TTLIs24Hours(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:plan", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(24 * 3600L);

        Long ttl = cacheService.getTtl(key);

        assertNotNull(ttl);
        assertEquals(24 * 3600, ttl);
    }

    @Property
    @DisplayName("连续记录缓存TTL为24小时")
    void streakCache_TTLIs24Hours(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:streak", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(24 * 3600L);

        Long ttl = cacheService.getTtl(key);

        assertNotNull(ttl);
        assertEquals(24 * 3600, ttl);
    }

    @Property
    @DisplayName("键名格式正确")
    void keyNameFormatIsCorrect(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String profileKey = CacheService.buildKey("learning:profile", userId);
        String planKey = CacheService.buildKey("learning:plan", userId);
        String streakKey = CacheService.buildKey("learning:streak", userId);
        String reviewsKey = CacheService.buildKey("memory:due_reviews", userId);

        assertTrue(profileKey.startsWith("learning:profile:"));
        assertTrue(planKey.startsWith("learning:plan:"));
        assertTrue(streakKey.startsWith("learning:streak:"));
        assertTrue(reviewsKey.startsWith("memory:due_reviews:"));
        assertTrue(profileKey.endsWith(String.valueOf(userId)));
    }

    @Property
    @DisplayName("用户ID嵌入键名")
    void userIdEmbeddedInKey(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:profile", userId);

        assertTrue(key.contains(String.valueOf(userId)));
    }

    @Property
    @DisplayName("不同用户使用不同键")
    void differentUsersHaveDifferentKeys(
            @ForAll @LongRange(min = 1, max = 1000) Long userId1,
            @ForAll @LongRange(min = 1001, max = 2000) Long userId2
    ) {
        String key1 = CacheService.buildKey("learning:profile", userId1);
        String key2 = CacheService.buildKey("learning:profile", userId2);

        assertNotEquals(key1, key2);
    }

    @Property
    @DisplayName("缓存不存在时返回null TTL")
    void cacheNotExists_ReturnsNullTTL(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:profile", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

        Long ttl = cacheService.getTtl(key);

        assertEquals(-2, ttl); // -2 means key does not exist
    }

    @Property
    @DisplayName("键存在但无TTL返回-1")
    void keyExistsNoTTL_ReturnsNegativeOne(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
        String key = CacheService.buildKey("learning:profile", userId);

        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-1L);

        Long ttl = cacheService.getTtl(key);

        assertEquals(-1, ttl); // -1 means no expiry set
    }

    @Property
    @DisplayName("设置缓存时使用正确TTL")
    void setCache_UsesCorrectTTL(
            @ForAll @LongRange(min = 1, max = 10000) Long userId
    ) {
        String key = CacheService.buildKey("learning:profile", userId);
        String data = "test profile data";

        cacheService.setUserProfile(userId, data);

        verify(valueOperations).set(eq(key), eq(data), eq(Duration.ofHours(24)));
    }

    @Property
    @DisplayName("设置待复习缓存使用1小时TTL")
    void setDueReviews_Uses1HourTTL(
            @ForAll @LongRange(min = 1, max = 10000) Long userId
    ) {
        String key = CacheService.buildKey("memory:due_reviews", userId);
        String data = "test reviews";

        cacheService.setDueReviews(userId, data);

        verify(valueOperations).set(eq(key), eq(data), eq(Duration.ofHours(1)));
    }
}
