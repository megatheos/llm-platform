package com.llmplatform.personalized.engine;

import com.llmplatform.entity.LearningRecord;

import java.util.List;
import java.util.Map;

/**
 * 学习分析引擎接口
 * 分析用户的学习行为和表现
 */
public interface LearningAnalyticsEngine {

    /**
     * 分析学习时间偏好
     *
     * @param records 最近的学习记录
     * @return 时间段分布 {"morning": 0.3, "afternoon": 0.5, "evening": 0.2}
     */
    Map<String, Double> analyzeTimePreferences(List<LearningRecord> records);

    /**
     * 识别薄弱领域
     *
     * @param records 学习记录
     * @return 薄弱领域列表（错误率>40%）
     */
    List<WeakArea> identifyWeakAreas(List<LearningRecord> records);

    /**
     * 计算学习速度
     *
     * @param records 最近30天的记录
     * @return 平均每日学习词汇数
     */
    double calculateLearningSpeed(List<LearningRecord> records);

    /**
     * 生成进度曲线数据
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 进度曲线数据
     */
    ProgressCurve generateProgressCurve(Long userId, int days);

    /**
     * 薄弱领域内部类
     */
    record WeakArea(String type, double errorRate) {}

    /**
     * 进度曲线数据内部类
     */
    record ProgressCurve(
            List<String> dates,
            List<Integer> cumulativeWords,
            List<Integer> masteredWords,
            List<Double> accuracyRates
    ) {}
}
