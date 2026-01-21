package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.DailyTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日任务数据访问接口
 */
@Mapper
public interface DailyTaskMapper extends BaseMapper<DailyTask> {
}
