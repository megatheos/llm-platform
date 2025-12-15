package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for word query results
 */
@Data
public class WordVO {

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
