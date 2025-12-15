package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for a single learning record
 */
@Data
public class LearningRecordVO {

    private Long id;

    private String activityType;

    private Long activityId;

    private LocalDateTime activityTime;

    /**
     * Activity-specific details (word info, dialogue info, or quiz info)
     */
    private Object activityDetails;
}
