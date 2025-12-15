package com.llmplatform.controller;

import com.llmplatform.common.Result;
import com.llmplatform.dto.CreateScenarioDTO;
import com.llmplatform.dto.SendMessageDTO;
import com.llmplatform.dto.StartSessionDTO;
import com.llmplatform.service.DialogueService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.AIResponseVO;
import com.llmplatform.vo.DialogueSessionVO;
import com.llmplatform.vo.ScenarioVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dialogue Controller
 * Handles scenario dialogue practice operations
 */
@RestController
@RequestMapping("/api/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final DialogueService dialogueService;
    private final JwtUtil jwtUtil;

    /**
     * Get all available scenarios (preset and user-created)
     * GET /api/dialogue/scenarios
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return list of scenarios
     */
    @GetMapping("/scenarios")
    public Result<List<ScenarioVO>> getScenarios(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<ScenarioVO> scenarios = dialogueService.getScenarios(userId);
        return Result.success(scenarios);
    }

    /**
     * Create a custom scenario
     * POST /api/dialogue/scenarios
     * 
     * @param dto scenario creation data
     * @param authHeader Authorization header containing the Bearer token
     * @return created scenario
     */
    @PostMapping("/scenarios")
    public Result<ScenarioVO> createScenario(
            @Valid @RequestBody CreateScenarioDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        ScenarioVO scenario = dialogueService.createScenario(dto, userId);
        return Result.success(scenario);
    }


    /**
     * Start a new dialogue session
     * POST /api/dialogue/sessions
     * 
     * @param dto session start data containing scenario ID
     * @param authHeader Authorization header containing the Bearer token
     * @return created session
     */
    @PostMapping("/sessions")
    public Result<DialogueSessionVO> startSession(
            @Valid @RequestBody StartSessionDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        DialogueSessionVO session = dialogueService.startSession(dto.getScenarioId(), userId);
        return Result.success(session);
    }

    /**
     * Get a specific dialogue session
     * GET /api/dialogue/sessions/{id}
     * 
     * @param id session ID
     * @param authHeader Authorization header containing the Bearer token
     * @return session details
     */
    @GetMapping("/sessions/{id}")
    public Result<DialogueSessionVO> getSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        DialogueSessionVO session = dialogueService.getSession(id, userId);
        if (session == null) {
            return Result.fail("SESSION_NOT_FOUND", "Dialogue session not found");
        }
        return Result.success(session);
    }

    /**
     * Get all sessions for the current user
     * GET /api/dialogue/sessions
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return list of sessions
     */
    @GetMapping("/sessions")
    public Result<List<DialogueSessionVO>> getUserSessions(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<DialogueSessionVO> sessions = dialogueService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * Send a message in a dialogue session
     * POST /api/dialogue/sessions/{id}/messages
     * 
     * @param id session ID
     * @param dto message data
     * @param authHeader Authorization header containing the Bearer token
     * @return AI response
     */
    @PostMapping("/sessions/{id}/messages")
    public Result<AIResponseVO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        AIResponseVO response = dialogueService.sendMessage(id, dto.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * End a dialogue session
     * DELETE /api/dialogue/sessions/{id}
     * 
     * @param id session ID
     * @param authHeader Authorization header containing the Bearer token
     * @return success result
     */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> endSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        dialogueService.endSession(id, userId);
        return Result.success(null);
    }

    /**
     * Extract user ID from Authorization header
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.getUserIdFromToken(token);
    }
}
