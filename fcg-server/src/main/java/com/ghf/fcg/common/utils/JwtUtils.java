package com.ghf.fcg.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtils {

    private static String secret;

    private static long expiration;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        JwtUtils.secret = secret;
    }

    @Value("${jwt.expiration}")
    public void setExpiration(long expiration) {
        JwtUtils.expiration = expiration;
    }

    /**
     * 生成家庭级 token（登录后使用，只含 familyId）
     * 用途：获取成员列表（选人页）
     */
    public static String generateFamilyToken(Long familyId, String username) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .claim("familyId", familyId)
                .claim("username", username)
                .claim("tokenType", "family")   // 标记 token 类型
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成成员级 token（选人后使用，含 familyId + memberId + role）
     * 用途：访问所有业务接口
     */
    public static String generateMemberToken(Long familyId, Long memberId, Integer role) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .claim("familyId", familyId)
                .claim("memberId", memberId)
                .claim("role", role)
                .claim("tokenType", "member")   // 标记 token 类型
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(getSigningKey())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 判断是否为家庭级 token */
    public static boolean isFamilyToken(Claims claims) {
        return "family".equals(claims.get("tokenType", String.class));
    }

    /** 判断是否为成员级 token */
    public static boolean isMemberToken(Claims claims) {
        return "member".equals(claims.get("tokenType", String.class));
    }

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
