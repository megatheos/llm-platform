package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.UserAchievement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户成就数据访问接口
 */
@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {
}
