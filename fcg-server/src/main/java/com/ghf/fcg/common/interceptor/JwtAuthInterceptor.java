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
 * 支持两种 token：
 *   - 家庭级 token（tokenType=family）：登录后获取，只能访问 /api/family/members 和 /api/family/switch-member
 *   - 成员级 token（tokenType=member）：选人后获取，可访问所有业务接口
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

        Long familyId = claims.get("familyId", Long.class);
        if (familyId == null) {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        if (JwtUtils.isFamilyToken(claims)) {
            // 家庭级 token：memberId 和 role 为 null，只允许访问选人相关接口
            UserContext.set(new UserContext.UserInfo(familyId, null, null));
        } else if (JwtUtils.isMemberToken(claims)) {
            // 成员级 token：携带 memberId 和 role
            Long memberId = claims.get("memberId", Long.class);
            Integer role = claims.get("role", Integer.class);
            if (memberId == null || role == null) {
                throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
            }
            UserContext.set(new UserContext.UserInfo(familyId, memberId, role));
        } else {
            throw new BusinessException(401, MessageConstant.UNAUTHORIZED);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
