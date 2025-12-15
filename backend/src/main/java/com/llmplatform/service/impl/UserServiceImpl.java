package com.llmplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.dto.LoginDTO;
import com.llmplatform.dto.RegisterDTO;
import com.llmplatform.entity.User;
import com.llmplatform.exception.BusinessException;
import com.llmplatform.mapper.UserMapper;
import com.llmplatform.service.UserService;
import com.llmplatform.util.JwtUtil;
import com.llmplatform.vo.LoginVO;
import com.llmplatform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final long TOKEN_BLACKLIST_EXPIRATION = 24; // hours

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // Check if username already exists
        if (getByUsername(dto.getUsername()) != null) {
            throw new BusinessException("USERNAME_EXISTS", "Username already exists");
        }

        // Check if email already exists
        User existingEmail = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (existingEmail != null) {
            throw new BusinessException("EMAIL_EXISTS", "Email already exists");
        }


        // Create new user
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        return convertToVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));

        return loginVO;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // Add token to blacklist in Redis
            String key = TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "1", TOKEN_BLACKLIST_EXPIRATION, TimeUnit.HOURS);
        }
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? convertToVO(user) : null;
    }

    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        // Check if token is in blacklist
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.FALSE.equals(redisTemplate.hasKey(key));
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
