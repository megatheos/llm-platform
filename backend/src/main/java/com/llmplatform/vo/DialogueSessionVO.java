package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * View object for DialogueSession
 */
@Data
public class DialogueSessionVO {

    private Long id;

    private Long userId;

    private Long scenarioId;

    private String scenarioName;

    private List<MessageVO> messages;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /**
     * Inner class for message representation
     */
    @Data
    public static class MessageVO {
        private String role;  // "user" or "assistant"
        private String content;
        private LocalDateTime timestamp;
    }
}
