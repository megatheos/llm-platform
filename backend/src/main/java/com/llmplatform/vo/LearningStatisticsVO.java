package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for learning statistics
 */
@Data
public class LearningStatisticsVO {

    /**
     * Total number of words queried
     */
    private Long totalWordQueries;

    /**
     * Total number of dialogue sessions
     */
    private Long totalDialogueSessions;

    /**
     * Total number of quizzes taken
     */
    private Long totalQuizzes;

    /**
     * Average quiz score (percentage)
     */
    private Double averageQuizScore;

    /**
     * Total learning activities
     */
    private Long totalActivities;

    /**
     * Date of first activity
     */
    private LocalDateTime firstActivityDate;

    /**
     * Date of most recent activity
     */
    private LocalDateTime lastActivityDate;

    /**
     * Number of activities in the last 7 days
     */
    private Long activitiesLast7Days;

    /**
     * Number of activities in the last 30 days
     */
    private Long activitiesLast30Days;
}
