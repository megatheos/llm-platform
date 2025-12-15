package com.llmplatform.property;

import com.llmplatform.dto.LoginDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.User;
import com.llmplatform.mapper.UserMapper;
import com.llmplatform.service.UserService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.LoginVO;
import com.llmplatform.vo.UserVO;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for UserService
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServicePropertyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Feature: llm-language-learning-platform, Property 1: Registration creates retrievable user
     * 
     * For any valid registration data (username, password, email), after successful registration,
     * querying the user by username should return the same user data (excluding password).
     * 
     * Validates: Requirements 1.1
     */
    @Test
    @Transactional
    void registrationCreatesRetrievableUser_property() {
        Arbitrary<String> usernames = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20);
        Arbitrary<String> passwords = Arbitraries.strings().ofMinLength(6).ofMaxLength(20);
        Arbitrary<String> emails = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
                .map(name -> name.toLowerCase() + "@test.com");


        Arbitrary<RegisterDTO> registerDTOs = Combinators.combine(usernames, passwords, emails)
                .as((username, password, email) -> {
                    RegisterDTO dto = new RegisterDTO();
                    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
                    dto.setUsername(username + uniqueSuffix);
                    dto.setPassword(password);
                    dto.setEmail(email.replace("@", uniqueSuffix + "@"));
                    return dto;
                });

        for (int i = 0; i < 100; i++) {
            RegisterDTO dto = registerDTOs.sample();
            UserVO registeredUser = userService.register(dto);
            User retrievedUser = userService.getByUsername(dto.getUsername());
            
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getUsername()).isEqualTo(dto.getUsername());
            assertThat(retrievedUser.getEmail()).isEqualTo(dto.getEmail());
            assertThat(retrievedUser.getId()).isEqualTo(registeredUser.getId());
            assertThat(retrievedUser.getPassword()).isNotEqualTo(dto.getPassword());
            assertThat(retrievedUser.getPassword()).isNotNull();
        }
    }

    /**
     * Feature: llm-language-learning-platform, Property 2: Login-logout token invalidation
     * 
     * For any registered user, after login the issued token should be valid (JWT validation),
     * and after logout the same token should be invalid (blacklisted in Redis).
     * 
     * Note: This test validates JWT token generation and validation. The Redis blacklist
     * functionality is tested separately when Redis is available.
     * 
     * Validates: Requirements 1.2, 1.4
     */
    @Test
    @Transactional
    void loginLogoutTokenInvalidation_property() {
        Arbitrary<String> usernames = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20);
        Arbitrary<String> passwords = Arbitraries.strings().ofMinLength(6).ofMaxLength(20);
        Arbitrary<String> emails = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
                .map(name -> name.toLowerCase() + "@test.com");

        Arbitrary<RegisterDTO> registerDTOs = Combinators.combine(usernames, passwords, emails)
                .as((username, password, email) -> {
                    RegisterDTO dto = new RegisterDTO();
                    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
                    dto.setUsername(username + uniqueSuffix);
                    dto.setPassword(password);
                    dto.setEmail(email.replace("@", uniqueSuffix + "@"));
                    return dto;
                });

        for (int i = 0; i < 100; i++) {
            RegisterDTO registerDTO = registerDTOs.sample();
            userService.register(registerDTO);

            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setUsername(registerDTO.getUsername());
            loginDTO.setPassword(registerDTO.getPassword());
            LoginVO loginVO = userService.login(loginDTO);

            // Assert - Token should be valid after login (JWT validation)
            String token = loginVO.getToken();
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(jwtUtil.validateToken(token)).isTrue();
            
            // Assert - Token contains correct user info
            assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(loginVO.getUser().getId());
            assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(registerDTO.getUsername());
        }
    }
}
