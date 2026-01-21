package com.llmplatform.personalized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就实体
 * 存储用户已解锁的成就
 */
@Data
@TableName("user_achievements")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long achievementId;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockedTime;

    /**
     * 是否已通知
     */
    private Boolean isNotified;
}
