package com.llmplatform.service;

import com.llmplatform.dto.LoginDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.User;
import com.llmplatform.vo.LoginVO;
import com.llmplatform.vo.UserVO;

public interface UserService {
    
    /**
     * Register a new user
     * @param dto registration data
     * @return created user info
     */
    UserVO register(RegisterDTO dto);
    
    /**
     * User login
     * @param dto login credentials
     * @return login result with token
     */
    LoginVO login(LoginDTO dto);
    
    /**
     * User logout - invalidate token
     * @param token JWT token to invalidate
     */
    void logout(String token);
    
    /**
     * Get user by username
     * @param username username to search
     * @return user entity or null
     */
    User getByUsername(String username);
    
    /**
     * Get user by ID
     * @param userId user ID
     * @return user VO or null
     */
    UserVO getUserById(Long userId);
    
    /**
     * Check if token is valid (not blacklisted)
     * @param token JWT token
     * @return true if valid
     */
    boolean isTokenValid(String token);
}
