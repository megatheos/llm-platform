package com.llmplatform.service;

import com.llmplatform.dto.CreateScenarioDTO;
import com.llmplatform.vo.AIResponseVO;
import com.llmplatform.vo.DialogueSessionVO;
import com.llmplatform.vo.ScenarioVO;

import java.util.List;

/**
 * Dialogue service interface for scenario dialogue practice
 */
public interface DialogueService {

    /**
     * Get all available scenarios (preset and user-created)
     * 
     * @param userId user ID to include user's custom scenarios
     * @return list of scenarios
     */
    List<ScenarioVO> getScenarios(Long userId);

    /**
     * Create a custom scenario
     * 
     * @param dto scenario creation data
     * @param userId creator user ID
     * @return created scenario
     */
    ScenarioVO createScenario(CreateScenarioDTO dto, Long userId);

    /**
     * Get a scenario by ID
     * 
     * @param scenarioId scenario ID
     * @return scenario or null if not found
     */
    ScenarioVO getScenarioById(Long scenarioId);

    /**
     * Start a new dialogue session
     * 
     * @param scenarioId scenario ID
     * @param userId user ID
     * @return created session
     */
    DialogueSessionVO startSession(Long scenarioId, Long userId);

    /**
     * Send a message in a dialogue session
     * 
     * @param sessionId session ID
     * @param message user message
     * @param userId user ID for validation
     * @return AI response
     */
    AIResponseVO sendMessage(Long sessionId, String message, Long userId);

    /**
     * End a dialogue session
     * 
     * @param sessionId session ID
     * @param userId user ID for validation
     */
    void endSession(Long sessionId, Long userId);

    /**
     * Get a dialogue session by ID
     * 
     * @param sessionId session ID
     * @param userId user ID for validation
     * @return session or null if not found
     */
    DialogueSessionVO getSession(Long sessionId, Long userId);

    /**
     * Get all sessions for a user
     * 
     * @param userId user ID
     * @return list of sessions
     */
    List<DialogueSessionVO> getUserSessions(Long userId);
}
