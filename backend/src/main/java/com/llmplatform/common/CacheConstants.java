package com.llmplatform.common;

/**
 * Cache key constants for Redis operations
 */
public final class CacheConstants {

    private CacheConstants() {
        // Prevent instantiation
    }

    // Key prefixes
    public static final String WORD_PREFIX = "word:";
    public static final String SESSION_PREFIX = "session:";
    public static final String DIALOGUE_PREFIX = "dialogue:session:";
    public static final String STATS_PREFIX = "stats:user:";

    // TTL values in seconds
    public static final long WORD_TTL_SECONDS = 7 * 24 * 60 * 60;  // 7 days
    public static final long SESSION_TTL_SECONDS = 24 * 60 * 60;    // 24 hours
    public static final long DIALOGUE_TTL_SECONDS = 2 * 60 * 60;    // 2 hours
    public static final long STATS_TTL_SECONDS = 60 * 60;           // 1 hour

    /**
     * Generate word cache key
     * Format: word:{sourceLang}:{targetLang}:{word}
     */
    public static String wordKey(String sourceLang, String targetLang, String word) {
        return WORD_PREFIX + sourceLang + ":" + targetLang + ":" + word.toLowerCase();
    }

    /**
     * Generate session cache key
     * Format: session:{token}
     */
    public static String sessionKey(String token) {
        return SESSION_PREFIX + token;
    }

    /**
     * Generate dialogue context cache key
     * Format: dialogue:session:{sessionId}
     */
    public static String dialogueKey(Long sessionId) {
        return DIALOGUE_PREFIX + sessionId;
    }

    /**
     * Generate user statistics cache key
     * Format: stats:user:{userId}
     */
    public static String statsKey(Long userId) {
        return STATS_PREFIX + userId;
    }
}
