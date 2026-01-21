package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.LearningProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习档案数据访问接口
 * 继承MyBatis-Plus BaseMapper提供基础CRUD操作
 */
@Mapper
public interface LearningProfileMapper extends BaseMapper<LearningProfile> {
}
