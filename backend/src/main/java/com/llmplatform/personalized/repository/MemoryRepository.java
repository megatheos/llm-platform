package com.llmplatform.personalized.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.llmplatform.personalized.entity.MemoryRecord;
import com.llmplatform.personalized.vo.MemoryStatisticsVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记忆记录数据访问仓库接口
 */
public interface MemoryRepository {

    /**
     * 创建记忆记录
     *
     * @param record 记忆记录
     * @return 创建后的记录
     */
    MemoryRecord create(MemoryRecord record);

    /**
     * 更新记忆记录
     *
     * @param record 记忆记录
     * @return 更新后的记录
     */
    MemoryRecord update(MemoryRecord record);

    /**
     * 根据ID获取记忆记录
     *
     * @param id 记录ID
     * @return 记忆记录
     */
    Optional<MemoryRecord> findById(Long id);

    /**
     * 根据用户ID和词汇ID获取记忆记录
     *
     * @param userId 用户ID
     * @param wordId 词汇ID
     * @return 记忆记录
     */
    Optional<MemoryRecord> findByUserIdAndWordId(Long userId, Long wordId);

    /**
     * 获取用户的待复习记录列表（按优先级排序）
     * 优先级：复习时间已到的记录优先，在复习时间已到的记录中，掌握度较低的优先
     *
     * @param userId 用户ID
     * @param currentTime 当前时间
     * @param limit 限制数量
     * @return 待复习记录列表
     */
    List<MemoryRecord> findDueReviewsByUserId(Long userId, LocalDateTime currentTime, int limit);

    /**
     * 获取用户的待复习记录分页（按优先级排序）
     *
     * @param userId 用户ID
     * @param currentTime 当前时间
     * @param page 页码
     * @param size 每页大小
     * @return 待复习记录分页
     */
    Page<MemoryRecord> findDueReviewsPageByUserId(Long userId, LocalDateTime currentTime, int page, int size);

    /**
     * 获取用户的所有记忆记录
     *
     * @param userId 用户ID
     * @return 记忆记录列表
     */
    List<MemoryRecord> findAllByUserId(Long userId);

    /**
     * 获取用户的记忆统计
     *
     * @param userId 用户ID
     * @return 记忆统计VO
     */
    MemoryStatisticsVO getStatisticsByUserId(Long userId);

    /**
     * 获取用户已掌握的词汇数量
     *
     * @param userId 用户ID
     * @return 已掌握数量
     */
    long countMasteredByUserId(Long userId);

    /**
     * 获取用户总学习词汇数
     *
     * @param userId 用户ID
     * @return 总词汇数
     */
    long countTotalByUserId(Long userId);

    /**
     * 删除用户的记忆记录
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    int deleteByUserId(Long userId);
}
