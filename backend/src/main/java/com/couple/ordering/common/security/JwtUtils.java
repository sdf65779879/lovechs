package com.couple.ordering.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析工具（基于 jjwt 0.12.x）。
 *
 * <p>Token 中携带 subject=username，并附带 userId、role 声明；
 * 密钥通过配置 {@code couple.jwt.secret} 注入，要求至少 32 字节。</p>
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${couple.jwt.secret}")
    private String secret;

    @Value("${couple.jwt.expire-seconds}")
    private long expireSeconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，请通过环境变量 JWT_SECRET 注入足够长的随机密钥");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT。
     *
     * @param userId   用户 id
     * @param username 登录账号
     * @param role     角色
     * @return 签名后的 token 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token，返回 Claims；token 非法或过期时抛出异常。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Token 有效期（秒），用于返回给前端。
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }
}
