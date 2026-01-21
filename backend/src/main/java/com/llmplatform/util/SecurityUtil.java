package com.llmplatform.util;

import com.llmplatform.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 提供从安全上下文中获取当前用户信息的工具方法
 */
public class SecurityUtil {

    /**
     * 获取当前认证的用户ID
     *
     * @return 当前用户ID
     * @throws BusinessException 如果用户未认证
     */
    public static Long getCurrentUserId() {
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
        } else {
            throw new BusinessException("UNAUTHORIZED", "无法获取用户ID");
        }
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return true 如果已认证，false 否则
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getPrincipal());
    }
}