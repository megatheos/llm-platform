package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for word query history items
 */
@Data
public class WordHistoryVO {

    private Long id;
    
    private Long wordId;
    
    private String word;
    
    private String sourceLang;
    
    private String targetLang;
    
    private String translation;
    
    private LocalDateTime queryTime;
}
