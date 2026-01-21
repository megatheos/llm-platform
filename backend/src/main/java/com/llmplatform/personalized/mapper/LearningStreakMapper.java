package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.LearningStreak;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习连续记录数据访问接口
 */
@Mapper
public interface LearningStreakMapper extends BaseMapper<LearningStreak> {
}
