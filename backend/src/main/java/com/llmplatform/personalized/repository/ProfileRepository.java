package com.llmplatform.personalized.repository;

import com.llmplatform.personalized.entity.LearningProfile;

import java.util.Optional;

/**
 * 学习档案数据访问仓库接口
 */
public interface ProfileRepository {

    /**
     * 创建学习档案
     *
     * @param profile 学习档案
     * @return 创建后的档案
     */
    LearningProfile create(LearningProfile profile);

    /**
     * 更新学习档案
     *
     * @param profile 学习档案
     * @return 更新后的档案
     */
    LearningProfile update(LearningProfile profile);

    /**
     * 根据ID获取学习档案
     *
     * @param id 档案ID
     * @return 学习档案
     */
    Optional<LearningProfile> findById(Long id);

    /**
     * 根据用户ID获取学习档案
     *
     * @param userId 用户ID
     * @return 学习档案
     */
    Optional<LearningProfile> findByUserId(Long userId);

    /**
     * 检查用户是否有学习档案
     *
     * @param userId 用户ID
     * @return true if profile exists
     */
    boolean existsByUserId(Long userId);

    /**
     * 删除学习档案
     *
     * @param id 档案ID
     * @return true if deleted
     */
    boolean deleteById(Long id);

    /**
     * 删除用户的学习档案
     *
     * @param userId 用户ID
     * @return true if deleted
     */
    boolean deleteByUserId(Long userId);
}
