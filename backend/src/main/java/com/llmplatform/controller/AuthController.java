package com.llmplatform.controller;

import com.llmplatform.common.Result;
import com.llmplatform.dto.LoginDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.service.UserService;
import com.llmplatform.vo.LoginVO;
import com.llmplatform.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user registration, login, and logout operations
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Register a new user
     * POST /api/auth/register
     * 
     * @param dto registration data containing username, password, and email
     * @return created user information
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO user = userService.register(dto);
        return Result.success(user);
    }

    /**
     * User login
     * POST /api/auth/login
     * 
     * @param dto login credentials containing username and password
     * @return login result with JWT token and user information
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success(loginVO);
    }

    /**
     * Get current user information
     * GET /api/auth/me
     * 
     * @return current user information
     */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        UserVO user = userService.getCurrentUser();
        return Result.success(user);
    }

    /**
     * User logout - invalidates the current session token
     * POST /api/auth/logout
     * 
     * @param authHeader Authorization header containing the Bearer token
     * @return success result
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        userService.logout(token);
        return Result.success();
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
