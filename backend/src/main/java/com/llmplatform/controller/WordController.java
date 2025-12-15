package com.llmplatform.controller;

import com.llmplatform.common.Result;
import com.llmplatform.dto.WordQueryDTO;
import com.llmplatform.service.WordService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.WordHistoryVO;
import com.llmplatform.vo.WordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Word Query Controller
 * Handles word query and history operations
 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;
    private final JwtUtil jwtUtil;

    /**
     * Query a word with translation
     * POST /api/words/query
     * 
     * @param dto word query data containing word, source language, and target language
     * @param authHeader Authorization header containing the Bearer token
     * @return word information with definition and translation
     */
    @PostMapping("/query")
    public Result<WordVO> query(@Valid @RequestBody WordQueryDTO dto,
                                @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        WordVO wordVO = wordService.query(dto.getWord(), dto.getSourceLang(), 
                dto.getTargetLang(), userId);
        return Result.success(wordVO);
    }

    /**
     * Get word query history for the current user
     * GET /api/words/history
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return list of word query history items
     */
    @GetMapping("/history")
    public Result<List<WordHistoryVO>> getHistory(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<WordHistoryVO> history = wordService.getHistory(userId);
        return Result.success(history);
    }

    /**
     * Extract user ID from Authorization header
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.getUserIdFromToken(token);
    }
}
