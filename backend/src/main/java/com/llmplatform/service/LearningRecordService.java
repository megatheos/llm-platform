package com.llmplatform.service;

import com.llmplatform.dto.RecordQueryDTO;
import com.llmplatform.entity.DialogueSession;
import com.llmplatform.entity.Quiz;
import com.llmplatform.vo.LearningRecordsVO;
import com.llmplatform.vo.LearningStatisticsVO;

/**
 * Learning record service interface
 * Tracks and manages user learning activities
 */
public interface LearningRecordService {

    /**
     * Get learning records for a user with optional filtering
     *
     * @param userId user ID
     * @param params query parameters for filtering and pagination
     * @return paginated learning records
     */
    LearningRecordsVO getRecords(Long userId, RecordQueryDTO params);

    /**
     * Get learning statistics for a user
     *
     * @param userId user ID
     * @return learning statistics
     */
    LearningStatisticsVO getStatistics(Long userId);

    /**
     * Record a word query activity
     *
     * @param userId user ID
     * @param wordId word ID
     */
    void recordWordQuery(Long userId, Long wordId);

    /**
     * Record a dialogue session activity
     *
     * @param userId user ID
     * @param session dialogue session
     */
    void recordDialogueSession(Long userId, DialogueSession session);

    /**
     * Record a quiz result activity
     *
     * @param userId user ID
     * @param quiz completed quiz
     */
    void recordQuizResult(Long userId, Quiz quiz);
}
