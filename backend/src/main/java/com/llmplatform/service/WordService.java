package com.llmplatform.service;

import com.llmplatform.vo.WordHistoryVO;
import com.llmplatform.vo.WordVO;

import java.util.List;

/**
 * Word query service interface
 */
public interface WordService {

    /**
     * Query a word with translation
     * First checks cache, then database, then generates via AI if not found
     * 
     * @param word the word to query
     * @param sourceLang source language code
     * @param targetLang target language code
     * @param userId user ID for recording history
     * @return word information with translation
     */
    WordVO query(String word, String sourceLang, String targetLang, Long userId);

    /**
     * Get word query history for a user
     * 
     * @param userId user ID
     * @return list of word query history items
     */
    List<WordHistoryVO> getHistory(Long userId);

    /**
     * Get word by ID
     * 
     * @param wordId word ID
     * @return word VO or null if not found
     */
    WordVO getWordById(Long wordId);
}
