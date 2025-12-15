package com.llmplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz")
public class Quiz {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String difficulty;
    
    private String targetLang;
    
    private String questions;  // JSON format for quiz questions
    
    private Integer totalScore;
    
    private Integer userScore;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;
}
