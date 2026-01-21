package com.llmplatform.personalized.unit;

import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.engine.SpacedRepetitionEngine;
import com.llmplatform.personalized.repository.MemoryRepository;
import com.llmplatform.personalized.service.impl.MemoryServiceImpl;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemoryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoryServiceUnitTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private SpacedRepetitionEngine spacedRepetitionEngine;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MemoryServiceImpl memoryService;

    private MemoryRecord testRecord;
    private Long userId = 1L;
    private Long wordId = 100L;

    @BeforeEach
    void setUp() {
        testRecord = new MemoryRecord();
        testRecord.setId(1L);
        testRecord.setUserId(userId);
        testRecord.setWordId(wordId);
        testRecord.setMasteryLevel(50);
        testRecord.setReviewCount(5);
        testRecord.setCorrectCount(3);
        testRecord.setWrongCount(2);
        testRecord.setStatus("LEARNING");
        testRecord.setCreatedTime(LocalDateTime.now().minusDays(1));
        testRecord.setUpdatedTime(LocalDateTime.now().minusHours(1));

        // Common stubbing
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("创建记忆记录 - 初始值正确设置")
    void testCreateRecord_InitialValuesSetCorrectly() {
        when(memoryRepository.create(any(MemoryRecord.class))).thenAnswer(invocation -> {
            MemoryRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        MemoryRecord result = memoryService.createRecord(userId, wordId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(wordId, result.getWordId());
        assertEquals(0, result.getMasteryLevel());
        assertEquals(0, result.getReviewCount());
        assertEquals(0, result.getCorrectCount());
        assertEquals(0, result.getWrongCount());
        assertEquals("LEARNING", result.getStatus());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdatedTime());

        verify(memoryRepository).create(any(MemoryRecord.class));
    }

    @Test
    @DisplayName("创建或获取记录 - 记录存在时返回已有记录")
    void testCreateOrGetRecord_RecordExists_ReturnsExisting() {
        when(memoryRepository.findByUserIdAndWordId(userId, wordId))
                .thenReturn(Optional.of(testRecord));

        MemoryRecord result = memoryService.createOrGetRecord(userId, wordId);

        assertNotNull(result);
        assertEquals(testRecord.getId(), result.getId());
        verify(memoryRepository).findByUserIdAndWordId(userId, wordId);
        verify(memoryRepository, never()).create(any());
    }

    @Test
    @DisplayName("创建或获取记录 - 记录不存在时创建新记录")
    void testCreateOrGetRecord_RecordNotExists_CreatesNew() {
        when(memoryRepository.findByUserIdAndWordId(userId, wordId))
                .thenReturn(Optional.empty());
        when(memoryRepository.create(any(MemoryRecord.class))).thenAnswer(invocation -> {
            MemoryRecord record = invocation.getArgument(0);
            record.setId(2L);
            return record;
        });

        MemoryRecord result = memoryService.createOrGetRecord(userId, wordId);

        assertNotNull(result);
        assertEquals(0, result.getMasteryLevel());
        verify(memoryRepository).findByUserIdAndWordId(userId, wordId);
        verify(memoryRepository).create(any(MemoryRecord.class));
    }

    @Test
    @DisplayName("提交复习结果 - 正确更新掌握度和状态")
    void testSubmitReview_CorrectAnswer_UpdatesMasteryAndStatus() {
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(spacedRepetitionEngine.updateMasteryLevel(50, true)).thenReturn(60);
        when(spacedRepetitionEngine.determineStatus(60)).thenReturn("LEARNING");
        when(spacedRepetitionEngine.calculateReviewInterval(eq(60), anyInt(), anyInt())).thenReturn(8);
        when(memoryRepository.update(any(MemoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        MemoryRecord result = memoryService.submitReview(1L, true);

        assertNotNull(result);
        assertEquals(60, result.getMasteryLevel());
        assertEquals(4, result.getCorrectCount());
        assertEquals(6, result.getReviewCount());
        assertEquals("LEARNING", result.getStatus());

        verify(memoryRepository).findById(1L);
        verify(memoryRepository).update(any(MemoryRecord.class));
    }

    @Test
    @DisplayName("提交复习结果 - 错误更新掌握度")
    void testSubmitReview_WrongAnswer_UpdatesMastery() {
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(spacedRepetitionEngine.updateMasteryLevel(50, false)).thenReturn(35);
        when(spacedRepetitionEngine.determineStatus(35)).thenReturn("LEARNING");
        when(spacedRepetitionEngine.calculateReviewInterval(eq(35), anyInt(), anyInt())).thenReturn(4);
        when(memoryRepository.update(any(MemoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        MemoryRecord result = memoryService.submitReview(1L, false);

        assertNotNull(result);
        assertEquals(35, result.getMasteryLevel());
        assertEquals(3, result.getWrongCount());
        assertEquals(6, result.getReviewCount());

        verify(memoryRepository).findById(1L);
        verify(memoryRepository).update(any(MemoryRecord.class));
    }

    @Test
    @DisplayName("提交复习结果 - 记录不存在抛出异常")
    void testSubmitReview_RecordNotExists_ThrowsException() {
        when(memoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            memoryService.submitReview(999L, true);
        });

        verify(memoryRepository).findById(999L);
        verify(memoryRepository, never()).update(any());
    }

    @Test
    @DisplayName("获取待复习记录 - 从缓存获取")
    void testGetDueReviews_FromCache() {
        List<MemoryRecord> cachedRecords = Arrays.asList(testRecord);
        when(valueOperations.get(anyString())).thenReturn(cachedRecords);

        List<MemoryRecord> result = memoryService.getDueReviews(userId, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRecord.getId(), result.get(0).getId());

        verify(valueOperations).get(contains(String.valueOf(userId)));
        verify(memoryRepository, never()).findDueReviewsByUserId(any(), any(), anyInt());
    }

    @Test
    @DisplayName("获取待复习记录 - 缓存未命中从数据库获取")
    void testGetDueReviews_CacheMiss_FetchesFromDatabase() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(memoryRepository.findDueReviewsByUserId(eq(userId), any(), eq(10)))
                .thenReturn(Arrays.asList(testRecord));

        List<MemoryRecord> result = memoryService.getDueReviews(userId, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(valueOperations).get(contains(String.valueOf(userId)));
        verify(memoryRepository).findDueReviewsByUserId(eq(userId), any(), eq(10));
        // opsForValue is called twice, but set is only called once (when cache misses)
        verify(valueOperations).set(anyString(), anyList(), any());
    }

    @Test
    @DisplayName("获取记忆记录 - 记录存在")
    void testGetRecord_RecordExists() {
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        MemoryRecord result = memoryService.getRecord(1L);

        assertNotNull(result);
        assertEquals(testRecord.getId(), result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(wordId, result.getWordId());

        verify(memoryRepository).findById(1L);
    }

    @Test
    @DisplayName("获取记忆记录 - 记录不存在抛出异常")
    void testGetRecord_RecordNotExists_ThrowsException() {
        when(memoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            memoryService.getRecord(999L);
        });

        verify(memoryRepository).findById(999L);
    }

    @Test
    @DisplayName("获取用户和词汇的记忆记录")
    void testGetRecordByUserIdAndWordId() {
        when(memoryRepository.findByUserIdAndWordId(userId, wordId))
                .thenReturn(Optional.of(testRecord));

        MemoryRecord result = memoryService.getRecordByUserIdAndWordId(userId, wordId);

        assertNotNull(result);
        assertEquals(wordId, result.getWordId());

        verify(memoryRepository).findByUserIdAndWordId(userId, wordId);
    }

    @Test
    @DisplayName("获取记忆统计")
    void testGetStatistics() {
        MemoryStatisticsVO expectedStats = MemoryStatisticsVO.builder()
                .userId(userId)
                .totalWords(100)
                .masteredWords(30)
                .learningWords(60)
                .forgottenWords(10)
                .totalReviews(500)
                .correctCount(400)
                .wrongCount(100)
                .accuracyRate(0.8)
                .pendingReviews(20)
                .averageMasteryLevel(55.0)
                .build();

        when(memoryRepository.getStatisticsByUserId(userId)).thenReturn(expectedStats);

        MemoryStatisticsVO result = memoryService.getStatistics(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(100, result.getTotalWords());
        assertEquals(30, result.getMasteredWords());
        assertEquals(0.8, result.getAccuracyRate());

        verify(memoryRepository).getStatisticsByUserId(userId);
    }

    @Test
    @DisplayName("获取用户所有记忆记录")
    void testGetAllRecords() {
        MemoryRecord record2 = new MemoryRecord();
        record2.setId(2L);
        record2.setUserId(userId);
        record2.setWordId(101L);

        when(memoryRepository.findAllByUserId(userId))
                .thenReturn(Arrays.asList(testRecord, record2));

        List<MemoryRecord> result = memoryService.getAllRecords(userId);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(memoryRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("掌握度达到80以上更新为已掌握状态")
    void testSubmitReview_MasteredStatus() {
        testRecord.setMasteryLevel(75);
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(spacedRepetitionEngine.updateMasteryLevel(75, true)).thenReturn(85);
        when(spacedRepetitionEngine.determineStatus(85)).thenReturn("MASTERED");
        when(spacedRepetitionEngine.calculateReviewInterval(eq(85), anyInt(), anyInt())).thenReturn(24);
        when(memoryRepository.update(any(MemoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        MemoryRecord result = memoryService.submitReview(1L, true);

        assertEquals(85, result.getMasteryLevel());
        assertEquals("MASTERED", result.getStatus());

        verify(spacedRepetitionEngine).determineStatus(85);
    }

    @Test
    @DisplayName("连续3次答错重置复习间隔")
    void testSubmitReview_ConsecutiveWrong_ResetsInterval() {
        testRecord.setWrongCount(2);
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(spacedRepetitionEngine.updateMasteryLevel(anyInt(), eq(false))).thenReturn(35);
        when(spacedRepetitionEngine.determineStatus(anyInt())).thenReturn("LEARNING");
        when(spacedRepetitionEngine.calculateReviewInterval(anyInt(), anyInt(), eq(3))).thenReturn(1);
        when(memoryRepository.update(any(MemoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        MemoryRecord result = memoryService.submitReview(1L, false);

        verify(spacedRepetitionEngine).calculateReviewInterval(anyInt(), anyInt(), eq(3));
    }

    @Test
    @DisplayName("获取待复习记录 - 限制数量")
    void testGetDueReviews_WithLimit() {
        MemoryRecord record2 = new MemoryRecord();
        record2.setId(2L);
        record2.setUserId(userId);
        record2.setWordId(101L);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(memoryRepository.findDueReviewsByUserId(eq(userId), any(), eq(2)))
                .thenReturn(Arrays.asList(testRecord, record2));

        List<MemoryRecord> result = memoryService.getDueReviews(userId, 2);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(memoryRepository).findDueReviewsByUserId(eq(userId), any(), eq(2));
    }

    @Test
    @DisplayName("提交复习结果 - 更新下次复习时间")
    void testSubmitReview_UpdatesNextReviewTime() {
        when(memoryRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(spacedRepetitionEngine.updateMasteryLevel(anyInt(), anyBoolean())).thenReturn(55);
        when(spacedRepetitionEngine.determineStatus(anyInt())).thenReturn("LEARNING");
        when(spacedRepetitionEngine.calculateReviewInterval(anyInt(), anyInt(), anyInt())).thenReturn(12);
        when(memoryRepository.update(any(MemoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        LocalDateTime beforeSubmit = LocalDateTime.now();
        MemoryRecord result = memoryService.submitReview(1L, true);
        LocalDateTime afterSubmit = LocalDateTime.now();

        assertNotNull(result.getLastReviewTime());
        assertNotNull(result.getNextReviewTime());
        assertTrue(result.getLastReviewTime().isAfter(beforeSubmit.minusSeconds(1)));
        assertTrue(result.getNextReviewTime().isAfter(afterSubmit));
    }
}
