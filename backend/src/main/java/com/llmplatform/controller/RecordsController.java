package com.llmplatform.controller;

import com.llmplatform.common.Result;
import com.llmplatform.dto.RecordQueryDTO;
import com.llmplatform.service.LearningRecordService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.LearningRecordsVO;
import com.llmplatform.vo.LearningStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Learning Records Controller
 * Handles learning record retrieval and statistics operations
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordsController {

    private final LearningRecordService learningRecordService;
    private final JwtUtil jwtUtil;

    /**
     * Get learning records for the current user
     * GET /api/records
     * 
     * @param activityType optional filter by activity type (WORD_QUERY, DIALOGUE, QUIZ)
     * @param startDate optional filter for records from this date
     * @param endDate optional filter for records until this date
     * @param page page number (1-based, default 1)
     * @param pageSize page size (default 20)
     * @param authHeader Authorization header containing the Bearer token
     * @return paginated learning records
     */
    @GetMapping
    public Result<LearningRecordsVO> getRecords(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserId(authHeader);
        
        RecordQueryDTO params = new RecordQueryDTO();
        params.setActivityType(activityType);
        params.setStartDate(startDate);
        params.setEndDate(endDate);
        params.setPage(page);
        params.setPageSize(pageSize);
        
        LearningRecordsVO records = learningRecordService.getRecords(userId, params);
        return Result.success(records);
    }

    /**
     * Get learning statistics for the current user
     * GET /api/records/statistics
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return learning statistics including totals and averages
     */
    @GetMapping("/statistics")
    public Result<LearningStatisticsVO> getStatistics(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        LearningStatisticsVO statistics = learningRecordService.getStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * Extract user ID from Authorization header
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.getUserIdFromToken(token);
    }
}
