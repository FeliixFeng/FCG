package com.ghf.fcg.common.interceptor;

import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        String token = authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length()).trim()
                : authHeader.trim();

        if (token.isEmpty()) {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        Integer role = claims.get("role", Integer.class);

        if (userId == null || username == null || role == null) {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        UserContext.set(new UserContext.UserInfo(userId, username, role));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
