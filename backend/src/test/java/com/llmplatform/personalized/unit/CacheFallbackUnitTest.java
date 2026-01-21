package com.llmplatform.personalized.unit;

import com.llmplatform.personalized.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存降级单元测试
 * 验证需求：7.7
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("缓存降级单元测试")
class CacheFallbackUnitTest {

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

    @Test
    @DisplayName("Redis不可用时返回空Optional")
    void redisUnavailable_ReturnsEmptyOptional() {
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis connection failed"));

        Optional<String> result = cacheService.get(key, String.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Redis异常时get返回空Optional")
    void redisGetException_ReturnsEmptyOptional() {
        String key = "test:key";

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis timeout"));

        Optional<String> result = cacheService.get(key, String.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("set操作失败返回false")
    void setOperationFailed_ReturnsFalse() {
        String key = "test:key";
        String value = "test value";

        doThrow(new RuntimeException("Write failed"))
                .when(valueOperations).set(anyString(), any(), any());

        boolean result = cacheService.set(key, value, java.time.Duration.ofHours(1));

        assertFalse(result);
    }

    @Test
    @DisplayName("delete操作失败返回false")
    void deleteOperationFailed_ReturnsFalse() {
        String key = "test:key";

        when(redisTemplate.delete(key)).thenThrow(new RuntimeException("Delete failed"));

        boolean result = cacheService.delete(key);

        assertFalse(result);
    }

    @Test
    @DisplayName("exists检查失败返回false")
    void existsCheckFailed_ReturnsFalse() {
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Connection lost"));

        boolean result = cacheService.exists(key);

        assertFalse(result);
    }

    @Test
    @DisplayName("getTtl失败返回null")
    void getTtlFailed_ReturnsNull() {
        String key = "test:key";

        when(redisTemplate.getExpire(eq(key), any())).thenThrow(new RuntimeException("TTL check failed"));

        Long result = cacheService.getTtl(key);

        assertNull(result);
    }

    @Test
    @DisplayName("降级逻辑 - 返回默认值")
    void fallbackLogic_ReturnsDefaultValue() {
        String key = "test:key";
        String defaultValue = "default";

        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis unavailable"));

        String result = cacheService.getWithFallback(key, String.class, defaultValue);

        assertEquals(defaultValue, result);
    }

    @Test
    @DisplayName("降级逻辑 - 缓存命中时返回值")
    void fallbackLogic_CacheHit_ReturnsValue() {
        String key = "test:key";
        String cachedValue = "cached value";
        String defaultValue = "default";

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(valueOperations.get(key)).thenReturn(cachedValue);

        String result = cacheService.getWithFallback(key, String.class, defaultValue);

        assertEquals(cachedValue, result);
    }

    @Test
    @DisplayName("increment操作失败返回null")
    void incrementFailed_ReturnsNull() {
        String key = "counter";

        when(valueOperations.increment(key)).thenThrow(new RuntimeException("Increment failed"));

        Long result = cacheService.increment(key);

        assertNull(result);
    }

    @Test
    @DisplayName("Redis连接恢复后正常工作")
    void redisConnectionRecovery_WorksNormally() {
        String key = "test:key";
        String value = "test value";

        // First call fails
        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Connection lost"));
        Optional<String> firstResult = cacheService.get(key, String.class);
        assertTrue(firstResult.isEmpty());

        // Simulate connection recovery
        reset(redisTemplate);
        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(value);

        // Second call should work
        Optional<String> secondResult = cacheService.get(key, String.class);
        assertTrue(secondResult.isPresent());
        assertEquals(value, secondResult.get());
    }

    @Test
    @DisplayName("批量缓存操作部分失败不影响其他")
    void batchCacheOperation_PartialFailureDoesNotAffectOthers() {
        String key1 = "cache:key1";
        String key2 = "cache:key2";

        when(redisTemplate.delete(key1)).thenReturn(true);
        when(redisTemplate.delete(key2)).thenThrow(new RuntimeException("Delete failed"));

        // Should not throw
        assertDoesNotThrow(() -> cacheService.delete(key1));
        assertFalse(cacheService.delete(key2));
    }

    @Test
    @DisplayName("用户缓存失效清理所有键")
    void userCacheInvalidation_CleansAllKeys() {
        Long userId = 1L;

        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> cacheService.invalidateUserCache(userId));

        verify(redisTemplate).delete("learning:profile:" + userId);
        verify(redisTemplate).delete("learning:plan:" + userId);
        verify(redisTemplate).delete("learning:streak:" + userId);
        verify(redisTemplate).delete("memory:due_reviews:" + userId);
    }

    @Test
    @DisplayName("类型转换异常返回空Optional")
    void typeConversionException_ReturnsEmptyOptional() {
        String key = "test:key";
        Integer cachedValue = 42;

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(valueOperations.get(key)).thenReturn(cachedValue);

        // Try to get as String when it's actually Integer
        Optional<String> result = cacheService.get(key, String.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("空值处理")
    void nullValueHandling() {
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(valueOperations.get(key)).thenReturn(null);

        Optional<String> result = cacheService.get(key, String.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("连续Redis故障不抛出异常")
    void continuousRedisFailures_NoExceptionThrown() {
        String key = "test:key";

        when(redisTemplate.hasKey(key))
                .thenThrow(new RuntimeException("Connection failed 1"))
                .thenThrow(new RuntimeException("Connection failed 2"));

        // Multiple calls should not throw
        assertTrue(cacheService.get(key, String.class).isEmpty());
        assertTrue(cacheService.get(key, String.class).isEmpty());
    }
}
