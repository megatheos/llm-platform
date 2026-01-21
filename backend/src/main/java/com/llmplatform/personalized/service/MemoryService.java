package com.llmplatform.personalized.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆记录服务接口
 * 提供记忆记录的业务逻辑操作
 */
public interface MemoryService {

    /**
     * 创建记忆记录
     *
     * @param userId 用户ID
     * @param wordId 词汇ID
     * @return 创建的记忆记录
     */
    MemoryRecord createRecord(Long userId, Long wordId);

    /**
     * 创建或获取已有的记忆记录
     *
     * @param userId 用户ID
     * @param wordId 词汇ID
     * @return 记忆记录
     */
    MemoryRecord createOrGetRecord(Long userId, Long wordId);

    /**
     * 提交复习结果并更新记录
     *
     * @param recordId 记录ID
     * @param isCorrect 是否答对
     * @return 更新后的记忆记录
     */
    MemoryRecord submitReview(Long recordId, boolean isCorrect);

    /**
     * 获取用户的待复习记录列表
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 待复习记录列表
     */
    List<MemoryRecord> getDueReviews(Long userId, int limit);

    /**
     * 获取用户的待复习记录分页
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 待复习记录分页
     */
    Page<MemoryRecord> getDueReviewsPage(Long userId, int page, int size);

    /**
     * 获取记忆记录
     *
     * @param recordId 记录ID
     * @return 记忆记录
     */
    MemoryRecord getRecord(Long recordId);

    /**
     * 根据用户ID和词汇ID获取记忆记录
     *
     * @param userId 用户ID
     * @param wordId 词汇ID
     * @return 记忆记录
     */
    MemoryRecord getRecordByUserIdAndWordId(Long userId, Long wordId);

    /**
     * 获取用户的记忆统计
     *
     * @param userId 用户ID
     * @return 记忆统计信息
     */
    MemoryStatisticsVO getStatistics(Long userId);

    /**
     * 获取用户的所有记忆记录
     *
     * @param userId 用户ID
     * @return 记忆记录列表
     */
    List<MemoryRecord> getAllRecords(Long userId);
}
