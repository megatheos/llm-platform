package com.llmplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word")
public class Word {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String word;
    
    private String sourceLang;
    
    private String targetLang;
    
    private String definition;
    
    private String translation;
    
    private String examples;
    
    private String pronunciation;
    
    private LocalDateTime createdAt;
}
