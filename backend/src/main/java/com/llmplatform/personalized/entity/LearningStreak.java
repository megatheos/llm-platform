package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习连续记录实体
 * 跟踪用户的连续学习天数
 */
@Data
@TableName("learning_streaks")
public class LearningStreak {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 当前连续天数
     */
    private Integer currentStreak;

    /**
     * 历史最长连续天数
     */
    private Integer longestStreak;

    /**
     * 最后学习日期
     */
    private LocalDate lastLearningDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
