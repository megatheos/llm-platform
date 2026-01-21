package com.llmplatform.personalized.controller;

import com.llmplatform.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 个性化学习模块基类控制器
 * 提供通用的用户认证和请求处理功能
 */
public abstract class BaseController {

    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID
     * @throws BusinessException 如果用户未认证
     */
    protected Long requireUserId() {
        return getCurrentUserIdOrThrow();
    }

    /**
     * 获取当前用户ID或返回默认值
     *
     * @param defaultValue 默认值
     * @return 当前用户ID或默认值
     */
    protected Long getUserIdOrDefault(Long defaultValue) {
        try {
            return getCurrentUserIdOrThrow();
        } catch (BusinessException e) {
            return defaultValue;
        }
    }

    private Long getCurrentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("UNAUTHORIZED", "用户未认证");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new BusinessException("UNAUTHORIZED", "无效的用户ID");
            }
        }
        throw new BusinessException("UNAUTHORIZED", "无法获取用户ID");
    }
}
