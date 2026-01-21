package com.llmplatform.personalized.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.llmplatform.common.Result;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.service.MemoryService;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 记忆记录控制器
 * 提供记忆记录的REST API接口
 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController extends BaseController {

    private final MemoryService memoryService;

    /**
     * 创建记忆记录
     * POST /api/memory/records
     */
    @PostMapping("/records")
    public Result<MemoryRecord> createRecord(@RequestBody Map<String, Long> request) {
        Long wordId = request.get("wordId");
        if (wordId == null) {
            return Result.fail("wordId 不能为空");
        }

        MemoryRecord record = memoryService.createOrGetRecord(requireUserId(), wordId);
        return Result.success(record);
    }

    /**
     * 获取记忆记录
     * GET /api/memory/records/{id}
     */
    @GetMapping("/records/{id}")
    public Result<MemoryRecord> getRecord(@PathVariable Long id) {
        MemoryRecord record = memoryService.getRecord(id);
        return Result.success(record);
    }

    /**
     * 获取用户的所有记忆记录
     * GET /api/memory/records/user/{userId}
     */
    @GetMapping("/records/user/{userId}")
    public Result<List<MemoryRecord>> getRecordsByUserId(@PathVariable Long userId) {
        List<MemoryRecord> records = memoryService.getAllRecords(userId);
        return Result.success(records);
    }

    /**
     * 提交复习结果
     * PUT /api/memory/records/{id}/review
     */
    @PutMapping("/records/{id}/review")
    public Result<MemoryRecord> submitReview(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        Boolean isCorrect = request.get("correct");
        if (isCorrect == null) {
            return Result.fail("correct 字段不能为空");
        }

        MemoryRecord record = memoryService.submitReview(id, isCorrect);
        return Result.success(record);
    }

    /**
     * 获取待复习记录列表
     * GET /api/memory/due-reviews
     */
    @GetMapping("/due-reviews")
    public Result<List<MemoryRecord>> getDueReviews(
            @RequestParam(defaultValue = "10") int limit) {

        List<MemoryRecord> records = memoryService.getDueReviews(requireUserId(), limit);
        return Result.success(records);
    }

    /**
     * 获取待复习记录分页
     * GET /api/memory/due-reviews/paged
     */
    @GetMapping("/due-reviews/paged")
    public Result<Page<MemoryRecord>> getDueReviewsPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<MemoryRecord> records = memoryService.getDueReviewsPage(requireUserId(), page, size);
        return Result.success(records);
    }

    /**
     * 获取记忆统计信息
     * GET /api/memory/statistics
     */
    @GetMapping("/statistics")
    public Result<MemoryStatisticsVO> getStatistics() {
        MemoryStatisticsVO statistics = memoryService.getStatistics(requireUserId());
        return Result.success(statistics);
    }
}
