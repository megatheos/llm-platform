package com.llmplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dialogue_session")
public class DialogueSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long scenarioId;
    
    private String targetLang;  // Target language for the dialogue
    
    private String messages;  // JSON format for conversation history
    
    private LocalDateTime startedAt;
    
    private LocalDateTime endedAt;
}
