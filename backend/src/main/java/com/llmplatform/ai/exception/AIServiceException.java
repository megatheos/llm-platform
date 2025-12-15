package com.llmplatform.ai.exception;

import lombok.Getter;

/**
 * Exception thrown when AI service operations fail
 */
@Getter
public class AIServiceException extends RuntimeException {

    private final String provider;
    private final String originalMessage;

    public AIServiceException(String message) {
        super(message);
        this.provider = "unknown";
        this.originalMessage = message;
    }

    public AIServiceException(String provider, String message) {
        super(String.format("[%s] %s", provider, message));
        this.provider = provider;
        this.originalMessage = message;
    }

    public AIServiceException(String provider, String message, Throwable cause) {
        super(String.format("[%s] %s", provider, message), cause);
        this.provider = provider;
        this.originalMessage = message;
    }
}
