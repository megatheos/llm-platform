package com.llmplatform.personalized.unit;

import com.llmplatform.entity.LearningRecord;
import com.llmplatform.personalized.engine.LearningAnalyticsEngine;
import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.repository.ProfileRepository;
import com.llmplatform.personalized.service.impl.ProfileAnalysisServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProfileAnalysisService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileAnalysisServiceUnitTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private LearningAnalyticsEngine learningAnalyticsEngine;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ProfileAnalysisServiceImpl profileAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private LearningProfile testProfile;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        testProfile = new LearningProfile();
        testProfile.setId(1L);
        testProfile.setUserId(userId);
        testProfile.setPreferredLearningTimes("{\"morning\": 0.3, \"afternoon\": 0.5, \"evening\": 0.2}");
        testProfile.setWeakAreas("[]");
        testProfile.setAverageDailyWords(25.0);
        testProfile.setAverageAccuracy(0.8);
        testProfile.setLearningSpeedTrend("NORMAL");
        testProfile.setCreatedTime(LocalDateTime.now().minusDays(10));
        testProfile.setUpdatedTime(LocalDateTime.now().minusDays(1));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        profileAnalysisService = new ProfileAnalysisServiceImpl(
                profileRepository,
                learningAnalyticsEngine,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    @DisplayName("获取或创建档案 - 档案存在")
    void testGetOrCreateProfile_Exists_ReturnsFromCache() {
        when(valueOperations.get(anyString())).thenReturn(testProfile);

        LearningProfile result = profileAnalysisService.getOrCreateProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(profileRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("获取或创建档案 - 缓存未命中从数据库获取")
    void testGetOrCreateProfile_CacheMiss_FetchesFromDatabase() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        LearningProfile result = profileAnalysisService.getOrCreateProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(profileRepository).findByUserId(userId);
        verify(valueOperations).set(anyString(), eq(testProfile), any());
    }

    @Test
    @DisplayName("获取或创建档案 - 档案不存在创建默认")
    void testGetOrCreateProfile_NotExists_CreatesDefault() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.create(any(LearningProfile.class))).thenAnswer(invocation -> {
            LearningProfile profile = invocation.getArgument(0);
            profile.setId(2L);
            return profile;
        });

        LearningProfile result = profileAnalysisService.getOrCreateProfile(userId);

        assertNotNull(result);
        assertEquals(0.0, result.getAverageDailyWords());
        assertEquals("NORMAL", result.getLearningSpeedTrend());
        verify(profileRepository).create(any(LearningProfile.class));
    }

    @Test
    @DisplayName("更新档案 - 分析学习数据")
    void testUpdateProfile_AnalyzesLearningData() {
        List<LearningRecord> records = createTestRecords(10);
        Map<String, Double> timePreferences = Map.of("morning", 0.3, "afternoon", 0.5, "evening", 0.2);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(learningAnalyticsEngine.analyzeTimePreferences(records)).thenReturn(timePreferences);
        when(learningAnalyticsEngine.calculateLearningSpeed(records)).thenReturn(30.0);
        when(learningAnalyticsEngine.identifyWeakAreas(records)).thenReturn(Collections.emptyList());
        when(profileRepository.update(any(LearningProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningProfile result = profileAnalysisService.updateProfile(userId, records);

        assertNotNull(result);
        verify(learningAnalyticsEngine).analyzeTimePreferences(records);
        verify(learningAnalyticsEngine).calculateLearningSpeed(records);
        verify(profileRepository).update(any(LearningProfile.class));
    }

    @Test
    @DisplayName("获取档案 - 返回档案")
    void testGetProfile_Exists_ReturnsProfile() {
        when(valueOperations.get(anyString())).thenReturn(testProfile);

        LearningProfile result = profileAnalysisService.getProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    @DisplayName("获取档案 - 不存在返回null")
    void testGetProfile_NotExists_ReturnsNull() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        LearningProfile result = profileAnalysisService.getProfile(userId);

        assertNull(result);
    }

    @Test
    @DisplayName("生成洞察报告 - 数据充足")
    void testGenerateInsightReport_DataSufficient() {
        testProfile.setLastAnalysisTime(LocalDateTime.now());
        testProfile.setAverageDailyWords(30.0);
        testProfile.setAverageAccuracy(0.85);

        when(valueOperations.get(anyString())).thenReturn(testProfile);

        Map<String, Object> report = profileAnalysisService.generateInsightReport(userId);

        assertNotNull(report);
        assertFalse(report.containsKey("error"));
        assertTrue(report.containsKey("timePreferences"));
        assertTrue(report.containsKey("weakAreas"));
        assertTrue(report.containsKey("averageDailyWords"));
    }

    @Test
    @DisplayName("生成洞察报告 - 数据不足警告")
    void testGenerateInsightReport_DataInsufficient_Warns() {
        testProfile.setLastAnalysisTime(LocalDateTime.now().minusDays(10)); // 10天前

        when(valueOperations.get(anyString())).thenReturn(testProfile);

        Map<String, Object> report = profileAnalysisService.generateInsightReport(userId);

        assertTrue(report.containsKey("warning"));
        assertEquals(7, report.get("daysRequired"));
    }

    @Test
    @DisplayName("分析时间偏好")
    void testAnalyzeTimePreferences() {
        when(valueOperations.get(anyString())).thenReturn(testProfile);

        Map<String, Double> result = profileAnalysisService.analyzeTimePreferences(userId);

        assertNotNull(result);
        assertEquals(0.3, result.get("morning"));
        assertEquals(0.5, result.get("afternoon"));
        assertEquals(0.2, result.get("evening"));
    }

    @Test
    @DisplayName("识别薄弱领域")
    void testIdentifyWeakAreas() {
        when(valueOperations.get(anyString())).thenReturn(testProfile);

        List<Map<String, Object>> result = profileAnalysisService.identifyWeakAreas(userId);

        assertNotNull(result);
    }

    @Test
    @DisplayName("获取学习速度趋势")
    void testGetLearningSpeedTrend() {
        when(valueOperations.get(anyString())).thenReturn(testProfile);

        String result = profileAnalysisService.getLearningSpeedTrend(userId);

        assertEquals("NORMAL", result);
    }

    @Test
    @DisplayName("学习速度趋势判断 - FAST")
    void testDetermineSpeedTrend_Fast() {
        testProfile.setAverageDailyWords(60.0);

        when(valueOperations.get(anyString())).thenReturn(testProfile);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(learningAnalyticsEngine.analyzeTimePreferences(any())).thenReturn(Map.of());
        when(learningAnalyticsEngine.calculateLearningSpeed(any())).thenReturn(60.0);
        when(learningAnalyticsEngine.identifyWeakAreas(any())).thenReturn(Collections.emptyList());
        when(profileRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        LearningProfile result = profileAnalysisService.updateProfile(userId, List.of());

        assertEquals("FAST", result.getLearningSpeedTrend());
    }

    @Test
    @DisplayName("学习速度趋势判断 - SLOW")
    void testDetermineSpeedTrend_Slow() {
        testProfile.setAverageDailyWords(10.0);

        when(valueOperations.get(anyString())).thenReturn(testProfile);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(learningAnalyticsEngine.analyzeTimePreferences(any())).thenReturn(Map.of());
        when(learningAnalyticsEngine.calculateLearningSpeed(any())).thenReturn(10.0);
        when(learningAnalyticsEngine.identifyWeakAreas(any())).thenReturn(Collections.emptyList());
        when(profileRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        LearningProfile result = profileAnalysisService.updateProfile(userId, List.of());

        assertEquals("SLOW", result.getLearningSpeedTrend());
    }

    /**
     * 创建测试用的学习记录列表
     */
    private List<LearningRecord> createTestRecords(int count) {
        List<LearningRecord> records = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            LearningRecord record = new LearningRecord();
            record.setId((long) (i + 1));
            record.setUserId(userId);
            record.setActivityType("WORD_QUERY");
            record.setActivityTime(baseTime.minusHours(i));
            records.add(record);
        }

        return records;
    }
}
