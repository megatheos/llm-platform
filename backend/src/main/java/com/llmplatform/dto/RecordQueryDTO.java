package com.llmplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for querying learning records with filters
 */
@Data
public class RecordQueryDTO {

    /**
     * Filter by activity type (WORD_QUERY, DIALOGUE, QUIZ)
     */
    private String activityType;

    /**
     * Filter records from this date
     */
    private LocalDateTime startDate;

    /**
     * Filter records until this date
     */
    private LocalDateTime endDate;

    /**
     * Page number (1-based)
     */
    private Integer page = 1;

    /**
     * Page size
     */
    private Integer pageSize = 20;
}
