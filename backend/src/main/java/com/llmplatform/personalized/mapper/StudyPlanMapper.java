package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习计划数据访问接口
 */
@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {
}
