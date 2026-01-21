package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就实体
 * 定义系统中的成就徽章
 */
@Data
@TableName("achievements")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 成就代码: STREAK_7, WORDS_100, STREAK_30等
     */
    private String code;

    /**
     * 成就名称
     */
    private String name;

    /**
     * 成就描述
     */
    private String description;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * 类别: STREAK, MILESTONE, MASTERY
     */
    private String category;

    /**
     * 所需值 (如连续天数、词汇数等)
     */
    private Integer requiredValue;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
