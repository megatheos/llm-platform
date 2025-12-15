package com.llmplatform.vo;

import lombok.Data;

/**
 * View object for AI response in dialogue
 */
@Data
public class AIResponseVO {

    private String content;

    private String role;

    private Long sessionId;
}
