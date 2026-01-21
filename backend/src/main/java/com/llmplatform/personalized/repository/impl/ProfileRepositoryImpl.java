package com.llmplatform.personalized.repository.impl;

import com.llmplatform.personalized.entity.LearningProfile;
import com.llmplatform.personalized.mapper.LearningProfileMapper;
import com.llmplatform.personalized.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 学习档案数据访问仓库实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private static final String TABLE_NAME = "learning_profiles";

    private final LearningProfileMapper learningProfileMapper;

    @Override
    public LearningProfile create(LearningProfile profile) {
        profile.setCreatedTime(LocalDateTime.now());
        profile.setUpdatedTime(LocalDateTime.now());
        learningProfileMapper.insert(profile);
        log.debug("创建学习档案成功, userId={}", profile.getUserId());
        return profile;
    }

    @Override
    public LearningProfile update(LearningProfile profile) {
        profile.setUpdatedTime(LocalDateTime.now());
        learningProfileMapper.updateById(profile);
        log.debug("更新学习档案成功, userId={}", profile.getUserId());
        return profile;
    }

    @Override
    public Optional<LearningProfile> findById(Long id) {
        LearningProfile profile = learningProfileMapper.selectById(id);
        return Optional.ofNullable(profile);
    }

    @Override
    public Optional<LearningProfile> findByUserId(Long userId) {
        LearningProfile profile = learningProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningProfile>()
                        .eq(LearningProfile::getUserId, userId)
        );
        return Optional.ofNullable(profile);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return learningProfileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningProfile>()
                        .eq(LearningProfile::getUserId, userId)
        ) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        int result = learningProfileMapper.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        int result = learningProfileMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningProfile>()
                        .eq(LearningProfile::getUserId, userId)
        );
        return result > 0;
    }
}
