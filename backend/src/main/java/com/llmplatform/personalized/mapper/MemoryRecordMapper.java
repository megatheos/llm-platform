package com.llmplatform.personalized.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llmplatform.personalized.entity.MemoryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 记忆记录数据访问接口
 * 继承MyBatis-Plus BaseMapper提供基础CRUD操作
 */
@Mapper
public interface MemoryRecordMapper extends BaseMapper<MemoryRecord> {
}
